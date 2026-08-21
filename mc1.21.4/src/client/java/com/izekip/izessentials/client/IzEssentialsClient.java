package com.izekip.izessentials.client;

import com.izekip.izessentials.IzEssentials;
import com.izekip.izessentials.client.config.ConfigManager;
import com.izekip.izessentials.client.friends.PresenceManager;
import com.izekip.izessentials.client.macro.MacroSlots;
import com.izekip.izessentials.client.macro.MacroTickHandler;
import com.izekip.izessentials.client.update.ModUpdater;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.multiplayer.ServerData;

public class IzEssentialsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ConfigManager.get();
		MacroSlots.registerAll();
		ClientTickEvents.END_CLIENT_TICK.register(MacroTickHandler::onEndTick);

		ModUpdater.cleanupStaleJars();

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			ServerData server = client.getCurrentServer();
			String serverAddress = server != null ? server.ip : null;
			PresenceManager.start(serverAddress);
			ModUpdater.checkForUpdateAsync();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PresenceManager.stop());
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> PresenceManager.stop());

		IzEssentials.LOGGER.info("Aura Friendly client kisayol sistemi hazir.");
	}
}
