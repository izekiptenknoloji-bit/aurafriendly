package com.izekip.izessentials.client.friends.firebase;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Firebase email+sifre kimlik dogrulamasi (REST, resmi Firebase SDK'si olmadan).
 * localId + refreshToken diske kalici olarak yazilir (config/aurafriendly_auth.json),
 * idToken sadece bellekte tutulur ve gerektiginde otomatik yenilenir. Sifre hicbir
 * zaman diske yazilmaz, sadece giris/kayit anindaki tek seferlik istekte kullanilir.
 */
public final class FirebaseAuthClient {
	private static final Logger LOGGER = LoggerFactory.getLogger("aurafriendly/auth");
	private static final Gson GSON = new Gson();
	private static final long EXPIRY_SAFETY_MARGIN_SECONDS = 60;

	private final HttpClient httpClient;
	private final Path authFile;

	private String localId;
	private String refreshToken;
	private String idToken;
	private long idTokenExpiresAtMillis;
	private String email;

	public FirebaseAuthClient(HttpClient httpClient) {
		this.httpClient = httpClient;
		this.authFile = FabricLoader.getInstance().getConfigDir().resolve("aurafriendly_auth.json");
		loadPersisted();
	}

	public synchronized boolean hasStoredCredentials() {
		return refreshToken != null;
	}

	public synchronized String getUid() {
		return localId;
	}

	public synchronized String getEmail() {
		return email;
	}

	public synchronized void signUp(String email, String password) throws IOException, InterruptedException {
		authenticate("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + FirebaseConfig.WEB_API_KEY, email, password);
	}

	public synchronized void signIn(String email, String password) throws IOException, InterruptedException {
		authenticate("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + FirebaseConfig.WEB_API_KEY, email, password);
	}

	public synchronized void signOut() {
		this.localId = null;
		this.refreshToken = null;
		this.idToken = null;
		this.idTokenExpiresAtMillis = 0;
		this.email = null;
		try {
			Files.deleteIfExists(authFile);
		} catch (IOException e) {
			LOGGER.warn("aurafriendly_auth.json silinemedi.", e);
		}
	}

	/** Gecerli bir idToken dondurur, gerekirse yeniler. Giris yapilmamissa IllegalStateException firlatir. */
	public synchronized String getIdToken() throws IOException, InterruptedException {
		long now = System.currentTimeMillis();
		if (idToken != null && now < idTokenExpiresAtMillis) {
			return idToken;
		}
		if (refreshToken == null) {
			throw new IllegalStateException("Aura Friendly hesabina giris yapilmamis.");
		}
		refresh();
		return idToken;
	}

	private void authenticate(String url, String email, String password) throws IOException, InterruptedException {
		JsonObject body = new JsonObject();
		body.addProperty("email", email);
		body.addProperty("password", password);
		body.addProperty("returnSecureToken", true);

		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
				.build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

		if (response.statusCode() != 200) {
			String code = json.has("error") && json.getAsJsonObject("error").has("message")
					? json.getAsJsonObject("error").get("message").getAsString()
					: "BILINMEYEN_HATA";
			throw new IOException(translateError(code));
		}

		this.localId = json.get("localId").getAsString();
		this.refreshToken = json.get("refreshToken").getAsString();
		this.idToken = json.get("idToken").getAsString();
		this.email = json.has("email") ? json.get("email").getAsString() : email;
		applyExpiry(json.get("expiresIn").getAsString());
		persist();
	}

	private void refresh() throws IOException, InterruptedException {
		String url = "https://securetoken.googleapis.com/v1/token?key=" + FirebaseConfig.WEB_API_KEY;
		String body = "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			throw new IOException("Firebase token yenileme basarisiz: " + response.statusCode());
		}
		JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
		this.idToken = json.get("id_token").getAsString();
		this.refreshToken = json.get("refresh_token").getAsString();
		applyExpiry(json.get("expires_in").getAsString());
		persist();
	}

	private void applyExpiry(String expiresInSeconds) {
		long expiresIn = Long.parseLong(expiresInSeconds);
		long safe = Math.max(0, expiresIn - EXPIRY_SAFETY_MARGIN_SECONDS);
		this.idTokenExpiresAtMillis = System.currentTimeMillis() + safe * 1000L;
	}

	private static String translateError(String code) {
		if (code.startsWith("WEAK_PASSWORD")) {
			return "Sifre en az 6 karakter olmali.";
		}
		switch (code) {
			case "EMAIL_EXISTS": return "Bu email zaten kayitli, Giris Yap'i dene.";
			case "EMAIL_NOT_FOUND":
			case "INVALID_LOGIN_CREDENTIALS":
			case "INVALID_PASSWORD": return "Email veya sifre hatali.";
			case "INVALID_EMAIL": return "Gecersiz email adresi.";
			case "TOO_MANY_ATTEMPTS_TRY_LATER": return "Cok fazla deneme yapildi, birazdan tekrar dene.";
			case "EMAIL_NOT_FOUND_OR_PASSWORD_MISMATCH": return "Email veya sifre hatali.";
			default: return code;
		}
	}

	private void loadPersisted() {
		try {
			if (Files.exists(authFile)) {
				String json = Files.readString(authFile, StandardCharsets.UTF_8);
				AuthData data = GSON.fromJson(json, AuthData.class);
				if (data != null) {
					this.localId = data.localId;
					this.refreshToken = data.refreshToken;
					this.email = data.email;
				}
			}
		} catch (IOException | JsonSyntaxException e) {
			LOGGER.warn("aurafriendly_auth.json okunamadi, tekrar giris yapman gerekecek.", e);
		}
	}

	private void persist() {
		try {
			AuthData data = new AuthData();
			data.localId = localId;
			data.refreshToken = refreshToken;
			data.email = email;
			Path tmp = authFile.resolveSibling(authFile.getFileName() + ".tmp");
			Files.writeString(tmp, GSON.toJson(data), StandardCharsets.UTF_8);
			Files.move(tmp, authFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			LOGGER.error("aurafriendly_auth.json yazilamadi.", e);
		}
	}

	private static class AuthData {
		String localId;
		String refreshToken;
		String email;
	}
}
