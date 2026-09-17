package dev.justmangoou.monkeysmp.graveyard.util;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;

/**
 * Component.Serializer.toJson/fromJson was removed; Component now only exposes a Codec (ComponentSerialization.CODEC).
 * This mirrors the old toJson/fromJson(String, HolderLookup.Provider) call sites used throughout this mod.
 */
public final class YigdTextUtil {
	private YigdTextUtil() {
	}

	public static String toJson(Component component, HolderLookup.Provider registryLookup) {
		return ComponentSerialization.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registryLookup), component).getOrThrow().toString();
	}

	public static Component fromJson(String json, HolderLookup.Provider registryLookup) {
		return ComponentSerialization.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registryLookup), JsonParser.parseString(json)).getOrThrow();
	}
}
