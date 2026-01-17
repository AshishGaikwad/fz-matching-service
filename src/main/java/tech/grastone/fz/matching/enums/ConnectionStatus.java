package tech.grastone.fz.matching.enums;

public enum ConnectionStatus {
	ACCEPTED, // Both users are now connected (e.g., friends)
	REMOVED, // One user disconnected or unfriended the other
	BLOCKED // (Optional) A user has blocked the other
}
