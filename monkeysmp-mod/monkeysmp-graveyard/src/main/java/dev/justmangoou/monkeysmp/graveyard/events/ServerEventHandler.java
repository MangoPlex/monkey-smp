package dev.justmangoou.monkeysmp.graveyard.events;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.justmangoou.monkeysmp.graveyard.components.GraveComponent;
import dev.justmangoou.monkeysmp.graveyard.components.GraveyardComponents;
import dev.justmangoou.monkeysmp.graveyard.components.InventoryComponent;
import dev.justmangoou.monkeysmp.graveyard.components.PlayerGraveData;
import dev.justmangoou.monkeysmp.graveyard.config.MonkeySMPGraveyardConfig;
import dev.justmangoou.monkeysmp.graveyard.data.DeathInfoManager;
import dev.justmangoou.monkeysmp.graveyard.data.GraveStatus;
import dev.justmangoou.monkeysmp.graveyard.data.ListMode;
import dev.justmangoou.monkeysmp.graveyard.networking.ServerPacketHandler;
import dev.justmangoou.monkeysmp.graveyard.MonkeySMPGraveyard;

import dev.justmangoou.monkeysmp.graveyard.util.DropRule;
import dev.justmangoou.monkeysmp.graveyard.util.GraveOverrideAreas;
import dev.justmangoou.monkeysmp.graveyard.util.MonkeySMPTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;

