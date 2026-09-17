package dev.justmangoou.monkeysmp.graveyard.events;

import dev.justmangoou.monkeysmp.graveyard.data.DeathContext;
import dev.justmangoou.monkeysmp.graveyard.util.DropRule;
import dev.justmangoou.monkeysmp.graveyard.util.GraveOverrideAreas;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface DropRuleEvent {
	Event<DropRuleEvent> EVENT = EventFactory.createArrayBacked(DropRuleEvent.class, destroyItems -> (item, slot, context, modify) -> {
		for (DropRuleEvent event : destroyItems) {
			DropRule dropRule = event.getDropRule(item, slot, context, modify);
			if (dropRule != GraveOverrideAreas.INSTANCE.defaultDropRule) {
				return dropRule;
			}
		}
		return GraveOverrideAreas.INSTANCE.defaultDropRule;
	});

	DropRule getDropRule(ItemStack item, int slot, @Nullable DeathContext deathContext, boolean modify);
}
