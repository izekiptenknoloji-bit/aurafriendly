package com.izekip.izessentials.client.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * GitHub Releases uzerinden guncelleme kontrolu + oto-indirme.
 *
 * MİMARİ NOT (onemli): calisan JVM kendi yuklu oldugu jar dosyasini Windows'ta
 * SILEMEZ/DEGISTIREMEZ (dosya kilitli). Bu yuzden:
 *  1. Yeni surum FARKLI bir dosya adiyla (surum numarasi adin icinde) indirilir,
 *     mevcut calisan jar'a hic dokunulmaz.
 *  2. Eski jar, File.deleteOnExit() ile "JVM kapaninca sil" olarak isaretlenir -
 *     bu Windows'ta "best effort"tur, %100 garanti degildir.
 *  3. Bir sonraki acilista cleanupStaleJars() calisir: o an Fabric'in FIILEN
 *     yukledigi jar disindaki tum "aurafriendly-*.jar" dosyalarini siler - boylece
 *     deleteOnExit basarisiz olsa bile mods klasoru kendini temizler ve iki farkli
 *     surumun ayni anda yuklenmeye calisilip modid catismasi olusmasi engellenir.
 */
public final class ModUpdater {
	private static final Logger LOGGER = LoggerFactory.getLogger("aurafriendly/updater");
	private static final String MOD_ID = "aurafriendly";

	private static final String GITHUB_REPO = "izekiptenknoloji-bit/aurafriendly";

	/** Bu mod derlemesinin hedeflendigi Minecraft surumu - release asset'lerini filtrelemek icin. */
	private static final String ASSET_SUFFIX = "-mc1.21.4.jar";

	private static final AtomicBoolean CHECKED_THIS_SESSION = new AtomicBoolean(false);
	private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId();

	private ModUpdater() {
	}

