package com.omarlawatey.blocks.pipe.upgrade.upgrades;

import com.omarlawatey.blocks.pipe.upgrade.PipeUpgrade;
import com.omarlawatey.blocks.pipe.upgrade.PipeUpgradeType;

public class DiamondUpgrade extends PipeUpgrade {

  public DiamondUpgrade(PipeUpgradeType type) {
    super(type);
  }

  @Override
  public long modifyExtractionAmount(long baseAmount) {
    return 64;
  }

  @Override
  public int modifyTransferCooldown(int baseCooldown) {
    return baseCooldown / 2;
  }
}