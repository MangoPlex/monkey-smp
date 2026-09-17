package dev.justmangoou.monkeysmp.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;

public class MonkeySMPCore implements ModInitializer {
	public static final String BASE_ID = "monkeysmp";
	public static final Logger LOGGER = LoggerFactory.getLogger(BASE_ID);

	@Override
	public void onInitialize() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(BASE_ID, path);
	}
}
