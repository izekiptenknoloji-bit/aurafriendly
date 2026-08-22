package com.izekip.izessentials.client.core;

import net.minecraft.client.Minecraft;

/** Ilk icerik modulu yuklenene kadar (ya da yukleme basarisiz olursa) guvenli varsayilan - hicbir sey yapmaz, crash olmaz. */
public final class NoOpModule implements AuraFriendlyModule {
	@Override
	public void onLoad() {
	}

	@Override
	public void onJoin(Minecraft client, String serverAddress) {
	}

	@Override
	public void onDisconnect(Minecraft client) {
	}

	@Override
	public void onOpenMenu(Minecraft client) {
	}

	@Override
	public void onMacroSlotPressed(Minecraft client, int slotIndex) {
	}

	@Override
	public void onUnload(Minecraft client) {
	}
}
