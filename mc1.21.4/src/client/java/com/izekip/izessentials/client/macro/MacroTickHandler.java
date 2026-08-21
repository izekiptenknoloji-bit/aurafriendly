package com.izekip.izessentials.client.macro;

import com.izekip.izessentials.client.config.ConfigManager;
import com.izekip.izessentials.client.config.ModConfig;
import com.izekip.izessentials.client.config.ShortcutEntry;
import com.izekip.izessentials.client.config.screen.ShortcutConfigScreen;
import com.izekip.izessentials.client.gui.FriendsListScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * ClientTickEvents.END_CLIENT_TICK ile bir kez kaydedilir (bkz. IzEssentialsClient).
 * Sabit boyutlu SLOTS dizisini gezer, per-tick allocation yapmaz, listener biriktirmez.
 */
public final class MacroTickHandler {
	private MacroTickHandler() {
	}

	public static void onEndTick(Minecraft client) {
		if (client.player == null) {
			// Dunyada degiliz (ana menu vs.) - tuslari tuketmeye gerek yok.
			return;
		}
		if (client.screen != null) {
			// Bir ekran acikken (chat, envanter, kendi ayar ekranimiz vs.) macro/aç-kapa
			// tuslarini tuketmiyoruz - metin kutusuna yazarken yanlislikla komut gitmesin.
			return;
		}

		if (MacroSlots.openConfig != null && MacroSlots.openConfig.consumeClick()) {
			Minecraft.getInstance().setScreen(new ShortcutConfigScreen(null));
			return;
		}
		if (MacroSlots.openFriends != null && MacroSlots.openFriends.consumeClick()) {
			Minecraft.getInstance().setScreen(new FriendsListScreen(null));
			return;
		}

		ModConfig config = ConfigManager.get();
		List<ShortcutEntry> shortcuts = config.shortcuts;
		KeyMapping[] slots = MacroSlots.SLOTS;

		for (int i = 0; i < slots.length; i++) {
			KeyMapping mapping = slots[i];
			if (mapping == null) {
				continue;
			}
			while (mapping.consumeClick()) {
				if (i >= shortcuts.size()) {
					continue;
				}
				ShortcutEntry entry = shortcuts.get(i);
				if (entry != null && entry.enabled) {
					ChatSender.send(entry.commandText);
				}
			}
		}
	}
}
