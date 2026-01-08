package com.omarlawatey.utils;

import com.omarlawatey.blocks.pipe.upgrade.PipeUpgradeType;
import com.omarlawatey.blocks.pipe.upgrade.upgrades.DiamondUpgrade;
import com.omarlawatey.blocks.pipe.upgrade.upgrades.GoldUpgrade;
import com.omarlawatey.blocks.pipe.upgrade.upgrades.IronUpgrade;
import com.omarlawatey.blocks.pipe.upgrade.upgrades.NetheriteUpgrade;

/**
 * Registry for all pipe upgrades.
 */
public class ModUpgrades {

  public static final PipeUpgradeType IRON = PipeUpgradeType.register(
      "iron", ModItems.IRON_UPGRADE, IronUpgrade::new);

  public static final PipeUpgradeType GOLD = PipeUpgradeType.register(
      "gold", ModItems.GOLD_UPGRADE, GoldUpgrade::new);

  public static final PipeUpgradeType DIAMOND = PipeUpgradeType.register(
      "diamond", ModItems.DIAMOND_UPGRADE, DiamondUpgrade::new);

  public static final PipeUpgradeType NETHERITE = PipeUpgradeType.register(
      "netherite", ModItems.NETHERITE_UPGRADE, NetheriteUpgrade::new);

  public static void initialize() {
    // Upgrades are registered when the class is loaded
  }
}
