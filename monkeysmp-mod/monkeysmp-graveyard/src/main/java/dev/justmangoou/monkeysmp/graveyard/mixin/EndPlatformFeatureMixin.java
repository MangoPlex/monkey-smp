package dev.justmangoou.monkeysmp.graveyard.mixin;

import dev.justmangoou.monkeysmp.graveyard.MonkeySMPGraveyard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;

@Mixin(EndPlatformFeature.class)
public class EndPlatformFeatureMixin {
	@Redirect(method = "createEndPlatform", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"))
	private static boolean preserveGraves(BlockState state, Object block) {
		return state.is(MonkeySMPGraveyard.GRAVE_BLOCK) || state.is((Block) block);
	}
}
