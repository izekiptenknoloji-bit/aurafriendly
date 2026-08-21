package com.izekip.izessentials;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IzEssentials implements ModInitializer {
	public static final String MOD_ID = "aurafriendly";

	// Bu logger konsola ve log dosyasina yazmak icin kullanilir.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Minecraft mod yuklemeye hazir oldugunda bu kod calisir.
		// Iskelet asamadayiz: komutlar ve ozellikler daha sonra buraya eklenecek.

		LOGGER.info("Aura Friendly yuklendi!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
