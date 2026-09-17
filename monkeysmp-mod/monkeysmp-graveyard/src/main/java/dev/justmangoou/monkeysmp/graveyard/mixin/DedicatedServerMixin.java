package dev.justmangoou.monkeysmp.graveyard.mixin;

import dev.justmangoou.monkeysmp.graveyard.block.entity.GraveBlockEntity;
import dev.justmangoou.monkeysmp.graveyard.config.MonkeySMPGraveyardConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.SERVER)
@Mixin(DedicatedServer.class)
public class DedicatedServerMixin {
	@Inject(method = "isUnderSpawnProtection", at = @At(value = "RETURN"), cancellable = true)
	private void isSpawnProtected(ServerLevel world, BlockPos pos, Player player, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue() || !MonkeySMPGraveyardConfig.getConfig().graveConfig.overrideSpawnProtection) return;

		BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof GraveBlockEntity) cir.setReturnValue(false);
	}
}
