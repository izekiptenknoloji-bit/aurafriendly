package com.izekip.izessentials.client.friends.firebase;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Firebase Realtime Database REST API'sine (path.json) duz GET/PUT/PATCH istekleri
 * icin kucuk bir yardimci. idToken her istekte FirebaseAuthClient'tan taze alinir.
 */
public final class FirebaseRestClient {
	private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(6);

	private final HttpClient httpClient;
	private final FirebaseAuthClient auth;

	public FirebaseRestClient(HttpClient httpClient, FirebaseAuthClient auth) {
		this.httpClient = httpClient;
		this.auth = auth;
	}

	/** null donebilir (Firebase'de yol bossa "null" body doner). */
	public String get(String path) throws IOException, InterruptedException {
		String url = FirebaseConfig.DATABASE_URL + path + ".json?auth=" + auth.getIdToken();
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(DEFAULT_TIMEOUT)
				.GET().build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() / 100 != 2) {
			throw new IOException("GET " + path + " -> " + response.statusCode() + " " + response.body());
		}
		return response.body();
	}

	public void put(String path, String jsonValue) throws IOException, InterruptedException {
		send("PUT", path, jsonValue, DEFAULT_TIMEOUT);
	}

	public void patch(String path, String jsonValue) throws IOException, InterruptedException {
		send("PATCH", path, jsonValue, DEFAULT_TIMEOUT);
	}

	public void patch(String path, String jsonValue, Duration timeout) throws IOException, InterruptedException {
		send("PATCH", path, jsonValue, timeout);
	}

	private void send(String method, String path, String jsonValue, Duration timeout) throws IOException, InterruptedException {
		String url = FirebaseConfig.DATABASE_URL + path + ".json?auth=" + auth.getIdToken();
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.header("Content-Type", "application/json")
				.timeout(timeout)
				.method(method, HttpRequest.BodyPublishers.ofString(jsonValue))
				.build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() / 100 != 2) {
			throw new IOException(method + " " + path + " -> " + response.statusCode() + " " + response.body());
		}
	}
}
