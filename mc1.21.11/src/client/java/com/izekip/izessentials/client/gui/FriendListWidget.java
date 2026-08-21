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
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Arkadas listesini gosteren, her satirda duruma gore aksiyon butonlari (Katil/Kabul et/Sil) olan liste. */
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
		private final Button joinButton;
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

			boolean canJoin = friend.state == FriendState.ACCEPTED && friend.online
					&& friend.serverAddress != null && ServerAddress.isValidAddress(friend.serverAddress);
			this.joinButton = canJoin
					? Button.builder(Component.literal("Katil"), b -> joinServer(friend.serverAddress)).size(50, 18).build()
					: null;

			List<AbstractWidget> list = new ArrayList<>();
			if (joinButton != null) {
				list.add(joinButton);
			}
			list.add(actionButton);
			this.widgets = list;
		}

		private static void joinServer(String address) {
			Minecraft minecraft = Minecraft.getInstance();
			ServerAddress serverAddress = ServerAddress.parseString(address);
			ServerData serverData = new ServerData("Aura Friendly", address, ServerData.Type.OTHER);
			TransferState emptyTransfer = new TransferState(Map.of(), Map.of(), false);
			ConnectScreen.startConnecting(minecraft.screen, minecraft, serverAddress, serverData, false, emptyTransfer);
		}

		@Override
		public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			int x = getContentX();
			int y = getContentY();
			int rowWidth = getContentWidth();

			String name = friend.username != null ? friend.username : friend.uuid.toString();
			guiGraphics.drawString(font, name, x, y, AuraTheme.TEXT_WHITE);
			guiGraphics.drawString(font, statusText(), x, y + 10, statusColor());

			int cursor = x + rowWidth;
			cursor -= actionButton.getWidth();
			actionButton.setX(cursor);
			actionButton.setY(y);
			actionButton.render(guiGraphics, mouseX, mouseY, partialTick);

			if (joinButton != null) {
				cursor -= joinButton.getWidth() + 4;
				joinButton.setX(cursor);
				joinButton.setY(y);
				joinButton.render(guiGraphics, mouseX, mouseY, partialTick);
			}
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
