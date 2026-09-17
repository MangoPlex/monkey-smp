package dev.justmangoou.monkeysmp.graveyard.events;

import dev.justmangoou.monkeysmp.graveyard.components.GraveComponent;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface GraveClaimEvent {
	Event<GraveClaimEvent> EVENT = EventFactory.createArrayBacked(GraveClaimEvent.class, graveClaimEvents -> (player, world, pos, grave, tool) -> {
		boolean allow = false;
		for (GraveClaimEvent claimEvent : graveClaimEvents) {
			allow = allow || claimEvent.canClaim(player, world, pos, grave, tool);
		}
		return allow;
	});

	boolean canClaim(ServerPlayer player, ServerLevel world, BlockPos pos, GraveComponent grave, ItemStack tool);
}
