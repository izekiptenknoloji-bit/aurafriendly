package com.izekip.izessentials.client.gui;

import com.izekip.izessentials.client.config.screen.ShortcutConfigScreen;
import com.izekip.izessentials.client.core.ModuleUpdater;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Izekip Aura ana menusu - Essential/Feather Client tarzi sol sidebar duzeni.
 * Ortadaki vanilla-tarzi buton yigini yerine sol tarafta ince kenarlikli, yari saydam
 * butonlardan olusan dikey bir panel var; sag taraf bos (icerik ayri ekranlarda acilir).
 * Varsayilan tus: L.
 */
public class AuraMenuScreen extends Screen {
	private static final int SIDEBAR_WIDTH = 180;
	private static final int PADDING = 12;
	private static final int BUTTON_HEIGHT = 22;
	private static final int GAP = 6;

	private static final int GRADIENT_TOP = 0xAA0A0A10;
	private static final int GRADIENT_BOTTOM = 0xDD05050A;
	private static final int TITLE_LEFT_COLOR = 0xFF7A52CC;
	private static final int TITLE_RIGHT_COLOR = 0xFF52CCC9;
	private static final int VERSION_COLOR = 0xFFAAAAAA;

	private final Screen parent;

	public AuraMenuScreen(Screen parent) {
		super(Component.literal("Izekip Aura"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int buttonWidth = SIDEBAR_WIDTH - PADDING * 2;
		int y = 46;

		addRenderableWidget(new SidebarButton(this.font, PADDING, y, buttonWidth, BUTTON_HEIGHT,
				Component.literal("Kisayollar"), b -> this.minecraft.setScreen(new ShortcutConfigScreen(this))));
		y += BUTTON_HEIGHT + GAP;

		addRenderableWidget(new SidebarButton(this.font, PADDING, y, buttonWidth, BUTTON_HEIGHT,
				Component.literal("Arkadaslar"), b -> this.minecraft.setScreen(new FriendsListScreen(this))));
		y += BUTTON_HEIGHT + GAP;

		addRenderableWidget(new SidebarButton(this.font, PADDING, y, buttonWidth, BUTTON_HEIGHT,
				Component.literal("Guncellemeler"), b -> this.minecraft.setScreen(new UpdateInfoScreen(this))));
		y += BUTTON_HEIGHT + GAP;

		addRenderableWidget(new SidebarButton(this.font, PADDING, y, buttonWidth, BUTTON_HEIGHT,
				Component.literal("Hesap Bilgilerim"), b -> this.minecraft.setScreen(new AccountInfoScreen(this))));
		y += BUTTON_HEIGHT + GAP;

		addRenderableWidget(new SidebarButton(this.font, PADDING, y, buttonWidth, BUTTON_HEIGHT,
				Component.literal("Gizlilik ve Guvenlik"), b -> this.minecraft.setScreen(new PrivacySecurityScreen(this))));
		y += BUTTON_HEIGHT + GAP * 3;

		addRenderableWidget(new SidebarButton(this.font, PADDING, y, buttonWidth, BUTTON_HEIGHT,
				Component.literal("Kapat"), b -> closeScreen()));
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// Ortadaki gri kutu yok - tum ekrana yumusak siyah-mor dikey gradyan, dunya arkada hafifce secilir.
		guiGraphics.fillGradient(0, 0, this.width, this.height, GRADIENT_TOP, GRADIENT_BOTTOM);

		Component titleLeft = Component.literal("IZEKIP ");
		Component titleRight = Component.literal("AURA");
		guiGraphics.drawString(this.font, titleLeft, PADDING, PADDING, TITLE_LEFT_COLOR);
		guiGraphics.drawString(this.font, titleRight, PADDING + this.font.width(titleLeft), PADDING, TITLE_RIGHT_COLOR);

		String version = "v" + ModuleUpdater.getLocalVersionFriendly();
		guiGraphics.drawString(this.font, version, PADDING, PADDING + this.font.lineHeight + 3, VERSION_COLOR);

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
