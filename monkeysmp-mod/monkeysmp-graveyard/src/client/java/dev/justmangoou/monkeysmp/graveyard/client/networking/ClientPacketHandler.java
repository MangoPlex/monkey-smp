package dev.justmangoou.monkeysmp.graveyard.client.networking;

import dev.justmangoou.monkeysmp.graveyard.config.MonkeySMPGraveyardConfig;
import dev.justmangoou.monkeysmp.graveyard.networking.packets.*;
import dev.justmangoou.monkeysmp.graveyard.client.render.GraveBlockEntityRenderer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientPacketHandler {
	public static void registerReceivers() {
		ClientPlayNetworking.registerGlobalReceiver(SyncConfigS2CPacket.TYPE, (payload, context) -> {
			MonkeySMPGraveyardConfig.getConfig().graveConfig.retrieveMethods.onBreak = payload.gravesBreakable();
			GraveBlockEntityRenderer.syncedGlowing = payload.glowingGraves();
			GraveBlockEntityRenderer.syncedGlowingMaxDistance = payload.maxGraveGlowingDistance();
			GraveBlockEntityRenderer.syncedDeathSightDistance = payload.deathSightDistance();
		});
	}

	public static void sendConfigUpdate(MonkeySMPGraveyardConfig config) {
		ClientPlayNetworking.send(new UpdateConfigC2SPacket(config.graveConfig.claimPriority, config.graveConfig.graveRobbing.robPriority));
	}
}
