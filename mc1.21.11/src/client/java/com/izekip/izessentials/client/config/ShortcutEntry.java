package com.izekip.izessentials.client.config;

/**
 * Bir macro slot'unun ayari: tusa basilinca ne gonderilecek.
 * commandText "/" ile basliyorsa komut olarak, yoksa duz chat mesaji olarak gonderilir.
 * Gson ile duz alanlar uzerinden (de)serialize edilir, bu yuzden alanlar public.
 */
public class ShortcutEntry {
	public String label;
	public String commandText;
	public boolean enabled;

	public ShortcutEntry() {
		this("", "", true);
	}

	public ShortcutEntry(String label, String commandText, boolean enabled) {
		this.label = label;
		this.commandText = commandText;
		this.enabled = enabled;
	}

	/** Ayar ekraninin uzerinde calistigi bagimsiz calisma kopyasi icin. */
	public ShortcutEntry copy() {
		return new ShortcutEntry(label, commandText, enabled);
	}
}
