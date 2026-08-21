package com.izekip.izessentials.client.friends;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.izekip.izessentials.client.friends.firebase.FirebaseAuthClient;
import com.izekip.izessentials.client.friends.firebase.FirebaseRealtimeListener;
import com.izekip.izessentials.client.friends.firebase.FirebaseRestClient;
import com.izekip.izessentials.client.friends.model.FriendRecord;
import com.izekip.izessentials.client.friends.model.FriendState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Arkadas listesi, istekler ve her kabul edilmis arkadasin canli online durumu.
 * Tum ag/dosya islemleri sanal thread'lerde yapilir, render/tick thread'ini bloklamaz.
 * stop() cagrildiginda tum SSE dinleyicileri durur (asil iptal PresenceManager'in
 * paylasilan HttpClient'ini kapatmasiyla olur, bkz. FirebaseRealtimeListener).
 */
public final class FriendsManager {
	private static final Logger LOGGER = LoggerFactory.getLogger("aurafriendly/friends");
	private static final long PRESENCE_STALE_MS = 90_000;

	private final HttpClient httpClient;
	private final FirebaseAuthClient auth;
	private final FirebaseRestClient rest;
	private final Map<UUID, FriendRecord> friends = new ConcurrentHashMap<>();
	private final Set<UUID> watchedPresence = ConcurrentHashMap.newKeySet();
	private final List<FirebaseRealtimeListener> listeners = new CopyOnWriteArrayList<>();

	private volatile String myUuid;

	public FriendsManager(HttpClient httpClient, FirebaseAuthClient auth, FirebaseRestClient rest) {
		this.httpClient = httpClient;
		this.auth = auth;
		this.rest = rest;
	}

	public void start(String myUuid) {
		this.myUuid = myUuid;

		try {
			String json = rest.get("/users/" + myUuid + "/friends");
			if (json != null && !"null".equals(json)) {
				JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
				for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
					applyFriendUpdate(entry.getKey(), entry.getValue());
				}
			}
		} catch (Exception e) {
			LOGGER.warn("Baslangic arkadas listesi alinamadi (baglanti sorunlu olabilir).", e);
		}

		FirebaseRealtimeListener friendsTreeListener = new FirebaseRealtimeListener(httpClient, auth,
				"/users/" + myUuid + "/friends", this::onFriendsTreeEvent);
		friendsTreeListener.start();
		listeners.add(friendsTreeListener);
	}

	public Collection<FriendRecord> getFriends() {
		return friends.values();
	}

	public void addFriend(String username, Consumer<String> onError) {
		Thread.ofVirtual().name("aurafriendly-addfriend").start(() -> {
			if (myUuid == null) {
				onError.accept("Aura Friendly henuz hazir degil.");
				return;
			}
			try {
				String targetUuid = resolveUuid(username);
				if (targetUuid == null) {
					onError.accept("Kullanici bulunamadi: " + username);
					return;
				}
				if (targetUuid.equalsIgnoreCase(myUuid)) {
					onError.accept("Kendini ekleyemezsin.");
					return;
				}
				rest.put("/users/" + myUuid + "/friends/" + targetUuid, "\"pending_outgoing\"");
				rest.put("/users/" + targetUuid + "/friends/" + myUuid, "\"pending_incoming\"");
			} catch (Exception e) {
				LOGGER.warn("Arkadas eklenemedi.", e);
				onError.accept("Istek gonderilemedi: " + e.getMessage());
			}
		});
	}

	public void acceptFriend(UUID friendUuid) {
		Thread.ofVirtual().name("aurafriendly-accept").start(() -> {
			try {
				rest.put("/users/" + myUuid + "/friends/" + friendUuid, "\"accepted\"");
				rest.put("/users/" + friendUuid + "/friends/" + myUuid, "\"accepted\"");
			} catch (Exception e) {
				LOGGER.warn("Arkadaslik istegi kabul edilemedi.", e);
			}
		});
	}

	public void removeFriend(UUID friendUuid) {
		Thread.ofVirtual().name("aurafriendly-remove").start(() -> {
			try {
				rest.put("/users/" + myUuid + "/friends/" + friendUuid, "null");
				rest.put("/users/" + friendUuid + "/friends/" + myUuid, "null");
			} catch (Exception e) {
				LOGGER.warn("Arkadas silinemedi.", e);
			}
		});
	}

	public void stop() {
		for (FirebaseRealtimeListener listener : listeners) {
			listener.stop();
		}
		listeners.clear();
		friends.clear();
		watchedPresence.clear();
	}

	private void onFriendsTreeEvent(String eventType, JsonElement pathEl, JsonElement dataEl) {
		String path = (pathEl != null && !pathEl.isJsonNull()) ? pathEl.getAsString() : "/";
		if ("/".equals(path)) {
			friends.clear();
			if (dataEl != null && dataEl.isJsonObject()) {
				for (Map.Entry<String, JsonElement> entry : dataEl.getAsJsonObject().entrySet()) {
					applyFriendUpdate(entry.getKey(), entry.getValue());
				}
			}
		} else {
			String key = path.startsWith("/") ? path.substring(1) : path;
			applyFriendUpdate(key, dataEl);
		}
	}

	private void applyFriendUpdate(String uuidStr, JsonElement dataEl) {
		UUID friendUuid;
		try {
			friendUuid = UUID.fromString(uuidStr);
		} catch (IllegalArgumentException e) {
			return;
		}

		if (dataEl == null || dataEl.isJsonNull()) {
			friends.remove(friendUuid);
			return;
		}

		FriendState state = FriendState.fromJson(dataEl.getAsString());
		if (state == null) {
			return;
		}
		FriendRecord record = friends.computeIfAbsent(friendUuid, u -> new FriendRecord(u, null, state));
		record.state = state;

		if (state == FriendState.ACCEPTED) {
			if (record.username == null) {
				fetchUsername(record);
			}
			watchPresence(record);
		}
	}

	private void fetchUsername(FriendRecord record) {
		Thread.ofVirtual().name("aurafriendly-username-lookup").start(() -> {
			try {
				String json = rest.get("/users/" + record.uuid + "/profile");
				if (json != null && !"null".equals(json)) {
					JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
					if (obj.has("username")) {
						record.username = obj.get("username").getAsString();
					}
				}
			} catch (Exception ignored) {
				// Kullanici adi bulunamadi, UUID gosterilmeye devam eder.
			}
		});
	}

	private void watchPresence(FriendRecord record) {
		if (!watchedPresence.add(record.uuid)) {
			return;
		}
		FirebaseRealtimeListener listener = new FirebaseRealtimeListener(httpClient, auth,
				"/users/" + record.uuid + "/presence", (eventType, pathEl, dataEl) -> onPresenceEvent(record, dataEl));
		listener.start();
		listeners.add(listener);
	}

	private void onPresenceEvent(FriendRecord record, JsonElement dataEl) {
		boolean wasOnline = record.online;
		if (dataEl != null && dataEl.isJsonObject()) {
			JsonObject obj = dataEl.getAsJsonObject();
			long lastSeen = (obj.has("lastSeen") && obj.get("lastSeen").isJsonPrimitive()) ? obj.get("lastSeen").getAsLong() : 0;
			boolean flaggedOnline = obj.has("online") && obj.get("online").getAsBoolean();
			record.lastSeen = lastSeen;
			record.online = flaggedOnline && (System.currentTimeMillis() - lastSeen < PRESENCE_STALE_MS);
		} else {
			record.online = false;
		}

		if (!wasOnline && record.online) {
			notifyFriendOnline(record);
		}
	}

	private void notifyFriendOnline(FriendRecord record) {
		Minecraft client = Minecraft.getInstance();
		if (client.player != null) {
			String name = record.username != null ? record.username : record.uuid.toString();
			client.player.displayClientMessage(Component.literal("[Aura Friendly] " + name + " cevrimici oldu!"), false);
		}
	}

	private String resolveUuid(String username) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/" + username))
				.GET().build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			return null;
		}
		JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
		return insertDashes(obj.get("id").getAsString());
	}

	private static String insertDashes(String raw) {
		return raw.replaceFirst(
				"(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
				"$1-$2-$3-$4-$5");
	}
}
