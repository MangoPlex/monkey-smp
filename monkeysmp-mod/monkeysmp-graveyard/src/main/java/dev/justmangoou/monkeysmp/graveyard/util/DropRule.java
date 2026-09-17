package dev.justmangoou.monkeysmp.graveyard.util;

public enum DropRule {
	DROP,  // Drop items on ground, even if a grave is generated
	KEEP,  // Keep items on the player through death
	DESTROY,  // Destroy the items, and they can never be reclaimed ever again
	PUT_IN_GRAVE  // Try to put items in grave. If graves are not active, or grave failed to generate, items will drop on ground
}
