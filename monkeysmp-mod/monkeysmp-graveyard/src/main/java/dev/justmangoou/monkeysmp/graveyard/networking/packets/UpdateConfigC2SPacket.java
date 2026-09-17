package dev.justmangoou.monkeysmp.graveyard.networking.packets;

import dev.justmangoou.monkeysmp.graveyard.config.ClaimPriority;
import dev.justmangoou.monkeysmp.graveyard.MonkeySMPGraveyard;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UpdateConfigC2SPacket(ClaimPriority claiming, ClaimPriority robbing) implements CustomPacketPayload {
	public static final Type<UpdateConfigC2SPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MonkeySMPGraveyard.MOD_ID, "update_config"));
	public static final StreamCodec<RegistryFriendlyByteBuf, UpdateConfigC2SPacket> STREAM_CODEC = StreamCodec.ofMember(UpdateConfigC2SPacket::write, UpdateConfigC2SPacket::new);

	@Override
	public @NotNull Type<UpdateConfigC2SPacket> type() {
		return TYPE;
	}

	public UpdateConfigC2SPacket(RegistryFriendlyByteBuf buf) {
		this(buf.readEnum(ClaimPriority.class), buf.readEnum(ClaimPriority.class));
	}
	public void write(RegistryFriendlyByteBuf buf) {
		buf.writeEnum(this.claiming);
		buf.writeEnum(this.robbing);
	}
}
