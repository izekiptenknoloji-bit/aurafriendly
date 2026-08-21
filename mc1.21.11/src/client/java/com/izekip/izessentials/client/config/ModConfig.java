package com.izekip.izessentials.client.config;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
	public static final int SHORTCUT_SLOT_COUNT = 12;

	public List<ShortcutEntry> shortcuts = defaultShortcuts();

	private static List<ShortcutEntry> defaultShortcuts() {
		List<ShortcutEntry> list = new ArrayList<>(SHORTCUT_SLOT_COUNT);
		for (int i = 0; i < SHORTCUT_SLOT_COUNT; i++) {
			list.add(new ShortcutEntry("Slot " + (i + 1), "", true));
		}
		return list;
	}

	/** Ayar ekraninin uzerinde calisip diske sadece "Kaydet" ile yazacagi bagimsiz kopya. */
	public ModConfig copy() {
		ModConfig copy = new ModConfig();
		copy.shortcuts = new ArrayList<>(SHORTCUT_SLOT_COUNT);
		for (ShortcutEntry entry : shortcuts) {
			copy.shortcuts.add(entry.copy());
		}
		return copy;
	}
}
