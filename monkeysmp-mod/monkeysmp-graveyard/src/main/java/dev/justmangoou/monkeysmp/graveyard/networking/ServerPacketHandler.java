package dev.justmangoou.monkeysmp.graveyard.networking;

import java.util.UUID;

import dev.justmangoou.monkeysmp.graveyard.config.ClaimPriority;
import dev.justmangoou.monkeysmp.graveyard.config.MonkeySMPGraveyardConfig;
import dev.justmangoou.monkeysmp.graveyard.networking.packets.SyncConfigS2CPacket;
import dev.justmangoou.monkeysmp.graveyard.networking.packets.UpdateConfigC2SPacket;
import dev.justmangoou.monkeysmp.graveyard.MonkeySMPGraveyard;

import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ServerPacketHandler {
	public static void registerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(UpdateConfigC2SPacket.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();

			ClaimPriority claimPriority = payload.claiming();
			ClaimPriority robPriority = payload.robbing();

			UUID playerId = player.getUUID();
			MonkeySMPGraveyard.CLAIM_PRIORITIES.put(playerId, claimPriority);
			MonkeySMPGraveyard.ROB_PRIORITIES.put(playerId, robPriority);

			MonkeySMPGraveyard.LOGGER.info("Priority overwritten for player {}. Claiming: {} / Robbing: {}", player.getGameProfile().name(), claimPriority.name(), robPriority.name());
		});
	}

	public static void sendConfigSyncPacket(ServerPlayer player) {
		MonkeySMPGraveyardConfig config = MonkeySMPGraveyardConfig.getConfig();
		ServerPlayNetworking.send(player, new SyncConfigS2CPacket(
				config.graveConfig.allowBreakRetrieve,
				config.graveRendering.useGlowingEffect,
				config.graveRendering.glowingDistance,
				config.extraFeatures.deathSightEnchant.range));
	}
}
