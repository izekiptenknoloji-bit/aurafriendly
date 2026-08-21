package com.izekip.izessentials.client.macro;

import com.izekip.izessentials.IzEssentials;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

import com.izekip.izessentials.client.config.ModConfig;

/**
 * Sabit sayida genel "macro slot" KeyMapping'i olarak kaydeder. Hangi tusun
 * hangi metni gonderecegi ModConfig'te tutulur (bkz. ChatSender, MacroTickHandler) -
 * boylece tus sayisi sabit kalirken gonderilen komut tamamen kullaniciya kalir.
 */
public final class MacroSlots {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(IzEssentials.id("main"));
	public static final KeyMapping[] SLOTS = new KeyMapping[ModConfig.SHORTCUT_SLOT_COUNT];
	public static KeyMapping openConfig;
	public static KeyMapping openFriends;

	private MacroSlots() {
	}

	public static void registerAll() {
		for (int i = 0; i < ModConfig.SHORTCUT_SLOT_COUNT; i++) {
			String translationKey = "key.aurafriendly.macro_slot_" + (i + 1);
			KeyMapping mapping = new KeyMapping(translationKey, InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);
			SLOTS[i] = KeyBindingHelper.registerKeyBinding(mapping);
		}

		KeyMapping openConfigMapping = new KeyMapping("key.aurafriendly.open_config", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);
		openConfig = KeyBindingHelper.registerKeyBinding(openConfigMapping);

		KeyMapping openFriendsMapping = new KeyMapping("key.aurafriendly.open_friends", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);
		openFriends = KeyBindingHelper.registerKeyBinding(openFriendsMapping);
	}
}
