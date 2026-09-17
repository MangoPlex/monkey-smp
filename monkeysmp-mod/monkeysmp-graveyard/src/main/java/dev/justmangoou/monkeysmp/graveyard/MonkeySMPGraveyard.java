package dev.justmangoou.monkeysmp.graveyard;

import java.util.*;

import dev.justmangoou.monkeysmp.graveyard.block.GraveBlock;
import dev.justmangoou.monkeysmp.graveyard.block.entity.GraveBlockEntity;
import dev.justmangoou.monkeysmp.graveyard.compat.InvModCompat;
import dev.justmangoou.monkeysmp.graveyard.components.GraveComponent;
import dev.justmangoou.monkeysmp.graveyard.config.ClaimPriority;
import dev.justmangoou.monkeysmp.graveyard.config.MonkeySMPGraveyardConfig;
import dev.justmangoou.monkeysmp.graveyard.events.ServerEventHandler;
import dev.justmangoou.monkeysmp.graveyard.networking.PacketInitializer;
import dev.justmangoou.monkeysmp.graveyard.networking.ServerPacketHandler;
import dev.justmangoou.monkeysmp.graveyard.util.YigdCommands;
import dev.justmangoou.monkeysmp.graveyard.util.YigdResourceHandler;
import dev.justmangoou.monkeysmp.core.MonkeySMPCore;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class MonkeySMPGraveyard implements ModInitializer {
	public static final String MOD_ID = "monkeysmp-graveyard";
	public static Logger LOGGER = LoggerFactory.getLogger("MonkeySMP-Graveyard");

	private static final Identifier GRAVE_ID = MonkeySMPCore.id("grave");
	private static final ResourceKey<Block> GRAVE_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, GRAVE_ID);
	private static final ResourceKey<Item> GRAVE_ITEM_KEY = ResourceKey.create(Registries.ITEM, GRAVE_ID);

	public static GraveBlock GRAVE_BLOCK = new GraveBlock(BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).noOcclusion().setId(GRAVE_BLOCK_KEY));
	public static BlockEntityType<GraveBlockEntity> GRAVE_BLOCK_ENTITY;

	/**
	 * Any runnable added to this list will be executed on the end of the current server tick.
	 * Use if runnable is required to run before some other event that would have otherwise ran before.
	 */
	public static List<Runnable> END_OF_TICK = new ArrayList<>();

	public static Map<UUID, List<String>> NOT_NOTIFIED_ROBBERIES = new HashMap<>();
	public static Map<UUID, ClaimPriority> CLAIM_PRIORITIES = new HashMap<>();
	public static Map<UUID, ClaimPriority> ROB_PRIORITIES = new HashMap<>();

	@Override
	public void onInitialize() {
		AutoConfig.register(MonkeySMPGraveyardConfig.class, GsonConfigSerializer::new);

		GRAVE_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, MonkeySMPCore.id("grave_block_entity"), new BlockEntityType<>(GraveBlockEntity::new, java.util.Set.of(GRAVE_BLOCK)));

		Registry.register(BuiltInRegistries.BLOCK, GRAVE_BLOCK_KEY, GRAVE_BLOCK);
		Registry.register(BuiltInRegistries.ITEM, GRAVE_ITEM_KEY, new BlockItem(GRAVE_BLOCK, new Item.Properties().setId(GRAVE_ITEM_KEY)));

		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, MonkeySMPCore.id("grave_id"), GraveComponent.GRAVE_ID);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(GRAVE_BLOCK.asItem()));

		PacketInitializer.init();

		InvModCompat.reloadModCompat();
		// Makes sure proper mod compatibilities are loaded (on world load to check mods' config)
		ServerLifecycleEvents.SERVER_STARTED.register(server -> InvModCompat.reloadModCompat());

		ServerEventHandler.registerEventCallbacks();
		ServerEventHandler.registerEvents();
		ServerPacketHandler.registerReceivers();
		YigdResourceHandler.init();

		YigdCommands.register();
	}
}
