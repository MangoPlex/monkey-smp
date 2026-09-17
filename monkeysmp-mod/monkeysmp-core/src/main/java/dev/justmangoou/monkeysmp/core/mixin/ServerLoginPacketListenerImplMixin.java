package dev.justmangoou.monkeysmp.core.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.server.network.ServerLoginPacketListenerImpl;

@Mixin(ServerLoginPacketListenerImpl.class)
public class ServerLoginPacketListenerImplMixin {
//    @Redirect(method = "handleHello",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/server/MinecraftServer;usesAuthentication()Z"
//            )
//    )
//    private boolean hackyOnlineMode(@NotNull MinecraftServer instance, ServerboundHelloPacket packet) {
//        return instance.usesAuthentication() && !((MinecraftServerBridge) instance).getWhitelistNames().contains(packet.name());
//    }
}
