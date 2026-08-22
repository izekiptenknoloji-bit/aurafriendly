package com.izekip.izessentials.client.gui;

import com.izekip.izessentials.client.core.ModuleUpdater;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Mevcut surum bilgisi + manuel "Simdi Kontrol Et" butonu. */
public class UpdateInfoScreen extends Screen {
	private final Screen parent;
	private StringWidget statusWidget;

	public UpdateInfoScreen(Screen parent) {
		super(Component.literal("Aura Friendly - Guncellemeler"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int left = AuraTheme.contentLeft(this.width);
		int right = AuraTheme.contentRight(this.width);
		int centerX = (left + right) / 2;
		int top = AuraTheme.CONTENT_TOP;
		int bottom = AuraTheme.contentBottom(this.height);
		int contentWidth = right - left;

		Component versionLine = AuraTheme.colored("Mevcut surum: v" + ModuleUpdater.getLocalVersionFriendly(), AuraTheme.TEXT_WHITE);
		addRenderableWidget(new StringWidget(left, top, contentWidth, this.font.lineHeight, versionLine, this.font));

		Component repoLine = AuraTheme.colored("github.com/izekiptenknoloji-bit/aurafriendly/releases", AuraTheme.TEXT_MUTED);
		addRenderableWidget(new StringWidget(left, top + 16, contentWidth, this.font.lineHeight, repoLine, this.font));

		this.statusWidget = new StringWidget(left, top + 40, contentWidth, this.font.lineHeight, Component.literal(""), this.font);
		addRenderableWidget(this.statusWidget);

		addRenderableWidget(Button.builder(Component.literal("Simdi Kontrol Et"), b -> checkNow())
				.bounds(centerX - 75, top + 60, 150, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> closeScreen())
				.bounds(centerX - 50, bottom - 20, 100, 20).build());
	}

	private void checkNow() {
		statusWidget.setMessage(AuraTheme.colored("Kontrol ediliyor...", AuraTheme.TEXT_MUTED));
		ModuleUpdater.checkNow(status -> statusWidget.setMessage(AuraTheme.colored(status, AuraTheme.TEXT_WHITE)));
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
