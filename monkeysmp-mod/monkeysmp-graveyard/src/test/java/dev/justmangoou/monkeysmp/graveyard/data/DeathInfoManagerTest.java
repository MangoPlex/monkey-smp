package dev.justmangoou.monkeysmp.graveyard.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import dev.justmangoou.monkeysmp.graveyard.components.ExpComponent;
import dev.justmangoou.monkeysmp.graveyard.components.GraveComponent;
import dev.justmangoou.monkeysmp.graveyard.components.InventoryComponent;
import com.mojang.authlib.GameProfile;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;

class DeathInfoManagerTest {
	private static final HolderLookup.Provider EMPTY_LOOKUP = HolderLookup.Provider.create(Stream.empty());

	@Test
	void roundTripsGravesAndListSettings() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		GameProfile profile = new GameProfile(UUID.fromString("1b3f69c4-e00f-4b09-8bc7-97509a77f024"), "Monkey");
		ResolvableProfile owner = ResolvableProfile.createResolved(profile);
		UUID graveId = UUID.fromString("2e6a99a9-9ee1-436e-8dad-a876a44056b7");
		CompoundTag expTag = new CompoundTag();
		expTag.putInt("value", 42);
		GraveComponent grave = new GraveComponent(
				owner,
				InventoryComponent.fromNbt(new CompoundTag(), EMPTY_LOOKUP),
				ExpComponent.fromNbt(expTag),
				Level.NETHER,
				new BlockPos(12, 34, 56),
				Component.literal("fell from a high place"),
				graveId,
				GraveStatus.UNCLAIMED,
				true,
				new TimePoint(1200, 24000, LocalDateTime.of(2026, 9, 17, 12, 30)),
				null);

		DeathInfoManager original = new DeathInfoManager(null);
		CompoundTag seed = GraveDataTestUtil.managerTag(owner, grave, EMPTY_LOOKUP);
		original.load(seed, EMPTY_LOOKUP);
		original.setGraveListMode(ListMode.WHITELIST);
		original.addToList(owner);

		CompoundTag saved = original.save(new CompoundTag(), EMPTY_LOOKUP);
		DeathInfoManager restored = new DeathInfoManager(null);
		restored.load(saved, EMPTY_LOOKUP);

		assertEquals(ListMode.WHITELIST, restored.getGraveListMode());
		assertTrue(restored.isInList(owner));
		GraveComponent restoredGrave = restored.getGrave(graveId).orElseThrow();
		assertEquals(new BlockPos(12, 34, 56), restoredGrave.getPos());
		assertEquals(Level.NETHER, restoredGrave.getWorldRegistryKey());
		assertEquals(42, restoredGrave.getExpComponent().getStoredXp());
	}
}
