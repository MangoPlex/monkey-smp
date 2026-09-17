package dev.justmangoou.monkeysmp.graveyard.events;

import dev.justmangoou.monkeysmp.graveyard.components.GraveComponent;

import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface AllowBlockUnderGraveGenerationEvent {
	Event<AllowBlockUnderGraveGenerationEvent> EVENT = EventFactory.createArrayBacked(AllowBlockUnderGraveGenerationEvent.class, events -> (grave, currentUnder) -> {
		boolean allow = true;
		for (AllowBlockUnderGraveGenerationEvent event : events) {
			allow = allow && event.allowBlockGeneration(grave, currentUnder);
		}

		return allow;
	});

	boolean allowBlockGeneration(GraveComponent grave, BlockState currentUnder);
}
