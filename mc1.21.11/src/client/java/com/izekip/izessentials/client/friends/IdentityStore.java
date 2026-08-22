package com.izekip.izessentials.client.friends;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Oyuncunun EN SON oyun ici kimligini (UUID + kullanici adi) saklar.
 *
 * Neden gerekli: ana ekranda (TitleScreen) henuz bir LocalPlayer yoktur, dolayisiyla
 * player.getUUID() cagrilamaz. Minecraft.getUser().getProfileId() de guvenli degil - offline/cracked
 * baslaticilarda bu deger sunucunun atadigi offline UUID ile ayni olmayabilir (bkz. v0.6.7'deki
 * UUID uyusmazligi hatasi). Bu yuzden sunucuya girildiginde GERCEKTEN kullanilan UUID'yi diske
 * yazar, ana ekranda arkadas listesini onunla okuruz.
 *
 * Dosya kaybolursa/bozuksa hicbir sey patlamaz: sadece ana ekranda liste gosterilmez, oyuncu bir
 * kez sunucuya girince kayit tazelenir.
 */
public final class IdentityStore {
	private static final Logger LOGGER = LoggerFactory.getLogger("aurafriendly/identity");
	private static final Gson GSON = new Gson();

	private static volatile Identity cached;

	private IdentityStore() {
	}

	/** Diskteki kayit (yoksa null). Bir kez okunup bellekte tutulur. */
	public static Identity get() {
		Identity local = cached;
		if (local != null) {
			return local;
		}
		try {
			Path file = file();
			if (Files.exists(file)) {
				Identity loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Identity.class);
				if (loaded != null && loaded.uuid != null && !loaded.uuid.isBlank()) {
					cached = loaded;
					return loaded;
				}
			}
		} catch (Exception e) {
			LOGGER.debug("Kimlik dosyasi okunamadi (onemli degil): {}", e.toString());
		}
		return null;
	}

	/** Oyun ici oturum baslarken cagrilir. Ayni deger zaten kayitliysa diske hic dokunmaz. */
	public static void save(String uuid, String username) {
		if (uuid == null || uuid.isBlank()) {
			return;
		}
		Identity current = get();
		if (current != null && uuid.equals(current.uuid) && java.util.Objects.equals(username, current.username)) {
			return;
		}

		Identity identity = new Identity();
		identity.uuid = uuid;
		identity.username = username;
		cached = identity;

		try {
			Path file = file();
			Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
			Files.writeString(tmp, GSON.toJson(identity), StandardCharsets.UTF_8);
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (Exception e) {
			LOGGER.debug("Kimlik dosyasi yazilamadi (onemli degil): {}", e.toString());
		}
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("aurafriendly_identity.json");
	}

	public static final class Identity {
		public String uuid;
		public String username;
	}
}
