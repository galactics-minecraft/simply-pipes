package com.omarlawatey.registries;

import com.omarlawatey.utils.ModBlockEntities;
import com.omarlawatey.utils.ModBlocks;
import com.omarlawatey.utils.ModItems;
import com.omarlawatey.utils.ModUpgrades;

public class ItemsInitializer {
  public static void initializeAll() {
    ModBlocks.initialize();
    ModBlockEntities.initialize();
    ModItems.initialize();
    ModUpgrades.initialize();

    ItemsGroup.initialize(ModBlocks.PIPE.asItem());
    ItemsGroup.addItemsToItemGroup(
        ModItems.WRENCH,
        ModBlocks.PIPE,
        ModItems.IRON_UPGRADE,
        ModItems.GOLD_UPGRADE,
        ModItems.DIAMOND_UPGRADE,
        ModItems.NETHERITE_UPGRADE);
  }
}