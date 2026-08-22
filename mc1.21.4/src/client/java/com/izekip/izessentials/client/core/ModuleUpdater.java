package com.izekip.izessentials.client.core;

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
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * GitHub Releases uzerinden guncelleme kontrolu. Iki farkli sonuc yolu var:
 *
 *  1. CANLI GUNCELLEME (asil hedef): indirilen jar'daki icerik modulu ModuleHolder.trySwap()
 *     ile basariyla degistirilebiliyorsa, OYUN HIC KAPANMADAN yeni ozellikler devreye girer.
 *     Bu, "core" (bu paket + MacroSlots + tus kayitlari) DEGISMEDIGI surece calisir.
 *
 *  2. YENIDEN BASLATMA GEREKIR (yedek yol): core'un kendisi degismisse (ornegin arayuz
 *     degisti, yeni bir global tus eklendi) trySwap() basarisiz doner - bu durumda eski
 *     yontemimize doneriz: jar'i FARKLI bir dosya adiyla indirip mods/ klasorune koyariz,
 *     eski jar deleteOnExit ile isaretlenir, oyuncu oyunu manuel kapatip actiginda devreye girer.
 */
public final class ModuleUpdater {
	private static final Logger LOGGER = LoggerFactory.getLogger("aurafriendly/updater");
	private static final String MOD_ID = "aurafriendly";
	private static final String GITHUB_REPO = "izekiptenknoloji-bit/aurafriendly";
	private static final String ASSET_SUFFIX = "-mc1.21.4.jar";

	private static final AtomicBoolean CHECKED_THIS_SESSION = new AtomicBoolean(false);
	private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId();

