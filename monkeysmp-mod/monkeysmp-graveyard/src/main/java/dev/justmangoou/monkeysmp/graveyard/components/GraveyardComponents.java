package dev.justmangoou.monkeysmp.graveyard.components;

import dev.justmangoou.monkeysmp.graveyard.data.DeathInfoManager;
import dev.justmangoou.monkeysmp.graveyard.MonkeySMPGraveyard;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v8.level.LevelComponentFactoryRegistry;
import org.ladysnake.cca.api.v8.level.LevelComponentInitializer;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public final class GraveyardComponents implements EntityComponentInitializer, LevelComponentInitializer {
	public static final ComponentKey<PlayerGraveData> PLAYER_DATA = ComponentRegistryV3.INSTANCE.getOrCreate(
			Identifier.fromNamespaceAndPath(MonkeySMPGraveyard.MOD_ID, "player_data"), PlayerGraveData.class);
	public static final ComponentKey<DeathInfoManager> GRAVEYARD_DATA = ComponentRegistryV3.INSTANCE.getOrCreate(
			Identifier.fromNamespaceAndPath(MonkeySMPGraveyard.MOD_ID, "graveyard_data"), DeathInfoManager.class);

	@Override
	public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
		registry.registerForPlayers(PLAYER_DATA, PlayerGraveData::new);
	}

	@Override
	public void registerLevelComponentFactories(LevelComponentFactoryRegistry registry) {
		registry.registerFor(Level.OVERWORLD, GRAVEYARD_DATA, DeathInfoManager.class, DeathInfoManager::new);
	}
}
