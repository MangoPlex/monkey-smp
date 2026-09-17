package dev.justmangoou.monkeysmp.graveyard.data;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

public record DirectionalPos(BlockPos pos, Direction dir) {
	public DirectionalPos(int x, int y, int z, Direction dir) {
		this(new BlockPos(x, y, z), dir);
	}

	public double getSquaredDistance(Vec3i pos) {
		return this.pos.distSqr(pos);
	}
}
