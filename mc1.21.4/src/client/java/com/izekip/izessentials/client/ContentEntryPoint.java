package com.izekip.izessentials.client;

import com.izekip.izessentials.client.config.ConfigManager;
import com.izekip.izessentials.client.config.ModConfig;
import com.izekip.izessentials.client.config.ShortcutEntry;
import com.izekip.izessentials.client.core.AuraFriendlyModule;
import com.izekip.izessentials.client.friends.PresenceManager;
import com.izekip.izessentials.client.gui.AuraMenuScreen;
import com.izekip.izessentials.client.macro.ChatSender;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Degistirilebilir icerik modulunun giris noktasi - ModuleHolder bu sinifi (tam olarak bu
 * paket+isimle) reflection ile bulup ornekliyor. Sinif adini/paketini DEGISTIRME, yoksa
 * ModuleHolder onu bulamaz.
 *
 * Buradan asagisi (config, friends, gui, macro.ChatSender) canli guncellemeyle degisebilen
 * "icerik" - tus kayitlari/Fabric event kayitlari (core paketi) haric her sey.
 */
public class ContentEntryPoint implements AuraFriendlyModule {
	@Override
	public void onLoad() {
		ConfigManager.get();
	}

	@Override
	public void onJoin(Minecraft client, String serverAddress) {
		PresenceManager.start(serverAddress);
	}

	@Override
	public void onDisconnect(Minecraft client) {
		PresenceManager.stop();
	}

	@Override
	public void onOpenMenu(Minecraft client) {
		client.setScreen(new AuraMenuScreen(null));
	}

	@Override
	public void onMacroSlotPressed(Minecraft client, int slotIndex) {
		ModConfig config = ConfigManager.get();
		List<ShortcutEntry> shortcuts = config.shortcuts;
		if (slotIndex < 0 || slotIndex >= shortcuts.size()) {
			return;
		}
		ShortcutEntry entry = shortcuts.get(slotIndex);
		if (entry != null && entry.enabled) {
			ChatSender.send(entry.commandText);
		}
	}

	@Override
	public void onUnload(Minecraft client) {
		PresenceManager.stop();
		// Su an acik olan ekran bu modulun (bu classloader'in) kendi ekranlarindansa kapat -
		// yoksa eski classloader'a ait bir Screen nesnesi asilida kalir (sizinti).
		if (client.screen != null && client.screen.getClass().getClassLoader() == this.getClass().getClassLoader()) {
			client.setScreen(null);
		}
	}
}
