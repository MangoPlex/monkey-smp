package dev.justmangoou.monkeysmp.graveyard;

import java.util.UUID;

import dev.justmangoou.monkeysmp.graveyard.components.ExpComponent;
import dev.justmangoou.monkeysmp.graveyard.components.GraveComponent;
import dev.justmangoou.monkeysmp.graveyard.components.GraveyardComponents;
import dev.justmangoou.monkeysmp.graveyard.components.InventoryComponent;
import dev.justmangoou.monkeysmp.graveyard.config.MonkeySMPGraveyardConfig;
import dev.justmangoou.monkeysmp.graveyard.data.DeathContext;
import dev.justmangoou.monkeysmp.graveyard.events.DelayGraveGenerationEvent;
import dev.justmangoou.monkeysmp.graveyard.util.DropRule;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;

public class DeathHandler {
	public void onPlayerDeath(ServerPlayer player, ServerLevel world, Vec3 pos, DamageSource deathSource) {
		MonkeySMPGraveyardConfig config = MonkeySMPGraveyardConfig.getConfig();

		UUID killerId;
		if (deathSource.getEntity() instanceof ServerPlayer killer) {
			killerId = killer.getUUID();
		} else {
			killerId = null;
		}

		DeathContext context = new DeathContext(player, world, pos, deathSource);

		InventoryComponent inventoryComponent = new InventoryComponent(player);  // Will keep track of all items
		ExpComponent expComponent = new ExpComponent(player);  // Will keep track of XP

		InventoryComponent.clearPlayer(player);  // No use for actual inventory when inventory component is created
		ExpComponent.clearXp(player);  // No use for actual exp when exp component is created

		// Here would be an if statement for keepInventory, if the mod didn't let vanilla handle keepInventory
		// There once was code here, but is no more since removing it was the easiest fix a duplication bug

		// Handle drop rules
		inventoryComponent.onDeath(context);

		InventoryComponent retainedInventory = inventoryComponent.filteredInv(dropRule -> dropRule == DropRule.KEEP);
		InventoryComponent graveInventory = inventoryComponent.filteredInv(dropRule -> dropRule != DropRule.KEEP);
		GraveyardComponents.PLAYER_DATA.get(player).stageRetainedInventory(retainedInventory);

		ResolvableProfile profile = ResolvableProfile.createResolved(player.getGameProfile());
		Vec3 graveGenerationPos = !config.graveConfig.generateOnLastGroundPos ? pos
				: GraveyardComponents.PLAYER_DATA.get(player).getLastGroundPos(pos);
		GraveComponent graveComponent = new GraveComponent(profile, graveInventory, expComponent,
				world, graveGenerationPos.add(0D, .5D, 0D), deathSource.getLocalizedDeathMessage(player), killerId);  // Will keep track of player grave (if enabled)

		if (!graveComponent.isEmpty()) {
			graveComponent.backUp();
		} else {
			MonkeySMPGraveyard.LOGGER.info("No grave backup generated (empty grave)");  // There is no information worth backing up
		}

		Direction playerDirection = player.getDirection();

		if (!DelayGraveGenerationEvent.EVENT.invoker()
				.skipGenerationCall(graveComponent, playerDirection, context, "vanilla")) {
			graveComponent.generateOrDrop(playerDirection, context,
					() -> GraveyardComponents.PLAYER_DATA.get(player).markGraveGenerated());
		}
	}
}
