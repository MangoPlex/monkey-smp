package dev.justmangoou.monkeysmp.graveyard.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import dev.justmangoou.monkeysmp.graveyard.block.GraveBlock;
import dev.justmangoou.monkeysmp.graveyard.config.MonkeySMPGraveyardConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.justmangoou.monkeysmp.core.MonkeySMPCore;
import dev.justmangoou.monkeysmp.graveyard.MonkeySMPGraveyard;
import dev.justmangoou.monkeysmp.graveyard.client.events.ClientEventHandler;
import dev.justmangoou.monkeysmp.graveyard.client.networking.ClientPacketHandler;
import dev.justmangoou.monkeysmp.graveyard.client.render.GraveBlockEntityRenderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

public class MonkeySMPGraveyardClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockEntityRenderers.register(MonkeySMPGraveyard.GRAVE_BLOCK_ENTITY, GraveBlockEntityRenderer::new);

		ClientPacketHandler.registerReceivers();
		ClientEventHandler.registerEventCallbacks();
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
				Identifier.fromNamespaceAndPath(MonkeySMPGraveyard.MOD_ID, "custom_grave_model"),
				new GraveResourceLoader()
		);

		// There is no event handler for standard minecraft client events, so this is used here
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ClientPacketHandler.sendConfigUpdate(MonkeySMPGraveyardConfig.getConfig()));
	}

	private static class GraveResourceLoader implements ResourceManagerReloadListener {
		@Override
		public void onResourceManagerReload(ResourceManager manager) {
			Identifier resourceLocation = MonkeySMPCore.id("models/block/grave.json");
			List<Resource> resources = manager.getResourceStack(resourceLocation);

			for (Resource resource : resources) {
				try (InputStream is = resource.open()) {
					MonkeySMPGraveyard.LOGGER.info("Reloading grave model (client)");
					JsonObject resourceJson = (JsonObject) JsonParser.parseReader(new InputStreamReader(is));
					GraveBlockEntityRenderer.reloadModelFromJson(resourceJson);
					GraveBlock.reloadShapeFromJson(resourceJson);

					MonkeySMPGraveyard.LOGGER.info("Grave model and shape reload successful (client)");
				} catch (IOException | ClassCastException | NullPointerException e) {
					MonkeySMPGraveyard.LOGGER.error("Could not load resource `{}` from resource pack `{}`", resourceLocation, resource.sourcePackId(), e);
				}
			}
		}
	}
}
