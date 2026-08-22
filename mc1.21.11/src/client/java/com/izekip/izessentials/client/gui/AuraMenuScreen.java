package com.izekip.izessentials.client.gui;

import com.izekip.izessentials.client.config.screen.ShortcutConfigScreen;
import com.izekip.izessentials.client.core.ModuleUpdater;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Izekip Aura ana menusu - Essential/Feather Client tarzi sol sidebar duzeni.
 * Bu ekrana OZEL: tam opak arka plan (dunya/scoreboard tamamen kapali) ve GitHub butonu.
 * Yazilar Minecraft.in kendi fontuyla cizilir - oyunun geri kalanina hic dokunulmaz.
 * Varsayilan tus: L.
 */
public class AuraMenuScreen extends Screen {
	/** Degistirilebilir GitHub linki. */
	public static final String PUBLIC_GITHUB_URL = "https://github.com/izekip";

	private static final Identifier GITHUB_ICON = Identifier.fromNamespaceAndPath("izekip", "textures/gui/github_icon.png");

	private static final int SIDEBAR_WIDTH = 180;
	private static final int PADDING = 12;
	private static final int BUTTON_HEIGHT = 22;
	private static final int GAP = 6;

	private static final int SIDEBAR_BG = 0xFF0F0F1A;
	private static final int CONTENT_BG = 0xFF0A0A10;
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
		y += BUTTON_HEIGHT + GAP;

		addRenderableWidget(new SidebarButton(this.font, PADDING, y, buttonWidth, BUTTON_HEIGHT,
				Component.literal("GitHub"), ConfirmLinkScreen.confirmLink(this, PUBLIC_GITHUB_URL), GITHUB_ICON, 12));
		y += BUTTON_HEIGHT + GAP * 3;

		addRenderableWidget(new SidebarButton(this.font, PADDING, y, buttonWidth, BUTTON_HEIGHT,
				Component.literal("Kapat"), b -> closeScreen()));
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// Tam opak arka plan - dunya/scoreboard tamamen kapali (sadece bu ekranda gecerli).
		guiGraphics.fill(0, 0, SIDEBAR_WIDTH, this.height, SIDEBAR_BG);
		guiGraphics.fill(SIDEBAR_WIDTH, 0, this.width, this.height, CONTENT_BG);

		Component titleLeft = Component.literal("IZEKIP ");
		Component titleRight = Component.literal("AURA");
		guiGraphics.drawString(this.font, titleLeft, PADDING, PADDING, TITLE_LEFT_COLOR);
		guiGraphics.drawString(this.font, titleRight, PADDING + this.font.width(titleLeft), PADDING, TITLE_RIGHT_COLOR);

		Component version = Component.literal("v" + ModuleUpdater.getLocalVersionFriendly());
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
