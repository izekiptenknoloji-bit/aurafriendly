package com.izekip.izessentials.client.friends.firebase;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Firebase Realtime Database'in REST API'si "Accept: text/event-stream" ile
 * canli SSE akisi sunar (put/patch/keep-alive/cancel/auth_revoked olaylari).
 * Her dinleyici kendi sanal thread'inde calisir; kapatma PresenceManager'in
 * paylasilan HttpClient'ini shutdownNow() ile iptal etmesiyle olur - bu da
 * asagidaki send() cagrisini istisna firlatarak sonlandirir ve dongu biter.
 */
public final class FirebaseRealtimeListener {
	private static final Logger LOGGER = LoggerFactory.getLogger("aurafriendly/sse");

	public interface EventHandler {
		void onEvent(String eventType, JsonElement path, JsonElement data);
	}

	private final HttpClient httpClient;
	private final FirebaseAuthClient auth;
	private final String relativePath;
	private final EventHandler handler;
	private final AtomicBoolean stopped = new AtomicBoolean(false);

	public FirebaseRealtimeListener(HttpClient httpClient, FirebaseAuthClient auth, String relativePath, EventHandler handler) {
		this.httpClient = httpClient;
		this.auth = auth;
		this.relativePath = relativePath;
		this.handler = handler;
	}

	public void start() {
		Thread.ofVirtual().name("aurafriendly-sse-" + relativePath).start(this::run);
	}

	/** Bir sonraki dongu kontrolunde cikmasi icin bayrak koyar; asil iptal paylasimli HttpClient.shutdownNow() ile olur. */
	public void stop() {
		stopped.set(true);
	}

	private void run() {
		while (!stopped.get()) {
			try {
				String idToken = auth.getIdToken();
				String url = FirebaseConfig.DATABASE_URL + relativePath + ".json?auth=" + idToken;
				HttpRequest request = HttpRequest.newBuilder(URI.create(url))
						.header("Accept", "text/event-stream")
						.GET().build();
				HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());

				String eventType = null;
				Iterator<String> lines = response.body().iterator();
				while (lines.hasNext() && !stopped.get()) {
					String line = lines.next();
					if (line.isEmpty()) {
						continue;
					}
					if (line.startsWith("event: ")) {
						eventType = line.substring("event: ".length()).trim();
					} else if (line.startsWith("data: ")) {
						String data = line.substring("data: ".length());
						if ("put".equals(eventType) || "patch".equals(eventType)) {
							dispatch(eventType, data);
						}
						eventType = null;
					}
				}
			} catch (Exception e) {
				if (stopped.get()) {
					return;
				}
				LOGGER.debug("SSE baglantisi koptu ({}), 3sn sonra yeniden denenecek: {}", relativePath, e.toString());
				try {
					Thread.sleep(3000);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	private void dispatch(String eventType, String data) {
		try {
			JsonObject obj = JsonParser.parseString(data).getAsJsonObject();
			handler.onEvent(eventType, obj.get("path"), obj.get("data"));
		} catch (Exception e) {
			LOGGER.debug("SSE olayi ayristirilamadi: {}", data);
		}
	}
}
