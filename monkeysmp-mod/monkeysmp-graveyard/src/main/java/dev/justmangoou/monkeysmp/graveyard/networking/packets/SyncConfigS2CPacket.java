package dev.justmangoou.monkeysmp.graveyard.networking.packets;

import dev.justmangoou.monkeysmp.graveyard.MonkeySMPGraveyard;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncConfigS2CPacket(boolean gravesBreakable, boolean glowingGraves, int maxGraveGlowingDistance, double deathSightDistance) implements CustomPacketPayload {
	public static final Type<SyncConfigS2CPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MonkeySMPGraveyard.MOD_ID, "sync_config"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncConfigS2CPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, SyncConfigS2CPacket::gravesBreakable,
			ByteBufCodecs.BOOL, SyncConfigS2CPacket::glowingGraves,
			ByteBufCodecs.INT, SyncConfigS2CPacket::maxGraveGlowingDistance,
			ByteBufCodecs.DOUBLE, SyncConfigS2CPacket::deathSightDistance,
			SyncConfigS2CPacket::new);
	@Override
	public @NotNull Type<SyncConfigS2CPacket> type() {
		return TYPE;
	}
}
