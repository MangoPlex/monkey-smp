package dev.justmangoou.monkeysmp.graveyard.compat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import dev.justmangoou.monkeysmp.graveyard.components.InventoryComponent;
import dev.justmangoou.monkeysmp.graveyard.config.MonkeySMPGraveyardConfig;
import dev.justmangoou.monkeysmp.graveyard.data.DeathContext;
import dev.justmangoou.monkeysmp.graveyard.data.GraveItem;
import dev.justmangoou.monkeysmp.graveyard.events.DropRuleEvent;
import dev.justmangoou.monkeysmp.graveyard.util.DropRule;
import dev.justmangoou.monkeysmp.graveyard.util.YigdNbtUtil;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Ported against "Trinkets Updated" (eu.pb4:trinkets, mod id trinkets_updated), the maintained fork of the
 * original dev.emi:trinkets used upstream. Its API stays close to the original: TrinketsApi.getAttachment(entity)
 * replaces the old Optional-wrapped getTrinketComponent(player) (an attachment always exists, no Optional needed),
 * TrinketAttachment.getInventory() keeps the same deprecated-but-present Map&lt;group, Map&lt;slot, TrinketInventory&gt;&gt;
 * shape as before, SlotReference became TrinketSlotAccess (same 2-arg constructor), the per-item "Trinket" interface
 * was replaced by TrinketCallback.getCallback(stack), and TrinketEnums.DropRule became the top-level TrinketDropRule
 * enum (KEEP/DROP/DESTROY/DEFAULT; DROP is new and is treated the same as DEFAULT here).
 */
public class TrinketsCompat implements InvModCompat<Map<String, Map<String, NonNullList<GraveItem>>>> {

	@Override
	public String getModName() {
		return "trinkets";
	}

	@Override
	public void clear(ServerPlayer player) {
		TrinketAttachment attachment = TrinketsApi.getAttachment(player);
		for (Map.Entry<String, Map<String, TrinketInventory>> groupEntry : attachment.getInventory().entrySet()) {
			for (Map.Entry<String, TrinketInventory> slotEntry : groupEntry.getValue().entrySet()) {
				slotEntry.getValue().clearContent();
			}
		}
	}

	@Override
	public CompatComponent<Map<String, Map<String, NonNullList<GraveItem>>>> readNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
		Map<String, Map<String, NonNullList<GraveItem>>> inventory = new HashMap<>();

		for (String groupName : nbt.keySet()) {
			CompoundTag groupNbt = nbt.getCompoundOrEmpty(groupName);
			Map<String, NonNullList<GraveItem>> groupMap = new HashMap<>();

			for (String slotName : groupNbt.keySet()) {
				CompoundTag slotNbt = groupNbt.getCompoundOrEmpty(slotName);
				NonNullList<GraveItem> items = InventoryComponent.listFromNbt(slotNbt, itemNbt -> {
					ItemStack stack = YigdNbtUtil.parseOptionalItemStack(registryLookup, itemNbt);
					if (stack.isEmpty()) return InventoryComponent.EMPTY_GRAVE_ITEM;

					DropRule dropRule;
					if (itemNbt.contains("dropRule")) {
						// We need to check in case the drop rule is a trinket drop rule (only has one difference and that is trinkets have DEFAULT)
						String dropRuleString = itemNbt.getStringOr("dropRule", "");
						if (dropRuleString.equals("DEFAULT")) {
							dropRule = MonkeySMPGraveyardConfig.getConfig().compatConfig.defaultTrinketsDropRule;
						} else {
							dropRule = DropRule.valueOf(dropRuleString);
						}
					} else {
						dropRule = MonkeySMPGraveyardConfig.getConfig().compatConfig.defaultTrinketsDropRule;
					}

					return new GraveItem(stack, dropRule);
				}, InventoryComponent.EMPTY_GRAVE_ITEM, "inventory", "size");

				groupMap.put(slotName, items);
			}

			inventory.put(groupName, groupMap);
		}

