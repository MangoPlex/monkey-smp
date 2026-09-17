package dev.justmangoou.monkeysmp.graveyard.util;


import dev.justmangoou.monkeysmp.graveyard.data.GraveItem;

import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface GraveItemModificationConsumer {
	void accept(ItemStack stack, int slot, GraveItem graveItem);
}
