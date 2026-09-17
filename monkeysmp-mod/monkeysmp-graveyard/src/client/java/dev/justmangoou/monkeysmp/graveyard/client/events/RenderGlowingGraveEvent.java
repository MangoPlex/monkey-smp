package dev.justmangoou.monkeysmp.graveyard.client.events;

import dev.justmangoou.monkeysmp.graveyard.block.entity.GraveBlockEntity;

import net.minecraft.client.player.LocalPlayer;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface RenderGlowingGraveEvent {
	Event<RenderGlowingGraveEvent> EVENT = EventFactory.createArrayBacked(RenderGlowingGraveEvent.class, events -> (be, player) -> {
		boolean allow = false;
		for (RenderGlowingGraveEvent event : events) {
			allow = allow || event.canRenderOutline(be, player);
		}
		return allow;
	});

	boolean canRenderOutline(GraveBlockEntity be, LocalPlayer player);
}
