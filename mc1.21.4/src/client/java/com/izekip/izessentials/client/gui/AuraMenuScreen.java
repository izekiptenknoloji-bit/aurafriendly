package com.izekip.izessentials.client.gui;

import com.izekip.izessentials.client.config.screen.ShortcutConfigScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Aura Friendly'nin ana menusu - diger tum ekranlara buradan gidilir. Varsayilan tus: L. */
public class AuraMenuScreen extends Screen {
	private final Screen parent;

	public AuraMenuScreen(Screen parent) {
		super(Component.literal("Aura Friendly"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int left = AuraTheme.contentLeft(this.width);
		int right = AuraTheme.contentRight(this.width);
		int centerX = (left + right) / 2;
		int buttonWidth = Math.min(200, right - left);
		int top = AuraTheme.CONTENT_TOP + 10;
		int spacing = 24;

		addRenderableWidget(Button.builder(Component.literal("Kisayollar"), b -> this.minecraft.setScreen(new ShortcutConfigScreen(this)))
				.bounds(centerX - buttonWidth / 2, top, buttonWidth, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Arkadaslar"), b -> this.minecraft.setScreen(new FriendsListScreen(this)))
				.bounds(centerX - buttonWidth / 2, top + spacing, buttonWidth, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Guncellemeler"), b -> this.minecraft.setScreen(new UpdateInfoScreen(this)))
				.bounds(centerX - buttonWidth / 2, top + spacing * 2, buttonWidth, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Gizlilik ve Guvenlik"), b -> this.minecraft.setScreen(new PrivacySecurityScreen(this)))
				.bounds(centerX - buttonWidth / 2, top + spacing * 3, buttonWidth, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> closeScreen())
				.bounds(centerX - buttonWidth / 2, top + spacing * 4 + 10, buttonWidth, 20).build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		AuraTheme.renderPanel(guiGraphics, this.width, this.height, this.title, this.font);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	private void closeScreen() {
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void onClose() {
		closeScreen();
	}
}