	/** Eldeki mod'un yuklu oldugu jar disinda kalan eski aurafriendly-*.jar dosyalarini siler. Client init'te bir kez cagrilir. */
	public static void cleanupStaleJars() {
		try {
			Optional<ModContainer> containerOpt = FabricLoader.getInstance().getModContainer(MOD_ID);
			if (containerOpt.isEmpty() || containerOpt.get().getOrigin().getPaths().isEmpty()) {
				return;
			}
			Path currentJar = containerOpt.get().getOrigin().getPaths().get(0).toAbsolutePath();
			Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
			if (!Files.isDirectory(modsDir)) {
				return;
			}
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "aurafriendly-*.jar")) {
				for (Path candidate : stream) {
					if (!candidate.toAbsolutePath().equals(currentJar)) {
						Files.deleteIfExists(candidate);
						LOGGER.info("Eski Aura Friendly jar temizlendi: {}", candidate.getFileName());
					}
				}
			}
		} catch (Exception e) {
			LOGGER.debug("Eski jar temizligi basarisiz (onemli degil): {}", e.toString());
		}
	}

	/**
	 * GitHub'daki en son surumu kontrol eder, varsa arka planda indirir. Ag hatalarinda sessizce vazgecer.
	 * Bir oturumda (client acik kaldigi surece) sadece bir kez calisir - JOIN her sunucuya girildiginde
	 * tekrar tetiklendigi icin bu bayrak olmadan her sunucu degisiminde tekrar kontrol ederdi.
	 */
	public static void checkForUpdateAsync() {
		if (!CHECKED_THIS_SESSION.compareAndSet(false, true)) {
			return;
		}
		Thread.ofVirtual().name("aurafriendly-updater").start(ModUpdater::checkForUpdate);
	}

	private static void checkForUpdate() {
		try {
			Optional<ModContainer> containerOpt = FabricLoader.getInstance().getModContainer(MOD_ID);
			if (containerOpt.isEmpty()) {
				return;
			}
			ModContainer container = containerOpt.get();
			Version localVersion = container.getMetadata().getVersion();

			HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
			HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest"))
					.header("Accept", "application/vnd.github+json")
					.timeout(Duration.ofSeconds(10))
					.GET().build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				LOGGER.debug("Guncelleme kontrolu: GitHub'dan {} donuldu.", response.statusCode());
				return;
			}

			JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
			String tag = json.get("tag_name").getAsString();
			String remoteVersionStr = tag.startsWith("v") ? tag.substring(1) : tag;

			Version remoteVersion;
			try {
				remoteVersion = Version.parse(remoteVersionStr);
			} catch (VersionParsingException e) {
				LOGGER.debug("GitHub surum etiketi ayristirilamadi: {}", tag);
				return;
			}

			if (remoteVersion.compareTo(localVersion) <= 0) {
				return;
			}

			String downloadUrl = null;
			String assetName = null;
			JsonArray assets = json.getAsJsonArray("assets");
			for (JsonElement element : assets) {
				JsonObject asset = element.getAsJsonObject();
				String name = asset.get("name").getAsString();
				if (name.endsWith(ASSET_SUFFIX)) {
					downloadUrl = asset.get("browser_download_url").getAsString();
					assetName = name;
					break;
				}
			}

			String notes = json.has("body") && !json.get("body").isJsonNull() ? json.get("body").getAsString() : null;
			notifyUpdateToast(remoteVersionStr, notes);

			if (downloadUrl == null) {
				LOGGER.info("Yeni surum var ama bu Minecraft surumu ({}) icin dosya bulunamadi.", ASSET_SUFFIX);
				return;
			}

			downloadAndInstall(client, downloadUrl, assetName, container);
		} catch (Exception e) {
			LOGGER.debug("Guncelleme kontrolu basarisiz (onemli degil, oyunu etkilemez): {}", e.toString());
		}
	}

	private static void downloadAndInstall(HttpClient client, String downloadUrl, String assetName, ModContainer container) {
		try {
			Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
			Path target = modsDir.resolve(assetName);
			if (Files.exists(target)) {
				notifyRestartToast();
				return;
			}

			Path tmp = modsDir.resolve(assetName + ".tmp");
			HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl)).GET().build();
			HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tmp));
			if (response.statusCode() != 200) {
				Files.deleteIfExists(tmp);
				LOGGER.warn("Guncelleme indirilemedi: HTTP {}", response.statusCode());
				return;
			}
			Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);

			for (Path oldJar : container.getOrigin().getPaths()) {
				oldJar.toFile().deleteOnExit();
			}

			notifyRestartToast();
			LOGGER.info("Guncelleme indirildi: {}", target);
		} catch (IOException | InterruptedException e) {
			LOGGER.warn("Guncelleme indirilemedi.", e);
		}
	}

	/** Yeni surum bulununca sag ustte cikan kucuk oyun-ici bildirim (achievement/paket bildirimi gibi), tam ekran degil. */
	private static void notifyUpdateToast(String version, String notes) {
		Minecraft client = Minecraft.getInstance();
		client.execute(() -> {
			Component title = Component.literal("Aura Friendly - Yeni surum: v" + version);
			Component message = Component.literal(summarize(notes));
			SystemToast.add(client.getToastManager(), TOAST_ID, title, message);
		});
	}

	private static void notifyRestartToast() {
		Minecraft client = Minecraft.getInstance();
		client.execute(() -> SystemToast.add(client.getToastManager(), TOAST_ID,
				Component.literal("Aura Friendly guncellendi"),
				Component.literal("Devreye girmesi icin oyunu kapatip tekrar ac.")));
	}

	/** Release notlarinin ilk satirini toast'a sigacak kadar kisaltir. */
	private static String summarize(String notes) {
		if (notes == null || notes.isBlank()) {
			return "Detaylar icin GitHub'a bak.";
		}
		String firstLine = notes.strip().lines().findFirst().orElse("").strip();
		if (firstLine.isEmpty()) {
			return "Detaylar icin GitHub'a bak.";
		}
		return firstLine.length() > 90 ? firstLine.substring(0, 87) + "..." : firstLine;
	}
}
