package dev.justmangoou.monkeysmp.graveyard.mixin;

import java.util.HashMap;
import java.util.Map;

import dev.justmangoou.monkeysmp.core.MonkeySMPCore;
import dev.justmangoou.monkeysmp.graveyard.config.MonkeySMPGraveyardConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * This mixin is present so that the config can still toggle if the Death Sight enchantment is loaded (without a bunch of fuzz).
 * Ported from RegistryDataLoaderMixin in the upstream YIGD project, which injected into a per-element
 * loadElementFromResource hook that no longer exists on this Minecraft version. Registry resource discovery
 * now happens per-registry in ResourceManagerRegistryLoadTask, but its call to listMatchingResources() is
 * made from inside a lambda passed to CompletableFuture.supplyAsync(), so there's no plain "load" method
 * bytecode to redirect from there. Filtering the result of listMatchingResources() itself, here on its own
 * (non-lambda) declaring class, sidesteps that entirely - and since only the enchantment registry's listing
 * will ever actually contain "death_sight", no registry-check is even needed before removing it.
 */
@Mixin(FileToIdConverter.class)
public abstract class FileToIdConverterMixin {
	@Shadow
	public abstract Identifier idToFile(Identifier id);

	@Inject(method = "listMatchingResources", at = @At("RETURN"), cancellable = true)
	private void monkeysmp$maybeSkipDisabledEnchantments(ResourceManager manager, CallbackInfoReturnable<Map<Identifier, Resource>> cir) {
		if (MonkeySMPGraveyardConfig.getConfig().extraFeatures.deathSightEnchant.enabled) return;

		Identifier deathSightFile = this.idToFile(MonkeySMPCore.id("death_sight"));
		Map<Identifier, Resource> resources = cir.getReturnValue();
		if (!resources.containsKey(deathSightFile)) return;

		Map<Identifier, Resource> filtered = new HashMap<>(resources);
		filtered.remove(deathSightFile);
		cir.setReturnValue(filtered);
	}
}
