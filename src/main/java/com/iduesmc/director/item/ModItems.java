package com.iduesmc.director.item;

import com.iduesmc.director.DirectorMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

public final class ModItems {
	public static final Item MEGAFONO = new MegaphoneItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));

	private ModItems() {
	}

	public static void register() {
		Registry.register(Registries.ITEM, DirectorMod.id("megafono"), MEGAFONO);
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(MEGAFONO));
	}
}
