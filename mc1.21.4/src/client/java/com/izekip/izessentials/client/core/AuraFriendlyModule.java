package com.izekip.izessentials.client.core;

import net.minecraft.client.Minecraft;

/**
 * Core (bu paket) ile degistirilebilir icerik modulu (com.izekip.izessentials.client.ContentEntryPoint,
 * "core" disindaki her sey) arasindaki sabit sozlesme. Bu arayuz DEGISTIRILMEMELI - degisirse
 * canli guncelleme (hot-swap) artik calismaz, ModuleHolder.trySwap() basarisiz doner ve
 * ModuleUpdater eski (yeniden baslatma gerektiren) yonteme duser. Bkz. ModuleHolder, ModuleUpdater.
 */
public interface AuraFriendlyModule {
	/** Modul ilk yuklendiginde (veya canli guncelleme sonrasi yeni modul icin) bir kez cagrilir. */
	void onLoad();

	/** ClientPlayConnectionEvents.JOIN. */
	void onJoin(Minecraft client, String serverAddress);

	/** ClientPlayConnectionEvents.DISCONNECT / ClientLifecycleEvents.CLIENT_STOPPING. */
	void onDisconnect(Minecraft client);

	/** Ana menu tusuna (varsayilan L) basildiginda. */
	void onOpenMenu(Minecraft client);

	/**
	 * Bir ekran acildiginda (ScreenEvents.AFTER_INIT). Kayit CORE.da yapilir; boylece canli
	 * guncellemede eski modulun callback.i global event listesinde asili kalmaz (sizinti olmaz).
	 * Icerik modulu burada ornegin ana ekrana (TitleScreen) kendi panelini ekler.
	 */
	void onScreenInit(Minecraft client, net.minecraft.client.gui.screens.Screen screen);

	/** Kisayol slotlarindan biri (0..11) tetiklendiginde. */
	void onMacroSlotPressed(Minecraft client, int slotIndex);

	/**
	 * Bu modul canli guncellemeyle degistirilmeden hemen once cagrilir. Burada:
	 *  - Kendi ag baglantilarini/thread'lerini durdur (bkz. PresenceManager.stop()).
	 *  - Eger su an acik olan ekran bu modulun kendi ekranlaridansa kapat.
	 * Aksi halde eski classloader'a ait nesneler (thread, acik ekran) sizar.
	 */
	void onUnload(Minecraft client);
}
