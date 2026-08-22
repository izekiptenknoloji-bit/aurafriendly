package com.izekip.izessentials.client.gui;

import com.izekip.izessentials.client.friends.FriendsManager;
import com.izekip.izessentials.client.friends.PresenceManager;
import com.izekip.izessentials.client.friends.firebase.FirebaseAuthClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** Izekip Aura hesap bilgilerim: email, bagli Minecraft hesabi, arkadas sayisi. */
public class AccountInfoScreen extends Screen {
	private final Screen parent;

	public AccountInfoScreen(Screen parent) {
		super(Component.literal("Izekip Aura - Hesap Bilgilerim"));
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

		FirebaseAuthClient auth = PresenceManager.getAuthForLogin();
		boolean loggedIn = auth != null && !PresenceManager.needsLogin();

		int y = top;
		if (!loggedIn) {
			addLine(left, y, contentWidth, "Aura Friendly hesabina giris yapilmamis.", AuraTheme.TEXT_MUTED);
			y += 18;
			addRenderableWidget(Button.builder(Component.literal("Giris Yap / Kayit Ol"), b -> this.minecraft.setScreen(new LoginScreen(parent)))
					.bounds(centerX - 75, y + 6, 150, 20).build());
		} else {
			String email = auth.getEmail();
			addLine(left, y, contentWidth, "Email: " + (email != null ? email : "?"), AuraTheme.TEXT_WHITE);
			y += 16;

			LocalPlayer player = this.minecraft.player;
			if (player != null) {
				addLine(left, y, contentWidth, "Minecraft kullanici adi: " + player.getGameProfile().name(), AuraTheme.TEXT_WHITE);
				y += 16;
				addLine(left, y, contentWidth, "Minecraft UUID: " + player.getUUID(), AuraTheme.TEXT_MUTED);
				y += 16;
			}

			FriendsManager manager = PresenceManager.getFriendsManager();
			int friendCount = manager != null ? manager.getFriends().size() : 0;
			addLine(left, y, contentWidth, "Arkadas sayisi: " + friendCount, AuraTheme.TEXT_WHITE);
			y += 26;

			addRenderableWidget(Button.builder(Component.literal("Cikis Yap"), b -> {
				PresenceManager.logout();
				this.minecraft.setScreen(new AccountInfoScreen(parent));
			}).bounds(centerX - 75, y, 150, 20).build());
		}

		addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> closeScreen())
				.bounds(centerX - 50, bottom - 20, 100, 20).build());
	}

	private void addLine(int x, int y, int width, String text, int color) {
		addRenderableWidget(new StringWidget(x, y, width, this.font.lineHeight, AuraTheme.colored(text, color), this.font));
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
