package dev.justmangoou.monkeysmp.graveyard.block.entity;

import java.util.Optional;
import java.util.UUID;

import dev.justmangoou.monkeysmp.graveyard.components.GraveComponent;
import dev.justmangoou.monkeysmp.graveyard.config.MonkeySMPGraveyardConfig;
import dev.justmangoou.monkeysmp.graveyard.data.DeathInfoManager;
import dev.justmangoou.monkeysmp.graveyard.data.GraveStatus;
import dev.justmangoou.monkeysmp.graveyard.util.YigdTextUtil;
import dev.justmangoou.monkeysmp.graveyard.MonkeySMPGraveyard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class GraveBlockEntity extends BlockEntity {
	@Nullable
	private GraveComponent component = null;
	@Nullable
	private UUID graveId = null;
	@Nullable
	private ResolvableProfile graveSkull = null;
	@Nullable
	private Component graveText = null;
	@Nullable
	private BlockState previousState = null;

	private boolean claimed = true;

	private static MonkeySMPGraveyardConfig cachedConfig = MonkeySMPGraveyardConfig.getConfig();

	public GraveBlockEntity(BlockPos pos, BlockState state) {
		super(MonkeySMPGraveyard.GRAVE_BLOCK_ENTITY, pos, state);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder componentMapBuilder) {
		super.collectImplicitComponents(componentMapBuilder);
		componentMapBuilder.set(DataComponents.PROFILE, this.graveSkull);
		componentMapBuilder.set(DataComponents.CUSTOM_NAME, this.graveText);
		componentMapBuilder.set(GraveComponent.GRAVE_ID, this.graveId);
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		this.setGraveSkull(components.get(DataComponents.PROFILE));
		this.setGraveText(components.get(DataComponents.CUSTOM_NAME));
		this.graveId = components.get(GraveComponent.GRAVE_ID);
	}

	public void setComponent(GraveComponent component) {
		this.component = component;
		this.setClaimed(component.getStatus() == GraveStatus.CLAIMED);
		this.graveSkull = component.getOwner();
		this.graveId = component.getGraveId();
		this.graveSkull.name().ifPresent(name -> GraveBlockEntity.this.graveText = Component.nullToEmpty(name));
		this.setChanged();
	}
	public void setPreviousState(@Nullable BlockState previousState) {
		this.previousState = previousState;
	}
	public void setGraveText(@Nullable Component text) {
		this.graveText = text;
	}

	public @Nullable UUID getGraveId() {
		return this.graveId;
	}
	public @Nullable ResolvableProfile getGraveSkull() {
		return this.graveSkull;
	}
	public void setGraveSkull(@Nullable ResolvableProfile skull) {
		this.graveSkull = skull;
	}
	public @Nullable GraveComponent getComponent() {
		return this.component;
	}
	public @Nullable BlockState getPreviousState() {
		return this.previousState;
	}
	public boolean isUnclaimed() {
		return !this.claimed;
	}
	public void setClaimed(boolean claimed) {
		this.claimed = claimed;
	}
	public @Nullable Component getGraveText() {
		return this.graveText;
	}

	public void onBroken() {
		if (this.level == null || this.level.isClientSide()) return;

		MonkeySMPGraveyard.END_OF_TICK.add(() -> {
			Optional<GraveComponent> component = DeathInfoManager.get((ServerLevel) this.level).getGrave(this.graveId);
			component.ifPresent(grave -> {
				if (grave.getStatus() == GraveStatus.UNCLAIMED) {
					grave.onDestroyed();
				}
			});
		});
	}

	@Override
	public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
		CompoundTag nbt = this.saveWithoutMetadata(registryLookup);
		if (this.graveSkull != null) {
			ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, this.graveSkull).resultOrPartial()
					.ifPresent(nbtElement -> nbt.put("skull", nbtElement));
		}
		if (this.graveText != null)
			nbt.putString("text", YigdTextUtil.toJson(this.graveText, registryLookup));
		nbt.putBoolean("claimed", this.claimed);

		return nbt;
	}

	@Nullable
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		output.putBoolean("claimed", this.claimed);
		if (this.graveText != null)
			if (this.level != null) output.putString("text", YigdTextUtil.toJson(this.graveText, this.level.registryAccess()));
		if (this.graveSkull != null)
			output.store("skull", ResolvableProfile.CODEC, this.graveSkull);
		if (this.graveId != null)
			output.store("graveId", UUIDUtil.CODEC, this.graveId);
		if (this.previousState != null)
			output.store("previousState", BlockState.CODEC, this.previousState);
	}

	@Override
	public void loadAdditional(ValueInput input) {
		input.read("skull", ResolvableProfile.CODEC).ifPresent(this::setGraveSkull);

		input.getString("text").ifPresent(text -> this.graveText = YigdTextUtil.fromJson(text, input.lookup()));

		this.claimed = input.getBooleanOr("claimed", false);

		input.read("graveId", UUIDUtil.CODEC).ifPresent(id -> {
			this.graveId = id;
			if (this.component == null && this.level != null && !this.level.isClientSide()) {
				DeathInfoManager.get((ServerLevel) this.level).getGrave(this.graveId).ifPresent(this::setComponent);
			}
		});

		input.read("previousState", BlockState.CODEC).ifPresent(state -> this.previousState = state);
	}



	public static void tick(Level world, BlockPos pos, BlockState ignoredState, GraveBlockEntity be) {
		if (world.isClientSide()) return;

		if (be.component == null) {
			if (be.graveId == null) return;
			DeathInfoManager.get((ServerLevel) world).getGrave(be.graveId).ifPresent(be::setComponent);
			if (be.component == null) return;
		}
		if (world.getGameTime() % 2400 == 0) cachedConfig = MonkeySMPGraveyardConfig.getConfig();  // Reloads the config every 60 seconds

		MonkeySMPGraveyardConfig.GraveConfig.GraveTimeout timeoutConfig = cachedConfig.graveConfig.graveTimeout;

		if (!pos.equals(be.component.getPos())
				|| (!be.component.getWorldRegistryKey().equals(world.dimension()))) {
			be.updatePosition((ServerLevel) world, pos);
		}

		if (!timeoutConfig.enabled || be.component.getStatus() != GraveStatus.UNCLAIMED) return;

		long timePassed = world.getGameTime() - be.component.getCreationTime().getTime();
		final int ticksPerSecond = 20;
		if (timeoutConfig.timeUnit.toSeconds(timeoutConfig.afterTime) * ticksPerSecond <= timePassed) {
			// Not technically destroyed, but a status has to be set to not trigger the "onDestroyed" grave component method
			be.component.setStatus(GraveStatus.DESTROYED);


			BlockState newState = Blocks.AIR.defaultBlockState();
			BlockState previousState = be.getPreviousState();
			if (MonkeySMPGraveyardConfig.getConfig().graveConfig.replaceOldWhenClaimed && previousState != null) {
				newState = previousState;
			}
			be.component.replaceWithOld(newState, false);

			if (timeoutConfig.dropContentsOnTimeout) {
				be.component.dropAllGraveItems();
			}
		}
	}

	private void updatePosition(ServerLevel world, BlockPos pos) {
		if (this.component == null) return;

		this.component.setPos(pos);
		this.component.setWorld(world);
		if (this.component.getStatus() == GraveStatus.DESTROYED || !this.claimed) {
			this.component.setStatus(GraveStatus.UNCLAIMED);
			PlayerList playerManager = world.getServer().getPlayerList();
			ResolvableProfile owner = this.component.getOwner();
			ServerPlayer player = Optional.of(owner.partialProfile().id()).isPresent() ? playerManager.getPlayer(Optional.of(owner.partialProfile().id()).get()) : playerManager.getPlayerByName(owner.name().orElse(null));
			if (player != null) {
				player.sendSystemMessage(Component.translatable("text.monkeysmp.message.grave_relocated", pos.getX(), pos.getY(), pos.getZ(), world.dimension().identifier().toString()));
			}
		}
	}
}
