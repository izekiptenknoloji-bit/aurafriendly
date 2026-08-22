package com.izekip.izessentials.client.gui;

import com.izekip.izessentials.client.config.screen.ShortcutConfigScreen;
import com.izekip.izessentials.client.core.ModuleUpdater;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Izekip Aura ana menusu - Essential/Feather Client tarzi sol sidebar duzeni.
 * Bu ekrana OZEL: kendi fontu (Inter, assets/izekip/font/inter.ttf), tam opak arka plan
 * (dunya/scoreboard tamamen kapali), GitHub butonu. Bunlarin hicbiri oyunun geri kalanini
 * etkilemez - font sadece bu ekranda insa ettigimiz Component'lere uygulanir.
 * Varsayilan tus: L.
 */
public class AuraMenuScreen extends Screen {
	/** Degistirilebilir GitHub linki. */
	public static final String PUBLIC_GITHUB_URL = "https://github.com/izekip";

	private static final Identifier CUSTOM_FONT = Identifier.fromNamespaceAndPath("izekip", "inter");
	private static final Identifier GITHUB_ICON = Identifier.fromNamespaceAndPath("izekip", "textures/gui/github_icon.png");
	private static final Style FONT_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(CUSTOM_FONT));

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
		super(styled("Izekip Aura"));
		this.parent = parent;
	}

	/** Bu ekranda insa edilen her metin bu ozel fontla cizilsin diye - baska hicbir yeri etkilemez. */
	static Component styled(String text) {
		return Component.literal(text).setStyle(FONT_STYLE);
	}

	@Override
	protected void init() {
		int buttonWidth = SIDEBAR_WIDTH - PADDING * 2;
		int y = 46;

		addRenderableWidget(new SidebarButton(this.font, PADDING, y, buttonWidth, BUTTON_HEIGHT,
				styled("Kisayollar"), b -> this.minecraft.setScreen(new ShortcutConfigScreen(this))));
		y += BUTTON_HEIGHT + GAP;

		addRenderableWidget(new SidebarButton(this.font, PADDING, y, buttonWidth, BUTTON_HEIGHT,
				styled("Arkadaslar"), b -> this.minecraft.setScreen(new FriendsListScreen(this))));
		y += BUTTON_HEIGHT + GAP;

		addRenderableWidget(new SidebarButton(this.font, PADDING, y, buttonWidth, BUTTON_HEIGHT,
				styled("Guncellemeler"), b -> this.minecraft.setScreen(new UpdateInfoScreen(this))));
		y += BUTTON_HEIGHT + GAP;

		addRenderableWidget(new SidebarButton(this.font, PADDING, y, buttonWidth, BUTTON_HEIGHT,
				styled("Hesap Bilgilerim"), b -> this.minecraft.setScreen(new AccountInfoScreen(this))));
		y += BUTTON_HEIGHT + GAP;

		addRenderableWidget(new SidebarButton(this.font, PADDING, y, buttonWidth, BUTTON_HEIGHT,
				styled("Gizlilik ve Guvenlik"), b -> this.minecraft.setScreen(new PrivacySecurityScreen(this))));
		y += BUTTON_HEIGHT + GAP;

		addRenderableWidget(new SidebarButton(this.font, PADDING, y, buttonWidth, BUTTON_HEIGHT,
				styled("GitHub"), ConfirmLinkScreen.confirmLink(this, PUBLIC_GITHUB_URL), GITHUB_ICON, 12));
		y += BUTTON_HEIGHT + GAP * 3;

		addRenderableWidget(new SidebarButton(this.font, PADDING, y, buttonWidth, BUTTON_HEIGHT,
				styled("Kapat"), b -> closeScreen()));
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// Tam opak arka plan - dunya/scoreboard tamamen kapali (sadece bu ekranda gecerli).
		guiGraphics.fill(0, 0, SIDEBAR_WIDTH, this.height, SIDEBAR_BG);
		guiGraphics.fill(SIDEBAR_WIDTH, 0, this.width, this.height, CONTENT_BG);

		Component titleLeft = styled("IZEKIP ");
		Component titleRight = styled("AURA");
		guiGraphics.drawString(this.font, titleLeft, PADDING, PADDING, TITLE_LEFT_COLOR);
		guiGraphics.drawString(this.font, titleRight, PADDING + this.font.width(titleLeft), PADDING, TITLE_RIGHT_COLOR);

		Component version = styled("v" + ModuleUpdater.getLocalVersionFriendly());
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
