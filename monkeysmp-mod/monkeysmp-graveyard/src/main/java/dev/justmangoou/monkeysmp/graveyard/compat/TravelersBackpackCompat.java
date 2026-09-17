package dev.justmangoou.monkeysmp.graveyard.compat;

import java.util.function.Predicate;

import dev.justmangoou.monkeysmp.graveyard.components.InventoryComponent;
import dev.justmangoou.monkeysmp.graveyard.config.MonkeySMPGraveyardConfig;
import dev.justmangoou.monkeysmp.graveyard.data.DeathContext;
import dev.justmangoou.monkeysmp.graveyard.data.GraveItem;
import dev.justmangoou.monkeysmp.graveyard.events.DropRuleEvent;
import dev.justmangoou.monkeysmp.graveyard.util.DropRule;
import dev.justmangoou.monkeysmp.graveyard.util.YigdNbtUtil;
import dev.justmangoou.monkeysmp.graveyard.MonkeySMPGraveyard;
import com.tiviacz.travelersbackpack.TravelersBackpack;
import com.tiviacz.travelersbackpack.attachment.AttachmentUtils;
import com.tiviacz.travelersbackpack.attachment.BackpackAttachment;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Ported against the 26.2 Traveler's Backpack build, which replaced its "component" API
 * (com.tiviacz.travelersbackpack.component.ComponentUtils / ITravelersBackpack) with an attachment-based one:
 * ComponentUtils became AttachmentUtils, and the ITravelersBackpack interface became the concrete
 * BackpackAttachment class, both under com.tiviacz.travelersbackpack.attachment. Method names/signatures are
 * otherwise unchanged, except AttachmentUtils.getWearingBackpack(player) now always returns a value
 * (ItemStack.EMPTY instead of null when nothing is worn), confirmed against the mod's own bundled
 * UniversalGravesCompat, which integrates with another grave mod the same way this class does.
 */
public class TravelersBackpackCompat implements InvModCompat<GraveItem> {
	public static boolean isIntegrationEnabled() {
		try {
			return TravelersBackpack.enableIntegration();
		}
		catch (Exception | Error e) {
			return false;
		}
	}

	@Override
	public String getModName() {
		return "travelers backpack";
	}

	@Override
	public void clear(ServerPlayer player) {
		AttachmentUtils.getAttachment(player).ifPresent(BackpackAttachment::removeWearable);
	}

	@Override
	public CompatComponent<GraveItem> readNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
		ItemStack stack = YigdNbtUtil.parseOptionalItemStack(registryLookup, nbt);

		DropRule dropRule;
		if (nbt.contains("dropRule")) {
			dropRule = DropRule.valueOf(nbt.getStringOr("dropRule", ""));
		} else {
			dropRule = MonkeySMPGraveyardConfig.getConfig().compatConfig.defaultTravelersBackpackDropRule;
		}
		return new TBCompatComponent(new GraveItem(stack, dropRule));
	}

	@Override
	public CompatComponent<GraveItem> getNewComponent(ServerPlayer player) {
		return new TBCompatComponent(player);
	}

	private static class TBCompatComponent extends CompatComponent<GraveItem> {

		public TBCompatComponent(ServerPlayer player) {
			super(player);
		}

		public TBCompatComponent(GraveItem inventory) {
			super(inventory);
		}

		@Override
		public GraveItem getInventory(ServerPlayer player) {
			DropRule defaultDropRule = MonkeySMPGraveyardConfig.getConfig().compatConfig.defaultTravelersBackpackDropRule;
			ItemStack stack = AttachmentUtils.getWearingBackpack(player);
			return stack.isEmpty() ? InventoryComponent.EMPTY_GRAVE_ITEM : new GraveItem(stack, defaultDropRule);
		}

		@Override
		public NonNullList<GraveItem> merge(CompatComponent<?> mergingComponent, ServerPlayer merger) {
			NonNullList<GraveItem> extraItems = NonNullList.create();

			GraveItem graveItem = (GraveItem) mergingComponent.inventory;
			ItemStack mergingStack = graveItem.stack;
			ItemStack currentStack = this.inventory.stack;

			if (mergingStack.isEmpty()) return extraItems;

			if (!currentStack.isEmpty()) {
				extraItems.add(graveItem);
				return extraItems;
			}

			this.inventory = new GraveItem(mergingStack, graveItem.dropRule);
			return extraItems;
		}

		@Override
		public NonNullList<ItemStack> storeToPlayer(ServerPlayer player) {
			if (this.inventory.stack.isEmpty()) return NonNullList.create();

			AttachmentUtils.equipBackpack(player, this.inventory.stack.copy());

			return NonNullList.create();
		}

		@Override
		public void handleDropRules(DeathContext context) {
			MonkeySMPGraveyardConfig.CompatConfig compatConfig = MonkeySMPGraveyardConfig.getConfig().compatConfig;

			DropRule dropRule = compatConfig.defaultTravelersBackpackDropRule;

			ItemStack stack = this.inventory.stack;
			if (stack.isEmpty()) return;

			if (dropRule == DropRule.PUT_IN_GRAVE)
				dropRule = DropRuleEvent.EVENT.invoker().getDropRule(stack, -1, context, true);

			this.inventory.dropRule = dropRule;
		}

		@Override
		public NonNullList<GraveItem> getAsGraveItemList() {
			NonNullList<GraveItem> stacks = NonNullList.create();
			stacks.add(this.inventory);
			return stacks;
		}

		@Override
		public CompatComponent<GraveItem> filterInv(Predicate<DropRule> predicate) {
			GraveItem graveItem;
			if (predicate.test(this.inventory.dropRule)) {
				graveItem = this.inventory;
			} else {
				graveItem = InventoryComponent.EMPTY_GRAVE_ITEM;
			}
			return new TBCompatComponent(graveItem);
		}

		@Override
		public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
			ItemStack stack = this.inventory.stack;
			if (predicate.test(stack)) {
				stack.shrink(itemCount);
				return true;
			}
			return false;
		}

		@Override
		public void clear() {
			this.inventory = InventoryComponent.EMPTY_GRAVE_ITEM;
		}

		@Override
		public CompoundTag writeNbt(HolderLookup.Provider registries) {
			try {
				CompoundTag nbt = YigdNbtUtil.saveItemStack(this.inventory.stack, registries);
				nbt.putString("dropRule", this.inventory.dropRule.name());
				return nbt;
			}
			catch (Exception e) {
				MonkeySMPGraveyard.LOGGER.error("Error while converting item to NBT: {}", this.inventory.stack, e);
				return new CompoundTag();
			}
		}
	}
}
