package com.omarlawatey.utils;

import com.omarlawatey.SimplyPipes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final Item WRENCH = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(SimplyPipes.MOD_ID, "wrench"),
            new Item(new Item.Properties().stacksTo(1)));

    public static final Item IRON_UPGRADE = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(SimplyPipes.MOD_ID, "iron_upgrade"),
            new Item(new Item.Properties().stacksTo(64)));

    public static final Item GOLD_UPGRADE = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(SimplyPipes.MOD_ID, "gold_upgrade"),
            new Item(new Item.Properties().stacksTo(64)));

    public static final Item DIAMOND_UPGRADE = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(SimplyPipes.MOD_ID, "diamond_upgrade"),
            new Item(new Item.Properties().stacksTo(64)));

    public static final Item NETHERITE_UPGRADE = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(SimplyPipes.MOD_ID, "netherite_upgrade"),
            new Item(new Item.Properties().stacksTo(64).fireResistant()));

    public static void initialize() {
    }
}