		return new TrinketsCompatComponent(inventory);
	}

	@Override
	public CompatComponent<Map<String, Map<String, NonNullList<GraveItem>>>> getNewComponent(ServerPlayer player) {
		return new TrinketsCompatComponent(player);
	}


	private static class TrinketsCompatComponent extends CompatComponent<Map<String, Map<String, NonNullList<GraveItem>>>> {

		public TrinketsCompatComponent(ServerPlayer player) {
			super(player);
		}
		public TrinketsCompatComponent(Map<String, Map<String, NonNullList<GraveItem>>> inventory) {
			super(inventory);
		}

		private DropRule convertDropRule(TrinketDropRule dropRule) {
			return switch (dropRule) {
				case KEEP -> DropRule.KEEP;
				case DESTROY -> DropRule.DESTROY;
				default -> MonkeySMPGraveyardConfig.getConfig().compatConfig.defaultTrinketsDropRule;
			};
		}

		@Override
		public Map<String, Map<String, NonNullList<GraveItem>>> getInventory(ServerPlayer player) {
			Map<String, Map<String, NonNullList<GraveItem>>> items = new HashMap<>();

			TrinketAttachment attachment = TrinketsApi.getAttachment(player);
			for (Map.Entry<String, Map<String, TrinketInventory>> group : attachment.getInventory().entrySet()) {
				String groupString = group.getKey();
				Map<String, NonNullList<GraveItem>> slotMap = new HashMap<>();
				for (Map.Entry<String, TrinketInventory> slot : group.getValue().entrySet()) {
					String slotString = slot.getKey();
					TrinketInventory trinketInventory = slot.getValue();

					NonNullList<GraveItem> itemsInInventory = NonNullList.create();
					for (int i = 0; i < trinketInventory.getContainerSize(); i++) {
						ItemStack stack = trinketInventory.getItem(i);
						TrinketSlotAccess ref = new TrinketSlotAccess(trinketInventory, i);
						TrinketDropRule dropRule = TrinketCallback.getCallback(stack).getDropRule(stack, ref, player);

						itemsInInventory.add(new GraveItem(trinketInventory.getItem(i), this.convertDropRule(dropRule)));
					}

					slotMap.put(slotString, itemsInInventory);
				}
				items.put(groupString, slotMap);
			}

			return items;
		}

		@Override
		public NonNullList<ItemStack> pullBindingCurseItems(ServerPlayer playerRef) {
			NonNullList<ItemStack> noUnequipItems = NonNullList.create();

			if (!MonkeySMPGraveyardConfig.getConfig().graveConfig.treatBindingCurse) return noUnequipItems;

			Map<String, Map<String, TrinketInventory>> trinketInventory = TrinketsApi.getAttachment(playerRef).getInventory();

			for (Map.Entry<String, Map<String, NonNullList<GraveItem>>> group : this.inventory.entrySet()) {
				Map<String, TrinketInventory> componentSlots = trinketInventory.get(group.getKey());
				if (componentSlots == null) continue;

				for (Map.Entry<String, NonNullList<GraveItem>> slot : group.getValue().entrySet()) {
					TrinketInventory trinketSlot = componentSlots.get(slot.getKey());
					if (trinketSlot == null) continue;

					NonNullList<GraveItem> slotItems = slot.getValue();
					for (int i = 0; i < slotItems.size(); i++) {
						GraveItem graveItem = slotItems.get(i);
						ItemStack item = graveItem.stack;
						if (item.isEmpty()) {
							continue;
						}
						TrinketSlotAccess ref = new TrinketSlotAccess(trinketSlot, i);
						if (!TrinketCallback.getCallback(item).canUnequip(item, ref, playerRef)) {
							noUnequipItems.add(item.copy());
							slotItems.set(i, InventoryComponent.EMPTY_GRAVE_ITEM);
						}
					}
				}
			}

			return noUnequipItems;
		}

		@Override
		public NonNullList<GraveItem> merge(CompatComponent<?> mergingComponent, ServerPlayer merger) {
			NonNullList<GraveItem> extraItems = NonNullList.create();

			Map<String, Map<String, TrinketInventory>> trinketInventory = TrinketsApi.getAttachment(merger).getInventory();

			@SuppressWarnings("unchecked")
			Map<String, Map<String, NonNullList<GraveItem>>> mergingInventory = (Map<String, Map<String, NonNullList<GraveItem>>>) mergingComponent.inventory;
			for (Map.Entry<String, Map<String, NonNullList<GraveItem>>> groupEntry : mergingInventory.entrySet()) {  // From merging
				String groupName = groupEntry.getKey();
				Map<String, NonNullList<GraveItem>> slotMap = this.inventory.get(groupName);  // From this
				if (slotMap == null) {
					for (NonNullList<GraveItem> items : groupEntry.getValue().values()) {
						for (GraveItem graveItem : items) {
							extraItems.add(graveItem.copy());  // Solves the issue where the itemstacks are the same instance
						}
					}
					continue;
				}
				for (Map.Entry<String, NonNullList<GraveItem>> slotEntry : groupEntry.getValue().entrySet()) {  // From merging
					String slotName = slotEntry.getKey();
					NonNullList<GraveItem> stacks = slotMap.get(slotName);  // From this
					NonNullList<GraveItem> mergingItems = slotEntry.getValue();  // From merging
					if (stacks == null) {
						for (GraveItem graveItem : mergingItems) {
							extraItems.add(graveItem.copy());  // Solves the issue where the itemstacks are the same instance
						}
						continue;
					}

					for (int i = 0; i < mergingItems.size(); i++) {
						GraveItem graveItem = mergingItems.get(i);
						GraveItem mergingGraveItem = graveItem.copy();  // Solves the issue where the itemstacks are the same instance

						if (stacks.size() <= i) {
							extraItems.add(mergingGraveItem);
							continue;
						}

						GraveItem currentGraveItem = stacks.get(i);
						if (MonkeySMPGraveyardConfig.getConfig().graveConfig.treatBindingCurse && !this.canUnequip(trinketInventory, slotName, groupName, i, mergingGraveItem.stack, merger)) {
							extraItems.add(currentGraveItem);  // Add the current item to extraItems (as it's being replaced)
							stacks.set(i, new GraveItem(mergingGraveItem.stack, graveItem.dropRule));  // Can't be unequipped, so it's prioritized
							continue;  // Already set the item, so we can skip the rest
						}

						if (!currentGraveItem.stack.isEmpty()) {
							extraItems.add(mergingGraveItem);
							continue;
						}

						stacks.set(i, new GraveItem(mergingGraveItem.stack, graveItem.dropRule));
					}
				}
			}

			extraItems.removeIf(graveItem -> graveItem.stack.isEmpty());
			return extraItems;
		}
		private boolean canUnequip(@Nullable Map<String, Map<String, TrinketInventory>> trinketInventory, String slot, String group, int index, ItemStack item, ServerPlayer player) {
			if (trinketInventory == null) return true;
			Map<String, TrinketInventory> trinketGroup = trinketInventory.get(group);
			if (trinketGroup == null) return true;
			TrinketInventory inventory = trinketGroup.get(slot);
			if (inventory == null || inventory.getContainerSize() <= index) return true;

			if (item.isEmpty()) return true;
			TrinketSlotAccess ref = new TrinketSlotAccess(inventory, index);
			return TrinketCallback.getCallback(item).canUnequip(item, ref, player);
		}

		@Override
		public NonNullList<ItemStack> storeToPlayer(ServerPlayer player) {
			NonNullList<ItemStack> extraItems = NonNullList.create();

			Map<String, Map<String, TrinketInventory>> trinketInventory = TrinketsApi.getAttachment(player).getInventory();
			// Traverse through groups
			for (Map.Entry<String, Map<String, NonNullList<GraveItem>>> group : this.inventory.entrySet()) {
				Map<String, TrinketInventory> componentSlots = trinketInventory.get(group.getKey());
				if (componentSlots == null) {  // The trinket group is missing, and all those items need to be added to extraItems
					for (NonNullList<GraveItem> itemList : group.getValue().values()) {
						for (GraveItem graveItem : itemList) {
							extraItems.add(graveItem.stack.copy());
						}
					}
					continue;
				}

				// Traverse through slots
				for (Map.Entry<String, NonNullList<GraveItem>> slot : group.getValue().entrySet()) {
					TrinketInventory slotInventory = componentSlots.get(slot.getKey());

					NonNullList<GraveItem> slotItems = slot.getValue();

					if (slotInventory == null) {  // The trinket slot is missing, and all those items need to be added to extraItems
						for (GraveItem graveItem : slotItems) {
							extraItems.add(graveItem.stack.copy());
						}
						continue;
					}

					// Traverse through item stacks
					for (int i = 0; i < slotItems.size(); i++) {
						GraveItem graveItem = slotItems.get(i);
						ItemStack item = graveItem.stack.copy();
						if (i >= slotInventory.getContainerSize()) {
							extraItems.add(item);
							continue;
						}
						slotInventory.setItem(i, item);
					}
				}
			}

			extraItems.removeIf(ItemStack::isEmpty);
			return extraItems;
		}

		@Override
		public void handleDropRules(DeathContext context) {
			// Traverse through groups
			for (Map<String, NonNullList<GraveItem>> group : this.inventory.values()) {

				// Traverse through slots
				for (NonNullList<GraveItem> slotItems : group.values()) {

					// Traverse through item stacks
					for (GraveItem graveItem : slotItems) {
						ItemStack item = graveItem.stack;

						if (item.isEmpty()) continue;

						DropRule dropRule = graveItem.dropRule;
						if (dropRule == DropRule.PUT_IN_GRAVE)
							dropRule = DropRuleEvent.EVENT.invoker().getDropRule(item, -1, context, true);

						graveItem.dropRule = dropRule;
					}
				}
			}
		}

		@Override
		public NonNullList<GraveItem> getAsGraveItemList() {
			NonNullList<GraveItem> allItems = NonNullList.create();
			for (Map<String, NonNullList<GraveItem>> slotMap : this.inventory.values()) {
				for (NonNullList<GraveItem> itemStacks : slotMap.values()) {
					allItems.addAll(itemStacks);
				}
			}

			return allItems;
		}

		@Override
		public CompatComponent<Map<String, Map<String, NonNullList<GraveItem>>>> filterInv(Predicate<DropRule> predicate) {
			Map<String, Map<String, NonNullList<GraveItem>>> filtered = new HashMap<>();

			for (Map.Entry<String, Map<String, NonNullList<GraveItem>>> group : this.inventory.entrySet()) {
				Map<String, NonNullList<GraveItem>> filteredGroup = new HashMap<>();

				for (Map.Entry<String, NonNullList<GraveItem>> slot : group.getValue().entrySet()) {
					NonNullList<GraveItem> filteredSlot = NonNullList.create();

					NonNullList<GraveItem> slotItems = slot.getValue();
					for (GraveItem graveItem : slotItems) {
						if (predicate.test(graveItem.dropRule)) {
							filteredSlot.add(graveItem);
						} else {
							filteredSlot.add(InventoryComponent.EMPTY_GRAVE_ITEM);
						}
					}
					filteredGroup.put(slot.getKey(), filteredSlot);
				}
				filtered.put(group.getKey(), filteredGroup);
			}
			return new TrinketsCompatComponent(filtered);
		}

		@Override
		public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
			for (Map<String, NonNullList<GraveItem>> group : this.inventory.values()) {
				for (NonNullList<GraveItem> slot : group.values()) {
					for (GraveItem graveItem : slot) {
						ItemStack stack = graveItem.stack;
						if (predicate.test(stack)) {
							stack.shrink(itemCount);

							return true;
						}
					}
				}
			}
			return false;
		}

		@Override
		public void clear() {
			for (Map<String, NonNullList<GraveItem>> slotMap : this.inventory.values()) {
				for (NonNullList<GraveItem> items : slotMap.values()) {
					Collections.fill(items, InventoryComponent.EMPTY_GRAVE_ITEM);
				}
			}
		}

		@Override
		public CompoundTag writeNbt(HolderLookup.Provider registryLookup) {
			CompoundTag nbt = new CompoundTag();

			// Traverse through groups
			for (Map.Entry<String, Map<String, NonNullList<GraveItem>>> group : this.inventory.entrySet()) {
				CompoundTag groupNbt = new CompoundTag();

				// Traverse through slots
				for (Map.Entry<String, NonNullList<GraveItem>> slot : group.getValue().entrySet()) {
					NonNullList<GraveItem> slotItems = slot.getValue();

					CompoundTag slotNbt = InventoryComponent.listToNbt(slotItems, graveItem -> {
						CompoundTag itemNbt = YigdNbtUtil.saveItemStack(graveItem.stack, registryLookup);
						itemNbt.putString("dropRule", graveItem.dropRule.name());

						return itemNbt;
					}, graveItem -> graveItem.stack.isEmpty(), "inventory", "size");

					groupNbt.put(slot.getKey(), slotNbt);
				}
				nbt.put(group.getKey(), groupNbt);
			}

			return nbt;
		}
	}
}