	private ModuleUpdater() {
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

	public static String getLocalVersionFriendly() {
		return FabricLoader.getInstance().getModContainer(MOD_ID)
				.map(c -> c.getMetadata().getVersion().getFriendlyString())
				.orElse("?");
	}

	/** Oturum basi bir kere otomatik kontrol (JOIN'de cagrilir). */
	public static void checkForUpdateAsync() {
		if (!CHECKED_THIS_SESSION.compareAndSet(false, true)) {
			return;
		}
		Thread.ofVirtual().name("aurafriendly-updater").start(() -> runCheck(ModuleUpdater::notifyUpdateToast, null));
	}

	/** "Simdi Kontrol Et" butonu icin: oturum kilidini beklemeden hemen kontrol eder, sonucu metin olarak bildirir. */
	public static void checkNow(Consumer<String> onStatus) {
		Thread.ofVirtual().name("aurafriendly-manual-check").start(() -> runCheck((v, n) -> report(onStatus, "Guncelleniyor: v" + v), onStatus));
	}

	private static void runCheck(java.util.function.BiConsumer<String, String> onFound, Consumer<String> onStatus) {
		try {
			Optional<ModContainer> containerOpt = FabricLoader.getInstance().getModContainer(MOD_ID);
			if (containerOpt.isEmpty()) {
				if (onStatus != null) report(onStatus, "Mod bilgisi bulunamadi.");
				return;
			}
			ModContainer container = containerOpt.get();
			Version localVersion = container.getMetadata().getVersion();

			HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
			HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest"))
					.header("Accept", "application/vnd.github+json")
					.timeout(Duration.ofSeconds(10))
					.GET().build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				if (onStatus != null) report(onStatus, "GitHub'a ulasilamadi (HTTP " + response.statusCode() + ").");
				return;
			}

			JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
			String tag = json.get("tag_name").getAsString();
			String remoteVersionStr = tag.startsWith("v") ? tag.substring(1) : tag;

			Version remoteVersion;
			try {
				remoteVersion = Version.parse(remoteVersionStr);
			} catch (VersionParsingException e) {
				if (onStatus != null) report(onStatus, "Surum bilgisi okunamadi.");
				return;
			}

			if (remoteVersion.compareTo(localVersion) <= 0) {
				if (onStatus != null) report(onStatus, "Guncelsin! (v" + localVersion.getFriendlyString() + ")");
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
			if (downloadUrl == null) {
				if (onStatus != null) report(onStatus, "Yeni surum var (v" + remoteVersionStr + ") ama bu Minecraft surumu icin dosya yok.");
				return;
			}

			onFound.accept(remoteVersionStr, json.has("body") && !json.get("body").isJsonNull() ? json.get("body").getAsString() : null);
			downloadAndApply(client, downloadUrl, assetName, remoteVersionStr, container, onStatus);
		} catch (Exception e) {
			if (onStatus != null) report(onStatus, "Kontrol basarisiz: " + e.getMessage());
			LOGGER.debug("Guncelleme kontrolu basarisiz (onemli degil): {}", e.toString());
		}
	}

	private static void downloadAndApply(HttpClient client, String downloadUrl, String assetName, String version,
			ModContainer container, Consumer<String> onStatus) {
		try {
			Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
			Path target = modsDir.resolve(assetName);

			if (!Files.exists(target)) {
				Path tmp = modsDir.resolve(assetName + ".tmp");
				downloadWithProgress(client, downloadUrl, tmp, percent -> {
					notifyProgressToast(version, percent);
					if (onStatus != null) report(onStatus, "Indiriliyor: %" + percent);
				});
				Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
			}

			Path currentJar = container.getOrigin().getPaths().isEmpty() ? null : container.getOrigin().getPaths().get(0);
			boolean assetsChanged = currentJar == null || assetsChanged(currentJar, target);
			boolean hotSwapped = !assetsChanged && tryHotSwap(target);
			if (assetsChanged) {
				LOGGER.info("Yeni surumde kaynak (font/texture/lang) degisikligi var - canli guncelleme atlaniyor, restart istenecek.");
			}
			if (hotSwapped) {
				notifyLiveUpdateToast(version);
				if (onStatus != null) report(onStatus, "Canli guncellendi: v" + version + " (oyunu kapatmana gerek yok)");
				for (Path oldJar : container.getOrigin().getPaths()) {
					if (!oldJar.toAbsolutePath().equals(target.toAbsolutePath())) {
						oldJar.toFile().deleteOnExit();
					}
				}
			} else {
				for (Path oldJar : container.getOrigin().getPaths()) {
					oldJar.toFile().deleteOnExit();
				}
				notifyRestartToast();
				if (onStatus != null) report(onStatus, "Indirildi: v" + version + ". Oyunu yeniden baslat.");
			}
		} catch (IOException | InterruptedException e) {
			LOGGER.warn("Guncelleme indirilemedi.", e);
			if (onStatus != null) report(onStatus, "Indirme basarisiz: " + e.getMessage());
		}
	}

	/**
	 * Iki jar'daki "assets/" altindaki dosyalarin (font, texture, lang...) ayni olup olmadigini
	 * kontrol eder. Minecraft'in kaynak yoneticisi (ReloadableResourceManager) mod jar'larini
	 * sadece oyun aciliskten tarar - bizim ContentClassLoader hot-swap'imiz SADECE .class
	 * dosyalarini degistirir, kaynaklari degil. Bu yuzden assets/ altinda herhangi bir fark
	 * varsa canli guncelleme guvenilir degildir (yeni font/ikon vs. gorunmez, bozuk cikar) -
	 * boyle durumda restart-tabanli yola dusulmeli.
	 */
	private static boolean assetsChanged(Path oldJar, Path newJar) {
		try {
			return !readAssetCrcs(oldJar).equals(readAssetCrcs(newJar));
		} catch (IOException e) {
			LOGGER.debug("Asset karsilastirmasi basarisiz, guvenli taraf secildi (restart istenecek): {}", e.toString());
			return true;
		}
	}

	private static java.util.Map<String, Long> readAssetCrcs(Path jarPath) throws IOException {
		java.util.Map<String, Long> result = new java.util.HashMap<>();
		try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jarPath.toFile())) {
			java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				java.util.zip.ZipEntry entry = entries.nextElement();
				if (!entry.isDirectory() && entry.getName().startsWith("assets/")) {
					result.put(entry.getName(), entry.getCrc());
				}
			}
		}
		return result;
	}

	private static boolean tryHotSwap(Path jarPath) {
		java.util.concurrent.CompletableFuture<Boolean> result = new java.util.concurrent.CompletableFuture<>();
		Minecraft.getInstance().execute(() -> result.complete(ModuleHolder.trySwap(jarPath)));
		try {
			return result.get();
		} catch (Exception e) {
			LOGGER.warn("Canli guncelleme sonucu alinamadi.", e);
			return false;
		}
	}

	private static void downloadWithProgress(HttpClient client, String url, Path target, IntConsumer onProgress)
			throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
		HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
		if (response.statusCode() != 200) {
			throw new IOException("Indirme basarisiz: HTTP " + response.statusCode());
		}
		long total = response.headers().firstValueAsLong("Content-Length").orElse(-1);
		int lastPercent = -1;
		try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(target)) {
			byte[] buffer = new byte[8192];
			long downloaded = 0;
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
				downloaded += read;
				if (total > 0) {
					int percent = (int) (downloaded * 100 / total);
					if (percent != lastPercent) {
						lastPercent = percent;
						onProgress.accept(percent);
					}
				}
			}
		}
	}

	private static void notifyUpdateToast(String version, String notes) {
		Minecraft client = Minecraft.getInstance();
		client.execute(() -> SystemToast.add(client.getToastManager(), TOAST_ID,
				Component.literal("Aura Friendly - Yeni surum: v" + version),
				Component.literal(summarize(notes))));
	}

	private static void notifyProgressToast(String version, int percent) {
		Minecraft client = Minecraft.getInstance();
		client.execute(() -> SystemToast.addOrUpdate(client.getToastManager(), TOAST_ID,
				Component.literal("Aura Friendly indiriliyor: v" + version),
				Component.literal("%" + percent)));
	}

	private static void notifyLiveUpdateToast(String version) {
		Minecraft client = Minecraft.getInstance();
		client.execute(() -> SystemToast.addOrUpdate(client.getToastManager(), TOAST_ID,
				Component.literal("Aura Friendly guncellendi!"),
				Component.literal("v" + version + " canli olarak devreye girdi.")));
	}

	private static void notifyRestartToast() {
		Minecraft client = Minecraft.getInstance();
		client.execute(() -> SystemToast.addOrUpdate(client.getToastManager(), TOAST_ID,
				Component.literal("Aura Friendly guncellendi"),
				Component.literal("Devreye girmesi icin oyunu kapatip tekrar ac.")));
	}

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

	private static void report(Consumer<String> onStatus, String status) {
		Minecraft.getInstance().execute(() -> onStatus.accept(status));
	}
}
