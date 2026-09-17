package dev.justmangoou.monkeysmp.graveyard.client.events;

import dev.justmangoou.monkeysmp.graveyard.config.MonkeySMPGraveyardConfig;
import dev.justmangoou.monkeysmp.graveyard.config.MonkeySMPGraveyardConfig.ExtraFeatures.DeathSightConfig;
import dev.justmangoou.monkeysmp.graveyard.util.MonkeySMPTags;
import dev.justmangoou.monkeysmp.graveyard.client.render.GraveBlockEntityRenderer;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class ClientEventHandler {
	public static void registerEventCallbacks() {
		RenderGlowingGraveEvent.EVENT.register((be, player) -> {
			MonkeySMPGraveyardConfig config = MonkeySMPGraveyardConfig.getConfig();

			if (!config.graveRendering.useGlowingEffect || !GraveBlockEntityRenderer.syncedGlowing) return false;

			ResolvableProfile graveOwner = be.getGraveSkull();

			double distance = Integer.min(config.graveRendering.glowingDistance, GraveBlockEntityRenderer.syncedGlowingMaxDistance);
			boolean isOwner = graveOwner != null && graveOwner.partialProfile().equals(player.getGameProfile());
			DeathSightConfig deathSightConfig = config.extraFeatures.deathSightEnchant;

			ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
			if (!headStack.isEmpty() && EnchantmentHelper.hasTag(headStack, MonkeySMPTags.DEATH_SIGHT)) {
				distance = Double.min(deathSightConfig.range, GraveBlockEntityRenderer.syncedDeathSightDistance);

				// This doesn't actually mean that the user is the grave owner, but that the graves should light up
				isOwner = deathSightConfig.targets == DeathSightConfig.GraveTargets.ALL_GRAVES
						|| (graveOwner != null && deathSightConfig.targets == DeathSightConfig.GraveTargets.PLAYER_GRAVES);
				// If targets are OWN_GRAVES, the owner is already correct
			}

			boolean inRange = be.getBlockPos().closerToCenterThan(player.position(), distance);

			return isOwner && inRange;
		});
	}
}
