package dev.justmangoou.monkeysmp.graveyard.components;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;
import org.ladysnake.cca.api.v3.entity.RespawnableComponent;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class PlayerGraveData implements RespawnableComponent<PlayerGraveData>, ServerTickingComponent {
	private final Player owner;
	private @Nullable InventoryComponent pendingRetainedInventory;
	private boolean graveGenerated;
	private @Nullable Vec3 lastGroundPos;
	private @Nullable ResourceKey<Level> lastGroundDimension;

	public PlayerGraveData(Player owner) {
		this.owner = owner;
	}

	@Override
	public void serverTick() {
		if (!(this.owner instanceof ServerPlayer player)) return;

		ResourceKey<Level> currentDimension = player.level().dimension();
		if (!currentDimension.equals(this.lastGroundDimension)) {
			this.lastGroundDimension = currentDimension;
			this.lastGroundPos = player.position();
			return;
		}
		if (player.onGround()) {
			this.lastGroundPos = player.position();
		}
	}

	public Vec3 getLastGroundPos(Vec3 fallback) {
		if (this.lastGroundPos == null || !this.owner.level().dimension().equals(this.lastGroundDimension)) {
			return fallback;
		}
		return this.lastGroundPos;
	}

	public void stageRetainedInventory(InventoryComponent inventory) {
		if (this.pendingRetainedInventory == null && !inventory.isEmpty()) {
			this.pendingRetainedInventory = inventory;
		}
	}

	public Optional<InventoryComponent> getPendingRetainedInventory() {
		return Optional.ofNullable(this.pendingRetainedInventory);
	}

	public void clearPendingRetainedInventory() {
		this.pendingRetainedInventory = null;
	}

	public void markGraveGenerated() {
		this.graveGenerated = true;
	}

	public boolean consumeGraveGenerated() {
		boolean generated = this.graveGenerated;
		this.graveGenerated = false;
		return generated;
	}

	@Override
	public boolean shouldCopyForRespawn(boolean lossless, boolean keepInventory, boolean sameCharacter) {
		return sameCharacter;
	}

	@Override
	public void copyForRespawn(PlayerGraveData original, HolderLookup.Provider registryLookup, boolean lossless,
			boolean keepInventory, boolean sameCharacter) {
		if (original.pendingRetainedInventory != null) {
			this.pendingRetainedInventory = InventoryComponent.fromNbt(
					original.pendingRetainedInventory.toNbt(registryLookup), registryLookup);
		}
		this.graveGenerated = original.graveGenerated;
		if (lossless) {
			this.lastGroundPos = original.lastGroundPos;
			this.lastGroundDimension = original.lastGroundDimension;
		}
	}

	@Override
	public void readData(ValueInput input) {
		this.pendingRetainedInventory = input.read("retainedInventory", CompoundTag.CODEC)
				.map(tag -> InventoryComponent.fromNbt(tag, input.lookup()))
				.orElse(null);
		this.graveGenerated = input.getBooleanOr("graveGenerated", false);
		this.lastGroundPos = null;
		this.lastGroundDimension = null;
		input.read("lastGround", CompoundTag.CODEC).ifPresent(tag -> {
			try {
				this.lastGroundDimension = ResourceKey.create(Registries.DIMENSION,
						Identifier.parse(tag.getStringOr("dimension", "")));
				this.lastGroundPos = new Vec3(
						tag.getDoubleOr("x", 0D),
						tag.getDoubleOr("y", 0D),
						tag.getDoubleOr("z", 0D));
			} catch (RuntimeException ignored) {
				this.lastGroundDimension = null;
				this.lastGroundPos = null;
			}
		});
	}

	@Override
	public void writeData(ValueOutput output) {
		if (this.pendingRetainedInventory != null) {
			output.store("retainedInventory", CompoundTag.CODEC,
					this.pendingRetainedInventory.toNbt(this.owner.registryAccess()));
		}
		if (this.graveGenerated) output.putBoolean("graveGenerated", true);
		if (this.lastGroundPos != null && this.lastGroundDimension != null) {
			CompoundTag tag = new CompoundTag();
			tag.putString("dimension", this.lastGroundDimension.identifier().toString());
			tag.putDouble("x", this.lastGroundPos.x());
			tag.putDouble("y", this.lastGroundPos.y());
			tag.putDouble("z", this.lastGroundPos.z());
			output.store("lastGround", CompoundTag.CODEC, tag);
		}
	}
}
