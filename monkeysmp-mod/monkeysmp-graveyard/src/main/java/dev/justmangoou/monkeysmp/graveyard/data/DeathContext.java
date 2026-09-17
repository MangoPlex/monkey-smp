package dev.justmangoou.monkeysmp.graveyard.data;

import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

public record DeathContext(ServerPlayer player, @NotNull ServerLevel world, Vec3 deathPos,
						DamageSource deathSource) {
}
