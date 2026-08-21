package com.izekip.izessentials.client.gui;

import com.izekip.izessentials.client.friends.PresenceManager;
import com.izekip.izessentials.client.friends.firebase.FirebaseAuthClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/** Aura Friendly hesabina email+sifre ile giris/kayit ekrani. */
public class LoginScreen extends Screen {
	private final Screen parent;
	private EditBox emailBox;
	private EditBox passwordBox;
	private StringWidget statusWidget;
	private volatile boolean busy;

	public LoginScreen(Screen parent) {
		super(Component.literal("Aura Friendly - Giris"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int left = AuraTheme.contentLeft(this.width);
		int right = AuraTheme.contentRight(this.width);
		int centerX = (left + right) / 2;
		int fieldWidth = Math.min(200, right - left);
		int fieldX = centerX - fieldWidth / 2;
		int top = AuraTheme.CONTENT_TOP;

		if (PresenceManager.getAuthForLogin() == null) {
			Component notReady = AuraTheme.colored("Aura Friendly henuz hazir degil, once bir sunucuya/dunyaya gir.", AuraTheme.TEXT_MUTED);
			addRenderableWidget(new StringWidget(left, this.height / 2 - 5, right - left, this.font.lineHeight, notReady, this.font));
			addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> closeScreen())
					.bounds(centerX - 50, AuraTheme.contentBottom(this.height) - 20, 100, 20).build());
			return;
		}

		this.emailBox = new EditBox(this.font, fieldX, top, fieldWidth, 18, Component.literal("email"));
		this.emailBox.setHint(Component.literal("Email"));
		addRenderableWidget(this.emailBox);

		this.passwordBox = new EditBox(this.font, fieldX, top + 24, fieldWidth, 18, Component.literal("sifre"));
		this.passwordBox.setHint(Component.literal("Sifre"));
		this.passwordBox.addFormatter((text, pos) -> FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY));
		addRenderableWidget(this.passwordBox);

		this.statusWidget = new StringWidget(fieldX, top + 46, fieldWidth, this.font.lineHeight, Component.literal(""), this.font);
		addRenderableWidget(this.statusWidget);

		addRenderableWidget(Button.builder(Component.literal("Giris Yap"), b -> attempt(false))
				.bounds(fieldX, top + 66, (fieldWidth - 5) / 2, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Kayit Ol"), b -> attempt(true))
				.bounds(fieldX + (fieldWidth + 5) / 2, top + 66, (fieldWidth - 5) / 2, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Iptal"), b -> closeScreen())
				.bounds(fieldX, top + 92, fieldWidth, 20).build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		AuraTheme.renderPanel(guiGraphics, this.width, this.height, this.title, this.font);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	private void attempt(boolean signUp) {
		if (busy) {
			return;
		}
		String email = emailBox.getValue().trim();
		String password = passwordBox.getValue();
		if (email.isEmpty() || password.isEmpty()) {
			statusWidget.setMessage(AuraTheme.colored("Email ve sifre gerekli.", AuraTheme.ERROR_RED));
			return;
		}
		FirebaseAuthClient auth = PresenceManager.getAuthForLogin();
		if (auth == null) {
			statusWidget.setMessage(AuraTheme.colored("Aura Friendly henuz hazir degil.", AuraTheme.ERROR_RED));
			return;
		}

		busy = true;
		statusWidget.setMessage(AuraTheme.colored(signUp ? "Kayit olunuyor..." : "Giris yapiliyor...", AuraTheme.TEXT_MUTED));
		Thread.ofVirtual().name("aurafriendly-login").start(() -> {
			try {
				if (signUp) {
					auth.signUp(email, password);
				} else {
					auth.signIn(email, password);
				}
				PresenceManager.onLoginSuccess();
				this.minecraft.execute(() -> this.minecraft.setScreen(new FriendsListScreen(parent)));
			} catch (Exception e) {
				String message = e.getMessage() != null ? e.getMessage() : "Basarisiz, tekrar dene.";
				this.minecraft.execute(() -> {
					statusWidget.setMessage(AuraTheme.colored(message, AuraTheme.ERROR_RED));
					busy = false;
				});
			}
		});
	}

	private void closeScreen() {
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void onClose() {
		closeScreen();
	}
}
