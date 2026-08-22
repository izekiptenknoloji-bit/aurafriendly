package com.izekip.izessentials.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Essential/Feather Client tarzi sade sidebar butonu. Vanilla gri buton dokusu hic
 * kullanilmaz - renderDefaultSprite() cagrilmadigi surece devreye girmiyor (bkz. AbstractButton).
 * Sola hizali metin, ince kenarlik, hover'da parlayan renkler.
 */
public class SidebarButton extends Button {
	private static final int BG_DEFAULT = 0x880F0F1A;
	private static final int BORDER_DEFAULT = 0x447A52CC;
	private static final int BG_HOVER = 0xCC1A1A2E;
	private static final int BORDER_HOVER = 0xFF7A52CC;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int TEXT_PADDING = 10;

	private final Font font;

	public SidebarButton(Font font, int x, int y, int width, int height, Component message, Button.OnPress onPress) {
		super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
		this.font = font;
	}

	@Override
	protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		boolean hovered = isHoveredOrFocused();
		int bg = hovered ? BG_HOVER : BG_DEFAULT;
		int border = hovered ? BORDER_HOVER : BORDER_DEFAULT;

		guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
		guiGraphics.renderOutline(getX(), getY(), getWidth(), getHeight(), border);

		int textY = getY() + (getHeight() - font.lineHeight) / 2;
		guiGraphics.drawString(font, getMessage(), getX() + TEXT_PADDING, textY, TEXT_COLOR);
	}
}
