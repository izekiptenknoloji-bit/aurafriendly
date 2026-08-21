package com.izekip.izessentials.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * Tum Aura Friendly ekranlarinin ortak gorsel temasi: mor -> mavi "aura" paleti.
 * Sadece GuiGraphics'in kendi fill/fillGradient/renderOutline metotlarini kullanir,
 * disaridan hicbir texture/asset gerektirmez.
 */
public final class AuraTheme {
	public static final int ACCENT_PURPLE = 0xFF8A5CF6;
	public static final int ACCENT_BLUE = 0xFF3B82F6;
	public static final int PANEL_BG = 0xE0121018;
	public static final int TITLE_BAR_TOP = 0xFF6D3FD1;
	public static final int TITLE_BAR_BOTTOM = 0xFF3B82F6;
	public static final int TEXT_WHITE = 0xFFFFFFFF;
	public static final int TEXT_MUTED = 0xFFB9B9C9;
	public static final int ONLINE_GREEN = 0xFF4ADE80;
	public static final int OFFLINE_GRAY = 0xFF8A8A9A;
	public static final int ERROR_RED = 0xFFF87171;

	public static final int PANEL_MARGIN = 8;
	public static final int TITLE_BAR_HEIGHT = 20;
	public static final int CONTENT_TOP = PANEL_MARGIN + TITLE_BAR_HEIGHT + 10;
	public static final int CONTENT_INSET = PANEL_MARGIN + 8;

	private AuraTheme() {
	}

	/** Ekranin arkasina mor->mavi baslik cubuklu, kenarlikli bir panel cizer. Screen.render() icinde super.render()'dan ONCE cagrilmali. */
	public static void renderPanel(GuiGraphics guiGraphics, int screenWidth, int screenHeight, Component title, Font font) {
		int x1 = PANEL_MARGIN;
		int y1 = PANEL_MARGIN;
		int x2 = screenWidth - PANEL_MARGIN;
		int y2 = screenHeight - PANEL_MARGIN;

		guiGraphics.fill(x1, y1, x2, y2, PANEL_BG);
		guiGraphics.fillGradient(x1, y1, x2, y1 + TITLE_BAR_HEIGHT, TITLE_BAR_TOP, TITLE_BAR_BOTTOM);
		guiGraphics.renderOutline(x1, y1, x2 - x1, y2 - y1, ACCENT_PURPLE);

		int textY = y1 + (TITLE_BAR_HEIGHT - font.lineHeight) / 2;
		guiGraphics.drawCenteredString(font, title, (x1 + x2) / 2, textY, TEXT_WHITE);
	}

	public static int contentLeft(int screenWidth) {
		return CONTENT_INSET;
	}

	public static int contentRight(int screenWidth) {
		return screenWidth - CONTENT_INSET;
	}

	public static int contentBottom(int screenHeight) {
		return screenHeight - PANEL_MARGIN - 8;
	}

	/** StringWidget'ta ayri bir setColor() metodu olmadigi/surume gore degistigi icin rengi Style uzerinden veriyoruz. */
	public static Component colored(String text, int argbColor) {
		return Component.literal(text).setStyle(Style.EMPTY.withColor(argbColor & 0xFFFFFF));
	}

	public static Component colored(Component text, int argbColor) {
		return text.copy().setStyle(Style.EMPTY.withColor(argbColor & 0xFFFFFF));
	}
}
