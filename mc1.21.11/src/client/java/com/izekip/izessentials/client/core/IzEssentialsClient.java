package com.izekip.izessentials.client.core;

import com.izekip.izessentials.IzEssentials;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Fabric giris noktasi - CORE'un kendisi. Burada sadece: tus kaydi, Fabric event kayitlari
 * (hepsi ModuleHolder.current()'a yonlendirilir) ve guncelleme kontrolu baslatilir. Ekranlar,
 * arkadaslik, Firebase gibi asil ozellikler ContentEntryPoint'te (degistirilebilir modul) yasar.
 */
public class IzEssentialsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MacroSlots.registerAll();
		ModuleUpdater.cleanupStaleJars();
		ModuleHolder.loadInitial();

		ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			ServerData server = client.getCurrentServer();
			String serverAddress = server != null ? server.ip : null;
			ModuleHolder.current().onJoin(client, serverAddress);
			ModuleUpdater.checkForUpdateAsync();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ModuleHolder.current().onDisconnect(client));
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ModuleHolder.current().onDisconnect(client));

		// Ekran acilis olayi: kayit burada (core) yapilir ki canli guncellemede eski modulun
		// callback.i global event listesinde asili kalip classloader sizdirmasin.
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
				ModuleHolder.current().onScreenInit(client, screen));

		IzEssentials.LOGGER.info("Aura Friendly core hazir.");
	}

	private void onEndTick(Minecraft client) {
		if (client.player == null || client.screen != null) {
			// Dunyada degiliz, ya da bir ekran acik (chat/envanter/kendi ekranlarimiz) -
			// metin kutusuna yazarken yanlislikla komut gitmesin diye tuketmiyoruz.
			return;
		}

		if (MacroSlots.openMenu != null && MacroSlots.openMenu.consumeClick()) {
			ModuleHolder.current().onOpenMenu(client);
			return;
		}

		KeyMapping[] slots = MacroSlots.SLOTS;
		for (int i = 0; i < slots.length; i++) {
			KeyMapping mapping = slots[i];
			if (mapping == null) {
				continue;
			}
			while (mapping.consumeClick()) {
				ModuleHolder.current().onMacroSlotPressed(client, i);
			}
		}
	}
}
