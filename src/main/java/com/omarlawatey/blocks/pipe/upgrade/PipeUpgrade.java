package com.omarlawatey.blocks.pipe.upgrade;

import com.omarlawatey.blocks.pipe.PipeBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public abstract class PipeUpgrade {
  protected final PipeUpgradeType type;

  public PipeUpgrade(PipeUpgradeType type) {
    this.type = type;
  }

  public PipeUpgradeType getType() {
    return type;
  }

  public ItemStack getDropStack() {
    return new ItemStack(type.getItem());
  }

  public void onApplied(PipeBlockEntity pipe, Direction direction) {
  }

  public void onRemoved(PipeBlockEntity pipe, Direction direction) {
  }

  public int modifyTransferCooldown(int baseCooldown) {
    return baseCooldown;
  }

  public long modifyExtractionAmount(long baseAmount) {
    return baseAmount;
  }

  public boolean canTransfer(ItemVariant item) {
    return true;
  }

  public void saveAdditional(CompoundTag tag) {
  }

  public void loadAdditional(CompoundTag tag) {
  }

  public final CompoundTag save() {
    CompoundTag tag = new CompoundTag();
    tag.putString("Type", type.getId());
    saveAdditional(tag);
    return tag;
  }

  public static PipeUpgrade load(CompoundTag tag) {
    String typeId = tag.getString("Type");
    PipeUpgradeType type = PipeUpgradeType.fromId(typeId);
    if (type == null) {
      return null;
    }

    PipeUpgrade upgrade = type.createUpgrade();
    upgrade.loadAdditional(tag);
    return upgrade;
  }
}