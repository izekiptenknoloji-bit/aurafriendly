package com.izekip.izessentials.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * config/aurafriendly.json dosyasini okur/yazar. Bozuk/eksik dosya durumunda
 * client'in acilisini asla patlatmaz: taze varsayilan config'e duser.
 */
public final class ConfigManager {
	private static final Logger LOGGER = LoggerFactory.getLogger("aurafriendly/config");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("aurafriendly.json");

	private static ModConfig instance;

	private ConfigManager() {
	}

	public static synchronized ModConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	private static ModConfig load() {
		if (!Files.exists(CONFIG_PATH)) {
			ModConfig fresh = new ModConfig();
			save(fresh);
			return fresh;
		}

		try {
			String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
			ModConfig loaded = GSON.fromJson(json, ModConfig.class);
			if (loaded == null || loaded.shortcuts == null || loaded.shortcuts.size() != ModConfig.SHORTCUT_SLOT_COUNT) {
				LOGGER.warn("aurafriendly.json eksik/gecersiz, varsayilan config kullaniliyor.");
				ModConfig fresh = new ModConfig();
				save(fresh);
				return fresh;
			}
			return loaded;
		} catch (IOException | JsonSyntaxException e) {
			LOGGER.warn("aurafriendly.json okunamadi, varsayilan config ile devam ediliyor.", e);
			ModConfig fresh = new ModConfig();
			save(fresh);
			return fresh;
		}
	}

	public static synchronized void save(ModConfig config) {
		instance = config;
		try {
			Path tmp = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
			Files.writeString(tmp, GSON.toJson(config), StandardCharsets.UTF_8);
			Files.move(tmp, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			LOGGER.error("aurafriendly.json yazilamadi.", e);
		}
	}
}
