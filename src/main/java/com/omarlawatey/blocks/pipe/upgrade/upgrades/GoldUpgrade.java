package com.omarlawatey.blocks.pipe.upgrade.upgrades;

import com.omarlawatey.blocks.pipe.upgrade.PipeUpgrade;
import com.omarlawatey.blocks.pipe.upgrade.PipeUpgradeType;

public class GoldUpgrade extends PipeUpgrade {

  public GoldUpgrade(PipeUpgradeType type) {
    super(type);
  }

  @Override
  public long modifyExtractionAmount(long baseAmount) {
    return 16;
  }

}