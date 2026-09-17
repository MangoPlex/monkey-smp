package dev.justmangoou.monkeysmp.graveyard.components;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

class InventoryComponentTest {
	private static final HolderLookup.Provider EMPTY_LOOKUP = HolderLookup.Provider.create(Stream.empty());

	@Test
	void normalizesLegacyEquipmentLayout() {
		CompoundTag vanillaInventory = new CompoundTag();
		vanillaInventory.putInt("size", 43);
		vanillaInventory.putInt("mainSize", 40);
		vanillaInventory.putInt("armorSize", 4);
		vanillaInventory.putInt("offHandSize", 1);

		CompoundTag inventoryTag = new CompoundTag();
		inventoryTag.put("vanilla", vanillaInventory);
		inventoryTag.put("mods", new CompoundTag());

		InventoryComponent inventory = InventoryComponent.fromNbt(inventoryTag, EMPTY_LOOKUP);

		assertEquals(36, inventory.mainSize);
		assertEquals(4, inventory.armorSize);
		assertEquals(1, inventory.offHandSize);
	}
}
