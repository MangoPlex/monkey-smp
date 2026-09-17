package dev.justmangoou.monkeysmp.graveyard.events;

import dev.justmangoou.monkeysmp.graveyard.components.GraveComponent;
import dev.justmangoou.monkeysmp.graveyard.data.DeathContext;

import net.minecraft.core.Direction;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface DelayGraveGenerationEvent {
	Event<DelayGraveGenerationEvent> EVENT = EventFactory.createArrayBacked(DelayGraveGenerationEvent.class,
			events -> (grave, direction, context, caller) -> {
		for (DelayGraveGenerationEvent event : events) {
			// We want to stop at first match, so not multiple callers will get triggered,
			// if multiple independent ones are set up to only trigger when this event fires
			if (event.skipGenerationCall(grave, direction, context, caller))
				return true;
		}
		return false;
	});

	/**
	 * Weather or not grave generation calls are skipped or not. Can be used to delay grave generation to later (E.g. at respawn)
	 * @param grave Grave-component that would generate the grave
	 * @param direction Player facing direction
	 * @param context Death context for when player dies
	 * @param caller Unique string identifier to identify the caller. Mod ID recommended to use here
	 * @return Weather or not this iteration should be skipped
	 */
	boolean skipGenerationCall(GraveComponent grave, Direction direction, DeathContext context, String caller);
}
