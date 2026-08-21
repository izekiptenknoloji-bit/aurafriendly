package com.izekip.izessentials.client.macro;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

/**
 * Bir macro metnini, oyuncu chat'e elle yazmis gibi gonderir.
 * "/" ile baslayan metinler komut olarak, digerleri duz chat mesaji olarak gider.
 */
public final class ChatSender {
	private ChatSender() {
	}

	public static void send(String rawText) {
		if (rawText == null) {
			return;
		}
		String text = rawText.trim();
		if (text.isEmpty()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		ClientPacketListener connection = player.connection;
		if (connection == null) {
			return;
		}

		if (text.startsWith("/")) {
			connection.sendCommand(text.substring(1));
		} else {
			connection.sendChat(text);
		}
	}
}
