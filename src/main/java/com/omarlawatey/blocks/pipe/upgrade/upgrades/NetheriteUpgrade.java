package com.omarlawatey.blocks.pipe.upgrade.upgrades;

import com.omarlawatey.blocks.pipe.upgrade.PipeUpgrade;
import com.omarlawatey.blocks.pipe.upgrade.PipeUpgradeType;

public class NetheriteUpgrade extends PipeUpgrade {

  public NetheriteUpgrade(PipeUpgradeType type) {
    super(type);
  }

  @Override
  public long modifyExtractionAmount(long baseAmount) {
    return 512;
  }

  @Override
  public int modifyTransferCooldown(int baseCooldown) {
    return baseCooldown / 4;
  }
}