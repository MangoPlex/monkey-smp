package dev.justmangoou.monkeysmp.graveyard.data;

import dev.justmangoou.monkeysmp.graveyard.util.DropRule;

import net.minecraft.world.item.ItemStack;

public class GraveItem {
	public ItemStack stack;
	public DropRule dropRule;
	public GraveItem(ItemStack stack, DropRule dropRule) {
		this.stack = stack;
		this.dropRule = dropRule;
	}
	public GraveItem copy() {
		return new GraveItem(this.stack.copy(), this.dropRule);
	}
}
