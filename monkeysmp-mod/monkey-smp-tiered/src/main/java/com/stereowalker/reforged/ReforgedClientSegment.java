package com.stereowalker.reforged;

import com.stereowalker.unionlib.mod.ClientSegment;
import com.stereowalker.unionlib.util.VersionHelper;

import net.minecraft.resources.Identifier;

/**
 * No in-game config screen is registered here: {@link ClientSegment#getConfigScreen}
 * intentionally uses UnionLib's default (none), since this project's Loom setup keeps
 * "main" and "client" as strictly separate compile source sets (splitEnvironmentSourceSets),
 * and this class must stay resolvable from "main" (it is constructed directly by {@link Reforged}).
 * Adjust {@code config/reforged.json} directly to change settings instead.
 */
public class ReforgedClientSegment extends ClientSegment {

	@Override
	public Identifier getModIcon() {
		return VersionHelper.toLoc(Reforged.ID, "textures/icon.png");
	}

}
