package dev.justmangoou.monkeysmp.graveyard.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import dev.justmangoou.monkeysmp.graveyard.block.entity.GraveBlockEntity;
import dev.justmangoou.monkeysmp.graveyard.components.GraveComponent;
import dev.justmangoou.monkeysmp.graveyard.components.GraveyardComponents;
import dev.justmangoou.monkeysmp.graveyard.config.MonkeySMPGraveyardConfig;
import com.mojang.authlib.GameProfile;
import dev.justmangoou.monkeysmp.graveyard.MonkeySMPGraveyard;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v8.component.CardinalComponent;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class DeathInfoManager implements CardinalComponent {
	private final Level owner;
	private final Map<GameProfile, List<GraveComponent>> graveBackups = new HashMap<>();
	private final Map<UUID, GraveComponent> graveMap = new HashMap<>();
	private ListMode graveListMode = ListMode.BLACKLIST;
	private final Set<GameProfile> affectedPlayers = new HashSet<>();

	public DeathInfoManager(Level owner) {
		this.owner = owner;
	}

	public static DeathInfoManager get(MinecraftServer server) {
		return GraveyardComponents.GRAVEYARD_DATA.get(server.overworld());
	}

	public static DeathInfoManager get(ServerLevel level) {
		return get(level.getServer());
	}

	public void clear() {
		this.graveBackups.clear();
		this.graveMap.clear();
		this.affectedPlayers.clear();
		this.graveListMode = ListMode.BLACKLIST;
	}

	public Set<GameProfile> getAffectedPlayers() {
		return this.affectedPlayers;
	}

	/**
	 * Tries to delete a grave based on its grave ID.
	 *
	 * @return FAIL if nothing was deleted, PASS if only part of it was deleted, or SUCCESS if it was fully deleted
	 */
	public InteractionResult delete(UUID graveId) {
		GraveComponent component = this.graveMap.get(graveId);
		if (component == null) return InteractionResult.FAIL;

		GameProfile profile = component.getOwner().partialProfile();
		this.graveMap.remove(graveId);

		if (!this.graveBackups.containsKey(profile)) return InteractionResult.PASS;
		this.graveBackups.get(profile).remove(component);

		if (component.getStatus() != GraveStatus.UNCLAIMED) return InteractionResult.SUCCESS;
		return component.removeGraveBlock() ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	public Map<GameProfile, List<GraveComponent>> getPlayerGraves() {
		return this.graveBackups;
	}

	public void addBackup(ResolvableProfile profile, GraveComponent component) {
		List<GraveComponent> playerGraves = this.graveBackups.computeIfAbsent(profile.partialProfile(), key -> new ArrayList<>());
		playerGraves.add(component);
		this.graveMap.put(component.getGraveId(), component);

		MonkeySMPGraveyardConfig config = MonkeySMPGraveyardConfig.getConfig();
		if (playerGraves.size() > config.graveConfig.maxBackupsPerPerson) {
			GraveComponent toBeRemoved = playerGraves.getFirst();
			this.delete(toBeRemoved.getGraveId());
			if (toBeRemoved.getStatus() == GraveStatus.UNCLAIMED && config.graveConfig.dropFromOldestWhenDeleted) {
				toBeRemoved.dropAllGraveItems();
			}
		}
	}

	public @NotNull List<GraveComponent> getBackupData(ResolvableProfile profile) {
		return this.graveBackups.computeIfAbsent(profile.partialProfile(), key -> new ArrayList<>());
	}

	public Optional<GraveComponent> getGrave(UUID graveId) {
		return Optional.ofNullable(this.graveMap.get(graveId));
	}

	public ListMode getGraveListMode() {
		return this.graveListMode;
	}

	public void setGraveListMode(ListMode listMode) {
		this.graveListMode = listMode;
	}

	public void addToList(ResolvableProfile profile) {
		this.affectedPlayers.add(profile.partialProfile());
	}

	public boolean removeFromList(ResolvableProfile profile) {
		return this.affectedPlayers.remove(profile.partialProfile());
	}

	public boolean isInList(ResolvableProfile profile) {
		return this.affectedPlayers.contains(profile.partialProfile());
	}

	public void bindWorlds(MinecraftServer server) {
		for (GraveComponent component : this.graveMap.values()) {
			ServerLevel world = server.getLevel(component.getWorldRegistryKey());
			if (world == null) continue;

			component.setWorld(world);
			if (world.areEntitiesLoaded(ChunkPos.containing(component.getPos()).pack())
					&& world.getBlockEntity(component.getPos()) instanceof GraveBlockEntity blockEntity
					&& component.getGraveId().equals(blockEntity.getGraveId())) {
				blockEntity.setComponent(component);
			}
		}
	}

	@Override
	public void readData(ValueInput input) {
		this.clear();
		input.read("data", CompoundTag.CODEC).ifPresent(tag -> this.load(tag, input.lookup()));
	}

	@Override
	public void writeData(ValueOutput output) {
		output.store("data", CompoundTag.CODEC, this.save(new CompoundTag(), this.owner.registryAccess()));
	}

	CompoundTag save(CompoundTag nbt, HolderLookup.Provider registryLookup) {
		ListTag graveNbt = new ListTag();
		for (Map.Entry<GameProfile, List<GraveComponent>> entry : this.graveBackups.entrySet()) {
			CompoundTag graveCompound = new CompoundTag();
			ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, ResolvableProfile.createResolved(entry.getKey())).result()
					.ifPresent(profileTag -> graveCompound.put("user", profileTag));

			ListTag graveNbtList = new ListTag();
			for (GraveComponent graveComponent : entry.getValue()) {
				graveNbtList.add(graveComponent.toNbt(registryLookup));
			}
			graveCompound.put("graves", graveNbtList);
			graveNbt.add(graveCompound);
		}

		CompoundTag graveListNbt = new CompoundTag();
		graveListNbt.putString("listMode", this.graveListMode.name());
		ListTag affectedPlayersNbt = new ListTag();
		for (GameProfile profile : this.affectedPlayers) {
			ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, ResolvableProfile.createResolved(profile)).result()
					.ifPresent(affectedPlayersNbt::add);
		}
		graveListNbt.put("affectedPlayers", affectedPlayersNbt);

		nbt.put("graves", graveNbt);
		nbt.put("whitelist", graveListNbt);
		return nbt;
	}

	void load(CompoundTag nbt, HolderLookup.Provider registryLookup) {
		for (Tag graveElement : nbt.getListOrEmpty("graves")) {
			if (!(graveElement instanceof CompoundTag graveCompound)) continue;
			try {
				Tag userTag = graveCompound.get("user");
				if (userTag == null) continue;
				ResolvableProfile user = ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, userTag).getOrThrow();
				for (Tag graveTag : graveCompound.getListOrEmpty("graves")) {
					if (!(graveTag instanceof CompoundTag compound)) continue;
					GraveComponent component = GraveComponent.fromNbt(compound, registryLookup, null);
					if (component == null) continue;
					this.graveBackups.computeIfAbsent(user.partialProfile(), key -> new ArrayList<>()).add(component);
					this.graveMap.put(component.getGraveId(), component);
				}
			} catch (RuntimeException exception) {
				MonkeySMPGraveyard.LOGGER.error("Skipping invalid grave backup while loading CCA data", exception);
			}
		}

		CompoundTag graveListNbt = nbt.getCompoundOrEmpty("whitelist");
		String listMode = graveListNbt.getStringOr("listMode", ListMode.BLACKLIST.name());
		try {
			this.graveListMode = ListMode.valueOf(listMode);
		} catch (IllegalArgumentException exception) {
			MonkeySMPGraveyard.LOGGER.warn("Unknown grave list mode '{}'; using BLACKLIST", listMode);
			this.graveListMode = ListMode.BLACKLIST;
		}

		for (Tag profileTag : graveListNbt.getListOrEmpty("affectedPlayers")) {
			ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, profileTag).resultOrPartial(
					error -> MonkeySMPGraveyard.LOGGER.error("Skipping invalid player in the grave list: {}", error))
					.ifPresent(profile -> this.affectedPlayers.add(profile.partialProfile()));
		}
	}
}
