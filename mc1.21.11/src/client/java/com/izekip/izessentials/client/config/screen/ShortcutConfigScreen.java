package com.izekip.izessentials.client.config.screen;

import com.izekip.izessentials.client.config.ConfigManager;
import com.izekip.izessentials.client.config.ModConfig;
import com.izekip.izessentials.client.gui.AuraTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Aura Friendly kisayollarini (12 slot) duzenleme ekrani.
 * Calisma kopyasi uzerinde duzenlenir, sadece "Kaydet"e basinca diske yazilir (bkz. ModConfig.copy()).
 */
public class ShortcutConfigScreen extends Screen {
	private final Screen parent;
	private ModConfig workingConfig;
	private ShortcutListWidget list;

	public ShortcutConfigScreen(Screen parent) {
		super(Component.literal("Aura Friendly Kisayollari"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.workingConfig = ConfigManager.get().copy();

		int left = AuraTheme.contentLeft(this.width);
		int right = AuraTheme.contentRight(this.width);
		int centerX = (left + right) / 2;
		int top = AuraTheme.CONTENT_TOP;
		int bottom = AuraTheme.contentBottom(this.height);

		Component hint = AuraTheme.colored("Tus atamak icin: Secenekler -> Kontroller -> Aura Friendly", AuraTheme.TEXT_MUTED);
		int hintWidth = this.font.width(hint);
		addRenderableWidget(new StringWidget((this.width - hintWidth) / 2, bottom - 40, hintWidth, this.font.lineHeight, hint, this.font));

		int listTop = top;
		int listBottom = bottom - 46;
		this.list = new ShortcutListWidget(this.minecraft, this.width, listBottom - listTop, listTop, this.font, this.workingConfig.shortcuts);
		addRenderableWidget(this.list);

		addRenderableWidget(Button.builder(Component.literal("Kaydet"), button -> {
			ConfigManager.save(this.workingConfig);
			closeScreen();
		}).bounds(centerX - 105, bottom - 20, 100, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Vazgec"), button -> closeScreen())
				.bounds(centerX + 5, bottom - 20, 100, 20).build());
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
