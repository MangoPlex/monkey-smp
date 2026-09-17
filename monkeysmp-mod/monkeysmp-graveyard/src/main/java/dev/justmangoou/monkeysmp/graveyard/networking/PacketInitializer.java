package dev.justmangoou.monkeysmp.graveyard.networking;

import dev.justmangoou.monkeysmp.graveyard.networking.packets.*;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class PacketInitializer {
	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(UpdateConfigC2SPacket.TYPE, UpdateConfigC2SPacket.STREAM_CODEC);

		PayloadTypeRegistry.clientboundPlay().register(SyncConfigS2CPacket.TYPE, SyncConfigS2CPacket.STREAM_CODEC);
	}
}
