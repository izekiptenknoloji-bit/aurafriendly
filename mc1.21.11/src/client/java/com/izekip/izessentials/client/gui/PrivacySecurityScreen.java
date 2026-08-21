package com.izekip.izessentials.client.gui;

import com.izekip.izessentials.client.friends.PresenceManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Hesap durumu, kisa gizlilik bilgisi ve cikis yapma. */
public class PrivacySecurityScreen extends Screen {
	private final Screen parent;

	public PrivacySecurityScreen(Screen parent) {
		super(Component.literal("Aura Friendly - Gizlilik ve Guvenlik"));
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

		boolean loggedIn = PresenceManager.getAuthForLogin() != null && !PresenceManager.needsLogin();
		Component status = AuraTheme.colored(
				loggedIn ? "Aura Friendly hesabina giris yapilmis." : "Giris yapilmamis.",
				loggedIn ? AuraTheme.ONLINE_GREEN : AuraTheme.TEXT_MUTED);
		addRenderableWidget(new StringWidget(left, top, contentWidth, this.font.lineHeight, status, this.font));

		String info = "Sifren hicbir zaman kaydedilmez, sadece giris aninda kullanilir. "
				+ "Sunucuya girince kullanici adin ve online durumun Firebase'e yazilir, "
				+ "sadece kabul ettigin arkadaslar bunu gorebilir. Kaynak kodu acik: "
				+ "github.com/izekiptenknoloji-bit/aurafriendly";
		MultiLineTextWidget infoWidget = new MultiLineTextWidget(AuraTheme.colored(info, AuraTheme.TEXT_MUTED), this.font)
				.setMaxWidth(contentWidth)
				.setMaxRows(6);
		infoWidget.setX(left);
		infoWidget.setY(top + 18);
		addRenderableWidget(infoWidget);

		if (loggedIn) {
			addRenderableWidget(Button.builder(Component.literal("Cikis Yap"), b -> {
				PresenceManager.logout();
				closeScreen();
			}).bounds(centerX - 75, top + 90, 150, 20).build());
		}

		addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> closeScreen())
				.bounds(centerX - 50, bottom - 20, 100, 20).build());
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
