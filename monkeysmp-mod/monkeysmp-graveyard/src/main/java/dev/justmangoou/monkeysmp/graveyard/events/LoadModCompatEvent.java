package dev.justmangoou.monkeysmp.graveyard.events;

import java.util.List;

import dev.justmangoou.monkeysmp.graveyard.compat.InvModCompat;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface LoadModCompatEvent {
	Event<LoadModCompatEvent> EVENT = EventFactory.createArrayBacked(LoadModCompatEvent.class, events -> mods -> {
		for (LoadModCompatEvent event : events) {
			event.loadModCompat(mods);
		}
	});

	void loadModCompat(List<InvModCompat<?>> invCompatMods);
}
