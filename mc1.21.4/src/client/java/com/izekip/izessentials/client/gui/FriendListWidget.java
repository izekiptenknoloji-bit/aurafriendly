package com.izekip.izessentials.client.gui;

import com.izekip.izessentials.client.friends.FriendsManager;
import com.izekip.izessentials.client.friends.model.FriendRecord;
import com.izekip.izessentials.client.friends.model.FriendState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Arkadas listesini gosteren, her satirda duruma gore bir aksiyon butonu (Kabul et/Sil) olan liste (1.21.4 varyanti).
 * NOT: 1.21.4'te AbstractSelectionList.Entry'nin override noktasi "render(...)" (9 parametreli) -
 * bkz. ShortcutListWidget'taki ayni not.
 */
public class FriendListWidget extends ContainerObjectSelectionList<FriendListWidget.Entry> {
	public static final int ROW_HEIGHT = 22;

	public FriendListWidget(Minecraft minecraft, int width, int height, int y0, Font font, List<FriendRecord> friends, FriendsManager manager) {
		super(minecraft, width, height, y0, ROW_HEIGHT);
		for (FriendRecord friend : friends) {
			addEntry(new Entry(font, friend, manager));
		}
	}

	@Override
	public int getRowWidth() {
		return Math.min(360, getWidth() - 12);
	}

	public static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
		private final Font font;
		private final FriendRecord friend;
		private final Button actionButton;
		private final List<AbstractWidget> widgets;

		Entry(Font font, FriendRecord friend, FriendsManager manager) {
			this.font = font;
			this.friend = friend;

			if (friend.state == FriendState.PENDING_INCOMING) {
				this.actionButton = Button.builder(Component.literal("Kabul et"), b -> manager.acceptFriend(friend.uuid))
						.size(70, 18).build();
			} else {
				this.actionButton = Button.builder(Component.literal("Sil"), b -> manager.removeFriend(friend.uuid))
						.size(70, 18).build();
			}
			this.widgets = List.of(actionButton);
		}

		@Override
		public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
				int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
			String name = friend.username != null ? friend.username : friend.uuid.toString();
			guiGraphics.drawString(font, name, left, top, AuraTheme.TEXT_WHITE);
			guiGraphics.drawString(font, statusText(), left, top + 10, statusColor());

			actionButton.setX(left + width - actionButton.getWidth());
			actionButton.setY(top);
			actionButton.render(guiGraphics, mouseX, mouseY, partialTick);
		}

		private String statusText() {
			switch (friend.state) {
				case PENDING_OUTGOING: return "Istek gonderildi";
				case PENDING_INCOMING: return "Istek bekliyor";
				case ACCEPTED: return friend.online ? "Cevrimici" : "Cevrimdisi";
				default: return "";
			}
		}

		private int statusColor() {
			if (friend.state == FriendState.ACCEPTED) {
				return friend.online ? AuraTheme.ONLINE_GREEN : AuraTheme.OFFLINE_GRAY;
			}
			return AuraTheme.TEXT_MUTED;
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return widgets;
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return widgets;
		}

		@Override
		public boolean isDragging() {
			return false;
		}

		@Override
		public void setDragging(boolean dragging) {
		}
	}
}