public class ServerEventHandler {
	public static void registerEvents() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> DeathInfoManager.get(server).bindWorlds(server));

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (alive) return;

			ResolvableProfile newProfile = ResolvableProfile.createResolved(newPlayer.getGameProfile());
			PlayerGraveData playerData = GraveyardComponents.PLAYER_DATA.get(newPlayer);
			playerData.getPendingRetainedInventory().ifPresent(inventory -> {
				NonNullList<ItemStack> extraItems = inventory.applyToPlayer(newPlayer);
				for (ItemStack stack : extraItems) {
					InventoryComponent.dropItemIfToBeDropped(stack, newPlayer.getX(), newPlayer.getY(), newPlayer.getZ(), newPlayer.level());
				}
				playerData.clearPendingRetainedInventory();
			});

			if (MonkeySMPGraveyardConfig.getConfig().graveConfig.informGraveLocation && playerData.consumeGraveGenerated()) {
				List<GraveComponent> graves = new ArrayList<>(DeathInfoManager.get(newPlayer.level()).getBackupData(newProfile));
				graves.removeIf(grave -> grave.getStatus() != GraveStatus.UNCLAIMED);
				if (!graves.isEmpty()) {
					GraveComponent latest = graves.getLast();
					BlockPos gravePos = latest.getPos();
					newPlayer.sendSystemMessage(Component.translatable("text.monkeysmp.message.grave_location",
							gravePos.getX(), gravePos.getY(), gravePos.getZ(),
							latest.getWorldRegistryKey().identifier().toString()));
				}
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			List<Runnable> tickFunctions = new ArrayList<>(MonkeySMPGraveyard.END_OF_TICK);
			MonkeySMPGraveyard.END_OF_TICK.clear();
			for (Runnable function : tickFunctions) {
				function.run();
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			MonkeySMPGraveyardConfig config = MonkeySMPGraveyardConfig.getConfig();
			if (!config.graveConfig.sellOutOfflinePeople) return;

			ResolvableProfile loggedOffProfile = ResolvableProfile.createResolved(handler.player.getGameProfile());
			List<GraveComponent> loggedOffGraves = DeathInfoManager.get(handler.player.level()).getBackupData(loggedOffProfile);
			List<GraveComponent> loggedOffUnclaimed = new ArrayList<>(loggedOffGraves);
			loggedOffGraves.removeIf(c -> c.getStatus() == GraveStatus.UNCLAIMED);
			if (!loggedOffUnclaimed.isEmpty()) {
				GraveComponent component = loggedOffUnclaimed.getFirst();
				BlockPos lastGravePos = component.getPos();
				server.sendSystemMessage(Component.translatable("text.monkeysmp.message.sellout_player",
						loggedOffProfile.name().orElse("PLAYER_NOT_FOUND"), lastGravePos.getX(), lastGravePos.getY(), lastGravePos.getZ(),
						component.getWorldRegistryKey().identifier().toString()));
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			MonkeySMPGraveyardConfig.GraveConfig.GraveRobbing robConfig = MonkeySMPGraveyardConfig.getConfig().graveConfig.graveRobbing;
			UUID joiningId = handler.player.getUUID();

			ServerPacketHandler.sendConfigSyncPacket(handler.player);

			if (!MonkeySMPGraveyard.NOT_NOTIFIED_ROBBERIES.containsKey(joiningId)) return;

			// Check if notifying when robbed is not required, since it has to be set to true for players to be added to NOT_NOTIFIED_ROBBERIES
			if (robConfig.tellWhoRobbed) {
				List<String> robbedBy = MonkeySMPGraveyard.NOT_NOTIFIED_ROBBERIES.remove(joiningId);
				for (String robber : robbedBy) {
					handler.player.sendSystemMessage(Component.translatable("text.monkeysmp.message.inform_robbery.with_details", robber));
				}
			} else {
				MonkeySMPGraveyard.NOT_NOTIFIED_ROBBERIES.remove(joiningId);
				handler.player.sendSystemMessage(Component.translatable("text.monkeysmp.message.inform_robbery"));
			}
		});
	}

	public static void registerEventCallbacks() {
		DropRuleEvent.EVENT.register((item, slot, context, modify) -> {
			MonkeySMPGraveyardConfig config = MonkeySMPGraveyardConfig.getConfig();

			if (config.inventoryConfig.vanishingSlots.contains(slot)) return DropRule.DESTROY;
			if (config.inventoryConfig.dropOnGroundSlots.contains(slot)) return DropRule.DROP;

			if (item.is(MonkeySMPTags.NATURAL_SOULBOUND)) return DropRule.KEEP;
			if (item.is(MonkeySMPTags.NATURAL_VANISHING)) return DropRule.DESTROY;
			if (item.is(MonkeySMPTags.GRAVE_INCOMPATIBLE)) return DropRule.DROP;

			if (!item.isEmpty() && item.has(DataComponents.CUSTOM_DATA)) {
				CustomData nbt = item.get(DataComponents.CUSTOM_DATA);
				assert nbt != null;  // This should never be null, but it's sorta required for intelliJ to not complain
				CompoundTag itemNbt = nbt.copyTag();
				if (itemNbt.contains("Botania_keepIvy") && itemNbt.getBooleanOr("Botania_keepIvy", false)) {
					if (modify) {
						CustomData replaced = nbt.update(nbtCompound -> nbtCompound.remove("Botania_keepIvy"));
						item.set(DataComponents.CUSTOM_DATA, replaced);
					}

					return DropRule.KEEP;
				}
			}

			DropRule dropRule;
			if (context != null)
				dropRule = GraveOverrideAreas.INSTANCE.getDropRuleFromArea(BlockPos.containing(context.deathPos()), context.world());
			else
				dropRule = GraveOverrideAreas.INSTANCE.defaultDropRule;


			// Get drop rule from enchantment
			if (EnchantmentHelper.hasTag(item, MonkeySMPTags.VANISHING))
				return DropRule.DESTROY;

			if (EnchantmentHelper.hasTag(item, MonkeySMPTags.SOULBOUND)) {
				return DropRule.KEEP;
			}

			return dropRule;
		});

		GraveClaimEvent.EVENT.register((player, world, pos, grave, tool) -> {
			if (player.isDeadOrDying()) return false;

			MonkeySMPGraveyardConfig config = MonkeySMPGraveyardConfig.getConfig();

			if (config.graveConfig.requireShovelToLoot && !tool.is(ItemTags.SHOVELS)) {
				player.sendSystemMessage(Component.translatable("text.monkeysmp.message.no_shovel"), true);
				return false;
			}

			if (player.getUUID().equals(grave.getOwner().partialProfile().id())) return true;
			if (!grave.isLocked()) return true;

			MonkeySMPGraveyardConfig.GraveConfig.GraveRobbing robConfig = config.graveConfig.graveRobbing;
			if (!robConfig.enabled) return false;

			if (robConfig.killerSkipWaitTime && player.getUUID().equals(grave.getKillerId())) {
				return true;
			}

			final int tps = 20;  // ticks per second
			if (!grave.hasExistedTicks(robConfig.timeUnit.toSeconds(robConfig.afterTime) * tps)) {
				player.sendSystemMessage(Component.translatable("text.monkeysmp.message.rob.too_early", grave.getTimeUntilRobbable()), true);
				return false;
			}

			return true;
		});

		AllowGraveGenerationEvent.EVENT.register((context, grave) -> {
			MonkeySMPGraveyardConfig.GraveConfig graveConfig = MonkeySMPGraveyardConfig.getConfig().graveConfig;
			DeathInfoManager deathInfo = DeathInfoManager.get(context.world());
			if (!graveConfig.enabled) return false;

			if ((deathInfo.getGraveListMode() == ListMode.WHITELIST
					&& !deathInfo.isInList(ResolvableProfile.createResolved(context.player().getGameProfile())))
					|| (deathInfo.getGraveListMode() == ListMode.BLACKLIST
					&& deathInfo.isInList(ResolvableProfile.createResolved(context.player().getGameProfile())))) {
				MonkeySMPGraveyard.LOGGER.info("{} found on whitelist/blacklist, disallowing grave generation", context.player().getGameProfile().name());
			}

			if (!graveConfig.generateEmptyGraves && grave.isGraveEmpty()) return false;

			if (graveConfig.dimensionBlacklist.contains(grave.getWorldRegistryKey().identifier().toString())) return false;

			if (!graveConfig.generateGraveInVoid && grave.getPos().getY() < context.world().getMinY()) return false;

			if (graveConfig.requireItem) {
				Item item = BuiltInRegistries.ITEM.get(Identifier.parse(graveConfig.requiredItem)).map(Holder.Reference::value).orElse(null);
				if (!grave.getInventoryComponent().removeItem(stack -> stack.is(item), graveConfig.requiredItemCount)) {
					return false;
				}
			}

			return !graveConfig.ignoredDeathTypes.contains(context.deathSource().getMsgId());
		});
		AllowBlockUnderGraveGenerationEvent.EVENT.register(
				(grave, currentUnder) -> MonkeySMPGraveyardConfig.getConfig().graveConfig.blockUnderGrave.enabled && currentUnder.is(MonkeySMPTags.REPLACE_SOFT_WHITELIST));

		GraveGenerationEvent.EVENT.register((world, pos, nthTry) -> {
			if (world.isOutsideBuildHeight(pos) || !world.getWorldBorder().isWithinBounds(pos)) {
				return false;
			}

			BlockState state = world.getBlockState(pos);
			MonkeySMPGraveyardConfig.GraveConfig config = MonkeySMPGraveyardConfig.getConfig().graveConfig;
			if (world.getBlockEntity(pos) != null)  // Block entities should NOT be replaced by graves
				return false;
			switch (nthTry) {
				case 0 -> {
					if (!config.useSoftBlockWhitelist) return false;
					if (!state.is(MonkeySMPTags.REPLACE_SOFT_WHITELIST)) return false;
				}
				case 1 -> {
					if (!config.useStrictBlockBlacklist) return false;
					if (state.is(MonkeySMPTags.KEEP_STRICT_BLACKLIST)) return false;
				}
			}
			return true;
		});
		DropItemEvent.EVENT.register((stack, x, y, z, world) -> !stack.isEmpty());
	}
}
