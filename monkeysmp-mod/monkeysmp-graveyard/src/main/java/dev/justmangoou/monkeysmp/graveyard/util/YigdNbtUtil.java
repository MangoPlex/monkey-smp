package dev.justmangoou.monkeysmp.graveyard.util;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

/**
 * A handful of NBT convenience methods (UUID, BlockPos, ItemStack, MobEffectInstance) were removed
 * in favor of using their Codec directly. This centralizes that so call sites read the same as before.
 */
public final class YigdNbtUtil {
	private YigdNbtUtil() {
	}

	public static void putUUID(CompoundTag nbt, String key, UUID value) {
		nbt.put(key, createUUIDTag(value));
	}

	public static @Nullable UUID getUUID(CompoundTag nbt, String key) {
		return loadUUIDTag(nbt.get(key));
	}

	public static Tag createUUIDTag(UUID value) {
		return UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE, value).getOrThrow();
	}

	public static @Nullable UUID loadUUIDTag(@Nullable Tag tag) {
		return tag == null ? null : UUIDUtil.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null);
	}

	public static Tag writeBlockPos(BlockPos pos) {
		return BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).getOrThrow();
	}

	public static Optional<BlockPos> readBlockPos(CompoundTag nbt, String key) {
		return nbt.get(key) == null ? Optional.empty() : BlockPos.CODEC.parse(NbtOps.INSTANCE, nbt.get(key)).result();
	}

	public static CompoundTag saveItemStack(ItemStack stack, HolderLookup.Provider registryLookup) {
		return (CompoundTag) ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registryLookup), stack).getOrThrow();
	}

	public static ItemStack parseOptionalItemStack(HolderLookup.Provider registryLookup, Tag tag) {
		return ItemStack.OPTIONAL_CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registryLookup), tag).result().orElse(ItemStack.EMPTY);
	}
}
