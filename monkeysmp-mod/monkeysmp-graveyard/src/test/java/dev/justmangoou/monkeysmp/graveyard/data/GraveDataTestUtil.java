package dev.justmangoou.monkeysmp.graveyard.data;

import dev.justmangoou.monkeysmp.graveyard.components.GraveComponent;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.component.ResolvableProfile;

final class GraveDataTestUtil {
	private GraveDataTestUtil() {
	}

	static CompoundTag managerTag(ResolvableProfile owner, GraveComponent grave, HolderLookup.Provider lookup) {
		CompoundTag playerGraves = new CompoundTag();
		playerGraves.put("user", ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, owner).getOrThrow());
		ListTag graves = new ListTag();
		graves.add(grave.toNbt(lookup));
		playerGraves.put("graves", graves);

		ListTag players = new ListTag();
		players.add(playerGraves);
		CompoundTag manager = new CompoundTag();
		manager.put("graves", players);
		return manager;
	}
}
