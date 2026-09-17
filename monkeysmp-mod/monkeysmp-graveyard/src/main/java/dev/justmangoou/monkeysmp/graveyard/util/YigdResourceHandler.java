package dev.justmangoou.monkeysmp.graveyard.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import dev.justmangoou.monkeysmp.graveyard.block.GraveBlock;
import dev.justmangoou.monkeysmp.graveyard.components.GraveComponent;
import dev.justmangoou.monkeysmp.graveyard.data.GraveyardData;
import com.google.gson.*;
import dev.justmangoou.monkeysmp.core.MonkeySMPCore;
import dev.justmangoou.monkeysmp.graveyard.MonkeySMPGraveyard;

import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;

public class YigdResourceHandler {
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(Identifier.class, (JsonDeserializer<Identifier>) (elem, type, context) -> Identifier.parse(elem.getAsString()))
			.registerTypeAdapter(Vec3i.class, (JsonDeserializer<Vec3i>) (elem, type, context) -> new Vec3i(
					elem.getAsJsonArray().get(0).getAsInt(),
					elem.getAsJsonArray().get(1).getAsInt(),
					elem.getAsJsonArray().get(2).getAsInt()))
			.create();

	public static void init() {
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new GraveServerModelLoader());
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new GraveyardDataLoader());
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new GraveAreaOverrideLoader());
	}

	private static class GraveServerModelLoader implements SimpleSynchronousResourceReloadListener {
		@Override
		public Identifier getFabricId() {
			return Identifier.fromNamespaceAndPath(MonkeySMPGraveyard.MOD_ID, "custom_server_grave_shape");
		}

		@Override
		public void onResourceManagerReload(ResourceManager manager) {
			Identifier resourceLocation = MonkeySMPCore.id("custom/grave_shape.json");
			List<Resource> resources = manager.getResourceStack(resourceLocation);

			for (Resource resource : resources) {
				try (InputStream is = resource.open()) {
					MonkeySMPGraveyard.LOGGER.info("Reloading grave shape (server)");
					JsonObject resourceJson = (JsonObject) JsonParser.parseReader(new InputStreamReader(is));
					GraveBlock.reloadShapeFromJson(resourceJson);

					MonkeySMPGraveyard.LOGGER.info("Grave model and shape reload successful (server)");
				}
				catch (IOException | ClassCastException | NullPointerException e) {
					MonkeySMPGraveyard.LOGGER.error("Could not load resource `%s` from datapack `%s`".formatted(resourceLocation, resource.sourcePackId()), e);
				}
			}
		}
	}
	private static class GraveyardDataLoader implements SimpleSynchronousResourceReloadListener {
		@Override
		public Identifier getFabricId() {
			return Identifier.fromNamespaceAndPath(MonkeySMPGraveyard.MOD_ID, "graveyard");
		}

		@Override
		public void onResourceManagerReload(ResourceManager manager) {
			Identifier resourceLocation = MonkeySMPCore.id("custom/graveyard.json");
			List<Resource> resources = manager.getResourceStack(resourceLocation);

			for (Resource resource : resources) {
				try (InputStream is = resource.open()) {
					MonkeySMPGraveyard.LOGGER.info("Reloading YIGD graveyard data (server)");
					GraveComponent.graveyardData = GSON.fromJson(new InputStreamReader(is), GraveyardData.class);
					GraveComponent.graveyardData.handlePoint2Point();

					MonkeySMPGraveyard.LOGGER.info("Graveyard data successfully reloaded (server)");
				}
				catch (IOException | ClassCastException | NullPointerException e) {
					MonkeySMPGraveyard.LOGGER.error("Could not load resource `%s` from datapack `%s`".formatted(resourceLocation, resource.sourcePackId()), e);
				}
			}
		}
	}
	private static class GraveAreaOverrideLoader implements SimpleSynchronousResourceReloadListener {
		@Override
		public Identifier getFabricId() {
			return Identifier.fromNamespaceAndPath(MonkeySMPGraveyard.MOD_ID, "grave_area_override");
		}

		@Override
		public void onResourceManagerReload(ResourceManager manager) {
			Identifier resourceLocation = MonkeySMPCore.id("custom/grave_areas.json");
			List<Resource> resources = manager.getResourceStack(resourceLocation);

			for (Resource resource : resources) {
				try (InputStream is = resource.open()) {
					MonkeySMPGraveyard.LOGGER.info("Reloading YIGD grave area overrides (server)");
					GraveOverrideAreas.INSTANCE = GSON.fromJson(new InputStreamReader(is), GraveOverrideAreas.class);

					MonkeySMPGraveyard.LOGGER.info("Grave area overrides successfully reloaded (server)");
				}
				catch (IOException | ClassCastException | NullPointerException e) {
					MonkeySMPGraveyard.LOGGER.error("Could not load resource `%s` from datapack `%s`".formatted(resourceLocation, resource.sourcePackId()), e);
				}
			}
		}
	}
}
