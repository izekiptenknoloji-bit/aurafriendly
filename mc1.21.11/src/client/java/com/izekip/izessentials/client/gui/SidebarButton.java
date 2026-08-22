package com.izekip.izessentials.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Essential/Feather Client tarzi sade sidebar butonu. Vanilla gri buton dokusu hic
 * kullanilmaz - renderDefaultSprite() cagrilmadigi surece devreye girmiyor (bkz. AbstractButton).
 * Sola hizali metin, yumusatilmis (rounded) kenarlar, hover'da parlayan renkler.
 * Istege bagli olarak metnin soluna kucuk bir ikon (ornegin GitHub rozeti) cizilebilir.
 *
 * Yuvarlatma teknigi: Minecraft'ta gercek egri cizim yok, bu yuzden dolgu bir "arti/artiy"
 * seklinde (yatay + dikey iki dikdortgen) cizilir - kose kareleri hic boyanmaz, boylece
 * arkada ne olursa olsun (gradyan, dunya) oradan gorunur ve kose yumusamis gibi durur.
 * Kenarlik da 4 ayri kisa cizgi olarak, koselere deymeden cizilir.
 */
public class SidebarButton extends Button {
	private static final int BG_DEFAULT = 0x880F0F1A;
	private static final int BORDER_DEFAULT = 0x447A52CC;
	private static final int BG_HOVER = 0xCC1A1A2E;
	private static final int BORDER_HOVER = 0xFF7A52CC;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int TEXT_PADDING = 10;
	private static final int ICON_TEXT_GAP = 6;
	private static final int RADIUS = 4;

	private final Font font;
	private final Identifier icon;
	private final int iconSize;

	public SidebarButton(Font font, int x, int y, int width, int height, Component message, Button.OnPress onPress) {
		this(font, x, y, width, height, message, onPress, null, 0);
	}

	public SidebarButton(Font font, int x, int y, int width, int height, Component message, Button.OnPress onPress,
			Identifier icon, int iconSize) {
		super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
		this.font = font;
		this.icon = icon;
		this.iconSize = iconSize;
	}

	@Override
	protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		boolean hovered = isHoveredOrFocused();
		int bg = hovered ? BG_HOVER : BG_DEFAULT;
		int border = hovered ? BORDER_HOVER : BORDER_DEFAULT;

		drawRoundedRect(guiGraphics, getX(), getY(), getWidth(), getHeight(), RADIUS, bg, border);

		int textX = getX() + TEXT_PADDING;
		if (icon != null) {
			int iconY = getY() + (getHeight() - iconSize) / 2;
			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, textX, iconY, 0f, 0f, iconSize, iconSize, iconSize, iconSize);
			textX += iconSize + ICON_TEXT_GAP;
		}

		int textY = getY() + (getHeight() - font.lineHeight) / 2;
		guiGraphics.drawString(font, getMessage(), textX, textY, TEXT_COLOR);
	}

	/** Kose kareleri bos birakilan "yumusak kenarli" dikdortgen - herhangi bir arka planla calisir. */
	static void drawRoundedRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int fillColor, int borderColor) {
		int r = Math.min(radius, Math.min(width, height) / 2);

		// Dolgu: yatay + dikey seritlerin bilesimi (kose kareleri bos kalir).
		guiGraphics.fill(x + r, y, x + width - r, y + height, fillColor);
		guiGraphics.fill(x, y + r, x + width, y + height - r, fillColor);

		// Kenarlik: 4 kisa cizgi, koselere degmeden.
		guiGraphics.fill(x + r, y, x + width - r, y + 1, borderColor);
		guiGraphics.fill(x + r, y + height - 1, x + width - r, y + height, borderColor);
		guiGraphics.fill(x, y + r, x + 1, y + height - r, borderColor);
		guiGraphics.fill(x + width - 1, y + r, x + width, y + height - r, borderColor);
	}
}
