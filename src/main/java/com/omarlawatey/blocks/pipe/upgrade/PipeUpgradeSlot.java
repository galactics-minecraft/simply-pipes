package com.omarlawatey.blocks.pipe.upgrade;

import com.omarlawatey.blocks.pipe.PipeBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class PipeUpgradeSlot {
  private static final Direction[] DIRECTIONS = Direction.values();

  private final Map<Direction, PipeUpgrade> upgrades = new EnumMap<>(Direction.class);
  private final PipeBlockEntity pipe;

  public PipeUpgradeSlot(PipeBlockEntity pipe) {
    this.pipe = pipe;
  }

  @Nullable
  public PipeUpgrade getUpgrade(Direction direction) {
    return upgrades.get(direction);
  }

  public boolean hasUpgrade(Direction direction) {
    return upgrades.containsKey(direction);
  }

  @Nullable
  public ItemStack setUpgrade(Direction direction, @Nullable PipeUpgrade upgrade) {
    PipeUpgrade previous = upgrades.get(direction);
    ItemStack dropStack = null;

    if (previous != null) {
      previous.onRemoved(pipe, direction);
      dropStack = previous.getDropStack();
    }

    if (upgrade != null) {
      upgrades.put(direction, upgrade);
      upgrade.onApplied(pipe, direction);
    } else {
      upgrades.remove(direction);
    }

    pipe.invalidateUpgradeCache();
    pipe.setChanged();
    return dropStack;
  }

  @Nullable
  public ItemStack removeUpgrade(Direction direction) {
    return setUpgrade(direction, null);
  }

  public Map<Direction, ItemStack> clearAll() {
    Map<Direction, ItemStack> drops = new EnumMap<>(Direction.class);
    for (Direction dir : DIRECTIONS) {
      ItemStack drop = removeUpgrade(dir);
      if (drop != null && !drop.isEmpty()) {
        drops.put(dir, drop);
      }
    }
    return drops;
  }

  public void save(CompoundTag tag) {
    CompoundTag upgradesTag = new CompoundTag();
    for (Map.Entry<Direction, PipeUpgrade> entry : upgrades.entrySet()) {
      upgradesTag.put(entry.getKey().getName(), entry.getValue().save());
    }
    tag.put("Upgrades", upgradesTag);
  }

  public void load(CompoundTag tag) {
    upgrades.clear();
    if (tag.contains("Upgrades")) {
      CompoundTag upgradesTag = tag.getCompound("Upgrades");
      for (Direction dir : DIRECTIONS) {
        String dirName = dir.getName();
        if (upgradesTag.contains(dirName)) {
          PipeUpgrade upgrade = PipeUpgrade.load(upgradesTag.getCompound(dirName));
          if (upgrade != null) {
            upgrades.put(dir, upgrade);
          }
        }
      }
    }
  }
}