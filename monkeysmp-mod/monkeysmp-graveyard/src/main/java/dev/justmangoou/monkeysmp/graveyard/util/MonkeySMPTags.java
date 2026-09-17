package dev.justmangoou.monkeysmp.graveyard.util;

import dev.justmangoou.monkeysmp.core.MonkeySMPCore;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

public interface MonkeySMPTags {
	TagKey<Block> REPLACE_SOFT_WHITELIST = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MonkeySMPCore.BASE_ID, "replace_soft_whitelist"));
	TagKey<Block> KEEP_STRICT_BLACKLIST = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MonkeySMPCore.BASE_ID, "keep_strict_blacklist"));
	TagKey<Block> REPLACE_GRAVE_BLACKLIST = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MonkeySMPCore.BASE_ID, "replace_grave_blacklist"));

	TagKey<Item> NATURAL_SOULBOUND = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MonkeySMPCore.BASE_ID, "natural_soulbound"));
	TagKey<Item> NATURAL_VANISHING = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MonkeySMPCore.BASE_ID, "natural_vanishing"));
	TagKey<Item> LOSS_IMMUNE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MonkeySMPCore.BASE_ID, "loss_immune"));
	TagKey<Item> GRAVE_INCOMPATIBLE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MonkeySMPCore.BASE_ID, "grave_incompatible"));  // For items that should be dropped instead of put into graves

	TagKey<Enchantment> SOULBOUND = TagKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(MonkeySMPCore.BASE_ID, "soulbound"));
	TagKey<Enchantment> VANISHING = TagKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(MonkeySMPCore.BASE_ID, "vanishing"));
	TagKey<Enchantment> DEATH_SIGHT = TagKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(MonkeySMPCore.BASE_ID, "death_sight"));
}
