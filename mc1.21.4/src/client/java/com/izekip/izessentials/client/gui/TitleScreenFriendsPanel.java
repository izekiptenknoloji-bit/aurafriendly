package com.izekip.izessentials.client.gui;

import com.izekip.izessentials.client.friends.FriendsManager;
import com.izekip.izessentials.client.friends.PresenceManager;
import com.izekip.izessentials.client.friends.model.FriendRecord;
import com.izekip.izessentials.client.friends.model.FriendState;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Minecraft'in ana ekranina (TitleScreen) eklenen kompakt arkadas paneli.
 * Sag alt kosede durur; kimin cevrimici oldugunu ve hangi sunucuda oldugunu gosterir.
 * Salt-okunurdur - burada kendi durumumuzu yayinlamayiz (bkz. PresenceManager.startMenu()).
 *
 * Cizim, ekrana ozel ScreenEvents.afterRender ile yapilir: bu olay ekran kapaninca otomatik
 * dusar, dolayisiyla global bir dinleyici birikmez.
 */
public final class TitleScreenFriendsPanel {
	private static final int WIDTH = 150;
	private static final int PADDING = 6;
	private static final int LINE_HEIGHT = 10;
	private static final int MARGIN = 8;
	private static final int MAX_ROWS = 6;

	private static final int BG = 0xCC0F0F1A;
	private static final int BORDER = 0x667A52CC;
	private static final int TITLE_COLOR = 0xFF52CCC9;
	private static final int ONLINE_COLOR = 0xFF5BD97A;
	private static final int OFFLINE_COLOR = 0xFF7A7A8A;
	private static final int DIM_COLOR = 0xFF9A9AAA;

	private TitleScreenFriendsPanel() {
	}

	/** Ana ekran acildiginda cagrilir: baglantiyi (salt-okunur) baslatir ve cizimi kaydeder. */
	public static void attach(Screen screen) {
		PresenceManager.startMenu();
		ScreenEvents.afterRender(screen).register(TitleScreenFriendsPanel::render);
	}

	private static void render(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		Minecraft client = Minecraft.getInstance();
		if (client.font == null) {
			return;
		}

		List<FriendRecord> friends = acceptedFriends();
		if (friends == null) {
			return;
		}

		int rows = Math.min(friends.size(), MAX_ROWS);
		int extra = friends.size() - rows;
		int contentLines = Math.max(rows, 1) + (extra > 0 ? 1 : 0);
		int height = PADDING * 2 + LINE_HEIGHT + 2 + contentLines * LINE_HEIGHT;

		int x = screen.width - WIDTH - MARGIN;
		int y = screen.height - height - MARGIN;
		SidebarButton.drawRoundedRect(guiGraphics, x, y, WIDTH, height, 4, BG, BORDER);

		int textX = x + PADDING;
		int textY = y + PADDING;
		guiGraphics.drawString(client.font, Component.literal("AURA - Arkadaslar"), textX, textY, TITLE_COLOR);
		textY += LINE_HEIGHT + 2;

		if (friends.isEmpty()) {
			guiGraphics.drawString(client.font, Component.literal("Henuz arkadas yok"), textX, textY, DIM_COLOR);
			return;
		}

		for (int i = 0; i < rows; i++) {
			FriendRecord friend = friends.get(i);
			String name = friend.username != null ? friend.username : friend.uuid.toString().substring(0, 8);
			String line = (friend.online ? "* " : "o ") + trim(client, name, friend);
			guiGraphics.drawString(client.font, Component.literal(line), textX, textY,
					friend.online ? ONLINE_COLOR : OFFLINE_COLOR);
			textY += LINE_HEIGHT;
		}

		if (extra > 0) {
			guiGraphics.drawString(client.font, Component.literal("+" + extra + " kisi daha"), textX, textY, DIM_COLOR);
		}
	}

	/** Ad + (varsa) sunucu adresi, panele sigacak sekilde kisaltilir. */
	private static String trim(Minecraft client, String name, FriendRecord friend) {
		String text = name;
		if (friend.online && friend.serverAddress != null && !friend.serverAddress.isBlank()) {
			text = name + " - " + friend.serverAddress;
		}
		int maxWidth = WIDTH - PADDING * 2 - client.font.width("* ");
		if (client.font.width(text) <= maxWidth) {
			return text;
		}
		while (text.length() > 1 && client.font.width(text + "...") > maxWidth) {
			text = text.substring(0, text.length() - 1);
		}
		return text + "...";
	}

	/** Oturum hazir degilse null (panel hic cizilmez), hazirsa kabul edilmis arkadaslar - cevrimiciler once. */
	private static List<FriendRecord> acceptedFriends() {
		if (!PresenceManager.isSessionActive()) {
			return null;
		}
		FriendsManager manager = PresenceManager.getFriendsManager();
		if (manager == null) {
			return null;
		}
		return manager.getFriends().stream()
				.filter(f -> f.state == FriendState.ACCEPTED)
				.sorted((a, b) -> {
					if (a.online != b.online) {
						return a.online ? -1 : 1;
					}
					String an = a.username != null ? a.username : a.uuid.toString();
					String bn = b.username != null ? b.username : b.uuid.toString();
					return an.compareToIgnoreCase(bn);
				})
				.toList();
	}
}
