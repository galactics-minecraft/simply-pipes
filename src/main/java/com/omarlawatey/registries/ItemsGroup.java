package com.omarlawatey.registries;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.ModifyEntries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemsGroup {
  public static Event<ModifyEntries> ModifyEntriesEvent;

  public static Item icon;
  public static final ResourceKey<CreativeModeTab> CUSTOM_ITEM_GROUP_KEY = ResourceKey.create(
      BuiltInRegistries.CREATIVE_MODE_TAB.key(),
      ResourceLocation.fromNamespaceAndPath(com.omarlawatey.SimplyPipes.MOD_ID, "item_group"));
  public static final CreativeModeTab CUSTOM_ITEM_GROUP = FabricItemGroup.builder()
      .icon(() -> icon != null ? new ItemStack(icon) : ItemStack.EMPTY)
      .title(Component.translatable("itemGroup.simply-pipes.item_group"))
      .build();

  public static void initialize(Item iconItemStack) {
    icon = iconItemStack;

    Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CUSTOM_ITEM_GROUP_KEY, CUSTOM_ITEM_GROUP);

    ModifyEntriesEvent = ItemGroupEvents.modifyEntriesEvent(CUSTOM_ITEM_GROUP_KEY);
  }

  public static void addItemsToItemGroup(Object... toAdd) {
    ModifyEntriesEvent.register(entries -> {
      for (Object obj : toAdd) {
        if (obj instanceof net.minecraft.world.item.Item item) {
          entries.accept(item);
        } else if (obj instanceof net.minecraft.world.level.block.Block block) {
          entries.accept(block);
        }
      }
    });
  }
}
