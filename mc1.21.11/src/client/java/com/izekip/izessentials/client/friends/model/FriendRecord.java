package com.izekip.izessentials.client.friends.model;

import java.util.UUID;

/** Bir arkadasin bellek-ici durumu. Alanlar birden fazla thread'den (SSE dinleyiciler, GUI render) okunup yazildigi icin volatile. */
public class FriendRecord {
	public final UUID uuid;
	public volatile String username;
	public volatile FriendState state;
	public volatile boolean online;
	public volatile long lastSeen;
	public volatile String serverAddress;

	public FriendRecord(UUID uuid, String username, FriendState state) {
		this.uuid = uuid;
		this.username = username;
		this.state = state;
	}
}
