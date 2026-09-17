package dev.justmangoou.monkeysmp.graveyard.compat;

import java.util.ArrayList;
import java.util.List;

import dev.justmangoou.monkeysmp.graveyard.config.MonkeySMPGraveyardConfig;
import dev.justmangoou.monkeysmp.graveyard.events.LoadModCompatEvent;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.loader.api.FabricLoader;

public interface InvModCompat<T> {
	List<InvModCompat<?>> invCompatMods = new ArrayList<>();
	static void reloadModCompat() {
		invCompatMods.clear();
		FabricLoader loader = FabricLoader.getInstance();
		MonkeySMPGraveyardConfig.CompatConfig compatConfig = MonkeySMPGraveyardConfig.getConfig().compatConfig;

		if (compatConfig.enableTrinketsCompat && loader.isModLoaded("trinkets_updated"))
			invCompatMods.add(new TrinketsCompat());

		if (compatConfig.enableTravelersBackpackCompat && loader.isModLoaded("travelersbackpack") && TravelersBackpackCompat.isIntegrationEnabled())
			invCompatMods.add(new TravelersBackpackCompat());

		LoadModCompatEvent.EVENT.invoker().loadModCompat(invCompatMods);
	}

	String getModName();
	void clear(ServerPlayer player);
	CompatComponent<T> readNbt(CompoundTag nbt, HolderLookup.Provider registryLookup);

	CompatComponent<T> getNewComponent(ServerPlayer player);
}
