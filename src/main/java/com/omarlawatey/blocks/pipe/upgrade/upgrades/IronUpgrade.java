package com.omarlawatey.blocks.pipe.upgrade.upgrades;

import com.omarlawatey.blocks.pipe.upgrade.PipeUpgrade;
import com.omarlawatey.blocks.pipe.upgrade.PipeUpgradeType;

public class IronUpgrade extends PipeUpgrade {

  public IronUpgrade(PipeUpgradeType type) {
    super(type);
  }

  @Override
  public long modifyExtractionAmount(long baseAmount) {
    return 8;
  }
}