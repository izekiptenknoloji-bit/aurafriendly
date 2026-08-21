package com.izekip.izessentials.client.friends;

import com.izekip.izessentials.client.friends.firebase.FirebaseAuthClient;
import com.izekip.izessentials.client.friends.firebase.FirebaseRestClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * "Aura Friendly" ozelliginin yasam dongusu sahibi. Iki ayri bayrak var:
 *  - STARTED: HttpClient/auth/rest/friendsManager altyapisi kuruldu mu (her dunyaya
 *    girildiginde kurulur, cikista mutlaka temizlenir - hesaba giris yapilmis olsun ya da olmasin).
 *  - SESSION_ACTIVE: kullanici giris yapip heartbeat/friendsManager fiilen calisiyor mu.
 * Boylece "hic giris yapilmamis" durumda bile hicbir HttpClient/thread sizmaz.
 */
public final class PresenceManager {
	private static final Logger LOGGER = LoggerFactory.getLogger("aurafriendly/presence");
	private static final long HEARTBEAT_SECONDS = 20;

	private static final AtomicBoolean STARTED = new AtomicBoolean(false);
	private static final AtomicBoolean SESSION_ACTIVE = new AtomicBoolean(false);

	private static HttpClient httpClient;
	private static FirebaseAuthClient auth;
	private static FirebaseRestClient rest;
	private static FriendsManager friendsManager;
	private static ScheduledExecutorService heartbeatExecutor;
	private static volatile String serverAddress;
	private static volatile String lastError;

	private PresenceManager() {
	}

	/** ClientPlayConnectionEvents.JOIN'de cagrilir. Sadece altyapiyi kurar; giris yapilmissa oturumu da baslatir. */
	public static void start(String serverAddress) {
		PresenceManager.serverAddress = serverAddress;
		if (!STARTED.compareAndSet(false, true)) {
			LOGGER.debug("PresenceManager altyapisi zaten kurulu, tekrar kurulmadi.");
			return;
		}

		httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.build();
		auth = new FirebaseAuthClient(httpClient);
		rest = new FirebaseRestClient(httpClient, auth);
		friendsManager = new FriendsManager(httpClient, auth, rest);

		if (auth.hasStoredCredentials()) {
			Thread.ofVirtual().name("aurafriendly-autologin").start(PresenceManager::beginSession);
		}
	}

	/** LoginScreen'de basarili giris/kayit sonrasi cagrilir. */
	public static void onLoginSuccess() {
		Thread.ofVirtual().name("aurafriendly-session-start").start(PresenceManager::beginSession);
	}

	public static void stop() {
		if (!STARTED.compareAndSet(true, false)) {
			return;
		}
		SESSION_ACTIVE.set(false);

		try {
			if (rest != null && auth != null && auth.getUid() != null) {
				LocalPlayer player = Minecraft.getInstance().player;
				if (player != null) {
					String uuid = player.getUUID().toString();
					rest.patch("/users/" + uuid + "/presence", "{\"online\":false}", Duration.ofSeconds(2));
				}
			}
		} catch (Exception e) {
			LOGGER.debug("Cikista 'offline' yazilamadi (onemli degil, heartbeat zaten dusecek): {}", e.toString());
		}

		if (heartbeatExecutor != null) {
			heartbeatExecutor.shutdownNow();
			heartbeatExecutor = null;
		}
		if (friendsManager != null) {
			friendsManager.stop();
			friendsManager = null;
		}
		if (httpClient != null) {
			httpClient.shutdownNow();
			httpClient = null;
		}
		auth = null;
		rest = null;
	}

	public static FriendsManager getFriendsManager() {
		return friendsManager;
	}

	public static FirebaseAuthClient getAuthForLogin() {
		return auth;
	}

	/** Altyapi kurulu ama hesaba hic giris yapilmamis mi (LoginScreen gosterilmeli). */
	public static boolean needsLogin() {
		return auth != null && !auth.hasStoredCredentials();
	}

	/** Heartbeat/FriendsManager fiilen calisiyor mu. */
	public static boolean isSessionActive() {
		return SESSION_ACTIVE.get();
	}

	/** Son basarisiz beginSession() denemesinin hata mesaji (kullaniciya gostermek icin). Basarili baglantida null olur. */
	public static String getLastError() {
		return lastError;
	}

	private static void beginSession() {
		if (!SESSION_ACTIVE.compareAndSet(false, true)) {
			return;
		}
		try {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player == null) {
				SESSION_ACTIVE.set(false);
				return;
			}
			String uuid = player.getUUID().toString();

			auth.getIdToken();
			linkUidIfNeeded(uuid);
			rest.put("/users/" + uuid + "/profile", "{\"username\":\"" + escapeJson(player.getGameProfile().name()) + "\"}");

			heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
				Thread thread = new Thread(runnable, "aurafriendly-heartbeat");
				thread.setDaemon(true);
				return thread;
			});
			heartbeatExecutor.scheduleAtFixedRate(() -> heartbeat(uuid, serverAddress), 0, HEARTBEAT_SECONDS, TimeUnit.SECONDS);

			friendsManager.start(uuid);
			lastError = null;
			LOGGER.info("Aura Friendly oturumu baslatildi.");
		} catch (Exception e) {
			lastError = e.getMessage() != null ? e.getMessage() : e.toString();
			LOGGER.warn("Aura Friendly oturumu baslatilamadi.", e);
			SESSION_ACTIVE.set(false);
		}
	}

	/** uidLinks kaydi yoksa yazar; varsa ve ayniysa dokunmaz; varsa ve FARKLIYSA acikca hata verir (sessizce ustune yazmaz). */
	private static void linkUidIfNeeded(String uuid) throws Exception {
		String existing = rest.get("/uidLinks/" + auth.getUid());
		if (existing == null || "null".equals(existing)) {
			rest.put("/uidLinks/" + auth.getUid(), "\"" + uuid + "\"");
			return;
		}
		String existingUuid = existing.replace("\"", "");
		if (!existingUuid.equals(uuid)) {
			throw new IllegalStateException("Bu Aura Friendly hesabi baska bir Minecraft hesabina (" + existingUuid + ") bagli.");
		}
	}

	private static void heartbeat(String uuid, String serverAddress) {
		try {
			String addressJson = serverAddress == null ? "null" : "\"" + escapeJson(serverAddress) + "\"";
			String json = "{\"online\":true,\"lastSeen\":{\".sv\":\"timestamp\"},\"serverAddress\":" + addressJson + "}";
			rest.patch("/users/" + uuid + "/presence", json);
		} catch (Exception e) {
			LOGGER.debug("Heartbeat gonderilemedi (gecici baglanti sorunu olabilir): {}", e.toString());
		}
	}

	private static String escapeJson(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
