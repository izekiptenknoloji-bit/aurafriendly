package com.izekip.izessentials.client.friends.model;

public enum FriendState {
	PENDING_OUTGOING,
	PENDING_INCOMING,
	ACCEPTED;

	public static FriendState fromJson(String value) {
		if (value == null) {
			return null;
		}
		switch (value) {
			case "pending_outgoing": return PENDING_OUTGOING;
			case "pending_incoming": return PENDING_INCOMING;
			case "accepted": return ACCEPTED;
			default: return null;
		}
	}

	public String toJson() {
		switch (this) {
			case PENDING_OUTGOING: return "pending_outgoing";
			case PENDING_INCOMING: return "pending_incoming";
			case ACCEPTED: return "accepted";
		}
		throw new IllegalStateException("Bilinmeyen FriendState: " + this);
	}
}
