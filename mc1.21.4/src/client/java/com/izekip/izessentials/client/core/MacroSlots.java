package com.izekip.izessentials.client.core;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

/**
 * Tus kaydi (KeyMapping) - CORE'un sabit sorumlulugu. Fabric'in KeyBindingHelper'inda
 * "unregister" diye bir sey olmadigi icin bu kayitlar canli guncellemeyle degistirilemez,
 * hep ayni kalir. Tusa basilinca ne olacagi (hangi metin gonderilecek, hangi ekran acilacak)
 * ise degistirilebilir icerik modulune (bkz. AuraFriendlyModule) yonlendirilir.
 *
 * NOT: 1.21.4'te KeyMapping.Category henuz bir kayit nesnesi degil, duz String kategori adi.
 */
public final class MacroSlots {
	public static final int SLOT_COUNT = 12;
	public static final String CATEGORY = "key.categories.aurafriendly";

	public static final KeyMapping[] SLOTS = new KeyMapping[SLOT_COUNT];
	public static KeyMapping openMenu;

	private MacroSlots() {
	}

	public static void registerAll() {
		for (int i = 0; i < SLOT_COUNT; i++) {
			String translationKey = "key.aurafriendly.macro_slot_" + (i + 1);
			KeyMapping mapping = new KeyMapping(translationKey, InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);
			SLOTS[i] = KeyBindingHelper.registerKeyBinding(mapping);
		}

		KeyMapping openMenuMapping = new KeyMapping("key.aurafriendly.open_menu", InputConstants.Type.KEYSYM, InputConstants.KEY_L, CATEGORY);
		openMenu = KeyBindingHelper.registerKeyBinding(openMenuMapping);
	}
}
