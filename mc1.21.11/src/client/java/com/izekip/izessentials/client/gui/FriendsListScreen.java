package com.izekip.izessentials.client.gui;

import com.izekip.izessentials.client.friends.FriendsManager;
import com.izekip.izessentials.client.friends.PresenceManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

/** Arkadas ekleme + arkadas/online durumu listesi ekrani. */
public class FriendsListScreen extends Screen {
	private static final int REFRESH_INTERVAL_TICKS = 20;

	private final Screen parent;
	private EditBox usernameBox;
	private StringWidget statusWidget;
	private FriendListWidget list;
	private int ticksSinceRefresh;

	public FriendsListScreen(Screen parent) {
		super(Component.literal("Aura Friendly - Arkadaslar"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int left = AuraTheme.contentLeft(this.width);
		int right = AuraTheme.contentRight(this.width);
		int centerX = (left + right) / 2;
		int top = AuraTheme.CONTENT_TOP;
		int bottom = AuraTheme.contentBottom(this.height);

		FriendsManager manager = PresenceManager.getFriendsManager();

		if (PresenceManager.needsLogin()) {
			Component notLoggedIn = AuraTheme.colored("Aura Friendly hesabina giris yapmamissin.", AuraTheme.TEXT_MUTED);
			addRenderableWidget(new StringWidget(left, this.height / 2 - 20, right - left, this.font.lineHeight, notLoggedIn, this.font));
			addRenderableWidget(Button.builder(Component.literal("Giris Yap / Kayit Ol"), b -> this.minecraft.setScreen(new LoginScreen(parent)))
					.bounds(centerX - 75, this.height / 2, 150, 20).build());
			addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> closeScreen())
					.bounds(centerX - 50, bottom - 20, 100, 20).build());
			return;
		}

		if (!PresenceManager.isSessionActive() || manager == null) {
			String error = PresenceManager.getLastError();
			Component notConnected = error != null
					? AuraTheme.colored("Baglanti hatasi: " + error, AuraTheme.ERROR_RED)
					: AuraTheme.colored("Aura Friendly baglaniyor, biraz sonra tekrar ac...", AuraTheme.TEXT_MUTED);
			addRenderableWidget(new StringWidget(left, this.height / 2 - 5, right - left, this.font.lineHeight, notConnected, this.font));
			if (error != null) {
				addRenderableWidget(Button.builder(Component.literal("Tekrar Dene"), b -> PresenceManager.onLoginSuccess())
						.bounds(centerX - 50, bottom - 44, 100, 20).build());
			}
			addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> closeScreen())
					.bounds(centerX - 50, bottom - 20, 100, 20).build());
			return;
		}

		int addBoxWidth = Math.min(180, right - left - 65);
		this.usernameBox = new EditBox(this.font, left, top, addBoxWidth, 18, Component.literal("kullanici adi"));
		this.usernameBox.setHint(Component.literal("Kullanici adi"));
		addRenderableWidget(this.usernameBox);

		addRenderableWidget(Button.builder(Component.literal("Ekle"), b -> onAddFriend(manager))
				.bounds(left + addBoxWidth + 5, top, right - left - addBoxWidth - 5, 18).build());

		this.statusWidget = new StringWidget(left, top + 20, right - left, this.font.lineHeight, Component.literal(""), this.font);
		addRenderableWidget(this.statusWidget);

		int listTop = top + 34;
		int listBottom = bottom - 24;
		this.list = new FriendListWidget(this.minecraft, this.width, listBottom - listTop, listTop, this.font,
				new ArrayList<>(manager.getFriends()), manager);
		addRenderableWidget(this.list);

		addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> closeScreen())
				.bounds(centerX - 50, bottom - 20, 100, 20).build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		AuraTheme.renderPanel(guiGraphics, this.width, this.height, this.title, this.font);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void tick() {
		super.tick();
		if (list == null) {
			return;
		}
		ticksSinceRefresh++;
		if (ticksSinceRefresh >= REFRESH_INTERVAL_TICKS) {
			ticksSinceRefresh = 0;
			refreshList();
		}
	}

	private void refreshList() {
		FriendsManager manager = PresenceManager.getFriendsManager();
		if (manager == null) {
			return;
		}
		removeWidget(list);
		int top = AuraTheme.CONTENT_TOP;
		int bottom = AuraTheme.contentBottom(this.height);
		int listTop = top + 34;
		int listBottom = bottom - 24;
		this.list = new FriendListWidget(this.minecraft, this.width, listBottom - listTop, listTop, this.font,
				new ArrayList<>(manager.getFriends()), manager);
		addRenderableWidget(this.list);
	}

	private void onAddFriend(FriendsManager manager) {
		String username = usernameBox.getValue().trim();
		if (username.isEmpty()) {
			return;
		}
		statusWidget.setMessage(AuraTheme.colored("Araniyor: " + username + "...", AuraTheme.TEXT_MUTED));
		manager.addFriend(username, error -> this.minecraft.execute(() ->
				statusWidget.setMessage(AuraTheme.colored(error, AuraTheme.ERROR_RED))));
		usernameBox.setValue("");
	}

	private void closeScreen() {
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void onClose() {
		closeScreen();
	}
}
