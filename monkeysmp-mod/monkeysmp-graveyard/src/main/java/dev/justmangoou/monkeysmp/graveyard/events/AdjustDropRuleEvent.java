package dev.justmangoou.monkeysmp.graveyard.events;

import dev.justmangoou.monkeysmp.graveyard.components.InventoryComponent;
import dev.justmangoou.monkeysmp.graveyard.data.DeathContext;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface AdjustDropRuleEvent {
	Event<AdjustDropRuleEvent> EVENT = EventFactory.createArrayBacked(AdjustDropRuleEvent.class, events -> (inventory, context) -> {
		for (AdjustDropRuleEvent event : events) {
			event.adjustDropRules(inventory, context);
		}
	});

	void adjustDropRules(InventoryComponent inventoryComponent, DeathContext context);
}
