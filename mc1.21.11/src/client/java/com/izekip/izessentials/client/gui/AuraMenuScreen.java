package com.izekip.izessentials.client.gui;

import com.izekip.izessentials.client.config.screen.ShortcutConfigScreen;
import com.izekip.izessentials.client.core.ModuleUpdater;
import com.izekip.izessentials.client.friends.FriendsManager;
import com.izekip.izessentials.client.friends.PresenceManager;
import com.izekip.izessentials.client.friends.model.FriendRecord;
import com.izekip.izessentials.client.friends.model.FriendState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

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
	private static final int ONLINE_COLOR = 0xFF5BD97A;
	private static final int OFFLINE_COLOR = 0xFF7A7A8A;
	private static final int DIM_COLOR = 0xFF9A9AAA;

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

		renderFriendsOverview(guiGraphics);

		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	/**
	 * Sag icerik alaninda arkadaslarin ozeti. Ayri ekrana girmeden kim cevrimici, hangi sunucuda
	 * gorunur. Tamamen salt-okunur cizim - butonlar "Arkadaslar" ekraninda kaliyor, boylece bu
	 * ekran her karede widget olusturmaz (performans) ve mevcut davranis bozulmaz.
	 */
	private void renderFriendsOverview(GuiGraphics guiGraphics) {
		int x = SIDEBAR_WIDTH + PADDING * 2;
		int y = PADDING;
		int lineHeight = this.font.lineHeight + 2;

		guiGraphics.drawString(this.font, Component.literal("ARKADASLAR"), x, y, TITLE_RIGHT_COLOR);
		y += lineHeight + 4;

		if (!PresenceManager.isSessionActive()) {
			String message = PresenceManager.needsLogin()
					? "Izekip Cloud'a giris yapilmadi."
					: "Izekip Cloud'a baglaniliyor...";
			guiGraphics.drawString(this.font, Component.literal(message), x, y, DIM_COLOR);
			return;
		}

		FriendsManager manager = PresenceManager.getFriendsManager();
		if (manager == null) {
			return;
		}

		List<FriendRecord> friends = manager.getFriends().stream()
				.filter(f -> f.state == FriendState.ACCEPTED)
				.sorted(AuraMenuScreen::compareFriends)
				.toList();

		if (friends.isEmpty()) {
			guiGraphics.drawString(this.font, Component.literal("Henuz arkadasin yok."), x, y, DIM_COLOR);
			y += lineHeight;
			guiGraphics.drawString(this.font, Component.literal("Soldaki 'Arkadaslar' menusunden ekleyebilirsin."), x, y, DIM_COLOR);
			return;
		}

		long onlineCount = friends.stream().filter(f -> f.online).count();
		guiGraphics.drawString(this.font, Component.literal(onlineCount + " / " + friends.size() + " cevrimici"), x, y, DIM_COLOR);
		y += lineHeight + 4;

		// Ekranin altini tasmadan sigacak kadarini ciz.
		int available = (this.height - PADDING - y) / lineHeight;
		int shown = Math.min(friends.size(), Math.max(available - 1, 0));

		for (int i = 0; i < shown; i++) {
			FriendRecord friend = friends.get(i);
			String name = friend.username != null ? friend.username : friend.uuid.toString().substring(0, 8);
			String line = (friend.online ? "* " : "o ") + name;
			if (friend.online && friend.serverAddress != null && !friend.serverAddress.isBlank()) {
				line = line + "  -  " + friend.serverAddress;
			}
			guiGraphics.drawString(this.font, Component.literal(line), x, y, friend.online ? ONLINE_COLOR : OFFLINE_COLOR);
			y += lineHeight;
		}

		int hidden = friends.size() - shown;
		if (hidden > 0) {
			guiGraphics.drawString(this.font, Component.literal("+" + hidden + " kisi daha"), x, y, DIM_COLOR);
		}
	}

	/** Cevrimiciler once, sonra isme gore alfabetik. */
	private static int compareFriends(FriendRecord a, FriendRecord b) {
		if (a.online != b.online) {
			return a.online ? -1 : 1;
		}
		String an = a.username != null ? a.username : a.uuid.toString();
		String bn = b.username != null ? b.username : b.uuid.toString();
		return an.compareToIgnoreCase(bn);
	}

	private void closeScreen() {
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void onClose() {
		closeScreen();
	}
}
