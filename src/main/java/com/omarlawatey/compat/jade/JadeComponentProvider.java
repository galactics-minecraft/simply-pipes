package com.omarlawatey.compat.jade;

import java.util.ArrayList;
import java.util.List;

import com.omarlawatey.SimplyPipes;
import com.omarlawatey.blocks.pipe.Pipe;
import com.omarlawatey.blocks.pipe.PipeBlockEntity;
import com.omarlawatey.blocks.pipe.PipeSideState;
import com.omarlawatey.blocks.pipe.upgrade.PipeUpgrade;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum JadeComponentProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
  INSTANCE;

  private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(SimplyPipes.MOD_ID, "pipe_info");

  @Override
  public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
    if (!(accessor.getBlock() instanceof Pipe)) {
      return;
    }

    BlockState state = accessor.getBlockState();
    CompoundTag serverData = accessor.getServerData();

    List<String> sources = new ArrayList<>();
    List<String> destinations = new ArrayList<>();

    for (Direction dir : Direction.values()) {
      PipeSideState sideState = state.getValue(Pipe.getPropertyFor(dir));
      String dirName = dir.getName().substring(0, 1).toUpperCase() + dir.getName().substring(1);

      if (sideState == PipeSideState.SOURCE) {
        sources.add(dirName);
      } else if (sideState == PipeSideState.DESTINATION) {
        destinations.add(dirName);
      }
    }

    if (!sources.isEmpty()) {
      tooltip.add(Component.translatable("simplypipes.jade.sources", String.join(", ", sources)));
    }

    if (!destinations.isEmpty()) {
      tooltip.add(Component.translatable("simplypipes.jade.destinations", String.join(", ", destinations)));
    }

    if (serverData.contains("LockedDirections")) {
      byte locked = serverData.getByte("LockedDirections");
      List<String> lockedDirs = new ArrayList<>();

      for (Direction dir : Direction.values()) {
        if ((locked & (1 << dir.ordinal())) != 0) {
          String dirName = dir.getName().substring(0, 1).toUpperCase() + dir.getName().substring(1);
          lockedDirs.add(dirName);
        }
      }

      if (!lockedDirs.isEmpty()) {
        tooltip.add(Component.translatable("simplypipes.jade.locked", String.join(", ", lockedDirs)));
      }
    }

    if (serverData.contains("Upgrades")) {
      CompoundTag upgradesTag = serverData.getCompound("Upgrades");

      boolean hasUpgrades = false;
      for (Direction dir : Direction.values()) {
        if (upgradesTag.contains(dir.getName())) {
          hasUpgrades = true;
          break;
        }
      }

      if (hasUpgrades) {
        tooltip.add(Component.translatable("simplypipes.jade.upgrades"));

        for (Direction dir : Direction.values()) {
          if (upgradesTag.contains(dir.getName())) {
            CompoundTag upgradeTag = upgradesTag.getCompound(dir.getName());
            String typeId = upgradeTag.getString("Type");
            String dirName = dir.getName().substring(0, 1).toUpperCase() + dir.getName().substring(1);
            String upgradeName = formatUpgradeName(typeId);

            tooltip.add(Component.literal("  " + dirName + ": " + upgradeName));
          }
        }
      }
    }
  }

  private String formatUpgradeName(String typeId) {
    if (typeId.endsWith("_upgrade")) {
      typeId = typeId.substring(0, typeId.length() - 8);
    }
    return typeId.substring(0, 1).toUpperCase() + typeId.substring(1);
  }

  @Override
  public void appendServerData(CompoundTag data, BlockAccessor accessor) {
    if (accessor.getBlockEntity() instanceof PipeBlockEntity pipeEntity) {
      byte locked = 0;
      for (Direction dir : Direction.values()) {
        if (pipeEntity.isDirectionLocked(dir)) {
          locked |= (1 << dir.ordinal());
        }
      }
      data.putByte("LockedDirections", locked);

      CompoundTag upgradesTag = new CompoundTag();
      for (Direction dir : Direction.values()) {
        PipeUpgrade upgrade = pipeEntity.getUpgradeSlot().getUpgrade(dir);
        if (upgrade != null) {
          upgradesTag.put(dir.getName(), upgrade.save());
        }
      }
      if (!upgradesTag.isEmpty()) {
        data.put("Upgrades", upgradesTag);
      }
    }
  }

  @Override
  public ResourceLocation getUid() {
    return UID;
  }
}
