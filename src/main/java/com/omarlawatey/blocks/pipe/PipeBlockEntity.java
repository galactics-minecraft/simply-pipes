package com.omarlawatey.blocks.pipe;

import com.omarlawatey.blocks.pipe.network.DestinationAccess;
import com.omarlawatey.blocks.pipe.network.NetworkScanner;
import com.omarlawatey.blocks.pipe.network.NetworkState;
import com.omarlawatey.blocks.pipe.transfer.ItemTransferHandler;
import com.omarlawatey.blocks.pipe.upgrade.PipeUpgrade;
import com.omarlawatey.blocks.pipe.upgrade.PipeUpgradeSlot;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.*;

public class PipeBlockEntity extends BlockEntity {
  private final Set<Direction> lockedDirections = EnumSet.noneOf(Direction.class);
  private final PipeUpgradeSlot upgradeSlot = new PipeUpgradeSlot(this);

  private static final Direction[] DIRECTIONS = Direction.values();
  private static final int BASE_TRANSFER_COOLDOWN = 8;

  private NetworkState networkState = null;
  private int transferTimer = 0;
  private List<DestinationAccess> cachedDestinations = null;
  private Map<DestinationAccess, Storage<ItemVariant>> cachedStorages = null;
  private List<DestinationAccess> validDestinations = null;

  private int cachedMinCooldown = BASE_TRANSFER_COOLDOWN;
  private boolean cooldownDirty = true;

  private final long[] cachedExtractionAmounts = new long[6];
  private final int[] cachedCooldowns = new int[6];
  private boolean upgradeCacheDirty = true;

  public PipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  public PipeBlockEntity(BlockPos pos, BlockState state) {
    this(com.omarlawatey.utils.ModBlockEntities.PIPE_BLOCK_ENTITY, pos, state);
  }

  public PipeUpgradeSlot getUpgradeSlot() {
    return upgradeSlot;
  }

  public int getTransferCooldown(Direction direction) {
    if (upgradeCacheDirty) {
      refreshUpgradeCache();
    }
    return cachedCooldowns[direction.ordinal()];
  }

  public long getExtractionAmount(Direction direction) {
    if (upgradeCacheDirty) {
      refreshUpgradeCache();
    }
    return cachedExtractionAmounts[direction.ordinal()];
  }

  private void refreshUpgradeCache() {
    for (Direction dir : DIRECTIONS) {
      int ordinal = dir.ordinal();
      PipeUpgrade upgrade = upgradeSlot.getUpgrade(dir);

      if (upgrade != null) {
        cachedExtractionAmounts[ordinal] = upgrade.modifyExtractionAmount(1);
        cachedCooldowns[ordinal] = upgrade.modifyTransferCooldown(BASE_TRANSFER_COOLDOWN);
      } else {
        cachedExtractionAmounts[ordinal] = 1;
        cachedCooldowns[ordinal] = BASE_TRANSFER_COOLDOWN;
      }
    }
    upgradeCacheDirty = false;
  }

  public boolean canTransfer(Direction direction, ItemVariant item) {
    PipeUpgrade upgrade = upgradeSlot.getUpgrade(direction);
    if (upgrade != null) {
      return upgrade.canTransfer(item);
    }
    return true;
  }

  public List<ItemStack> getAllUpgradeDrops() {
    List<ItemStack> drops = new ArrayList<>();
    Map<Direction, ItemStack> upgradeDrops = upgradeSlot.clearAll();
    drops.addAll(upgradeDrops.values());
    return drops;
  }

  public boolean isDirectionLocked(Direction direction) {
    return lockedDirections.contains(direction);
  }

  public void lockDirection(Direction direction) {
    lockedDirections.add(direction);
    invalidateNetworkCache();
    setChanged();
  }

  public void unlockDirection(Direction direction) {
    lockedDirections.remove(direction);
    invalidateNetworkCache();
    setChanged();
  }

  public NetworkState getNetworkState() {
    return networkState;
  }

  public void setNetworkState(NetworkState networkState) {
    this.networkState = networkState;
  }

  public void invalidateCache() {
    this.cachedDestinations = null;
    this.cachedStorages = null;
    this.validDestinations = null;
    this.networkState = null;
    this.cooldownDirty = true;
    this.upgradeCacheDirty = true;
  }

  public void invalidateUpgradeCache() {
    this.upgradeCacheDirty = true;
    this.cooldownDirty = true;
  }

  public void invalidateNetworkCache() {
    if (level == null) {
      invalidateCache();
      return;
    }
    NetworkScanner.invalidateNetworkCache(level, this.worldPosition);
  }

  public static void serverTick(Level level, BlockPos pos, BlockState state, PipeBlockEntity blockEntity) {
    blockEntity.transferTimer++;

    int cooldown = blockEntity.getMinSourceCooldown(state);
    if (blockEntity.transferTimer < cooldown) {
      return;
    }
    blockEntity.transferTimer = 0;

    if (blockEntity.hasSourceConnection(state)) {
      blockEntity.transferItems(level, pos, state);
    }
  }

  private int getMinSourceCooldown(BlockState state) {
    if (cooldownDirty) {
      cachedMinCooldown = BASE_TRANSFER_COOLDOWN;
      for (Direction dir : DIRECTIONS) {
        EnumProperty<PipeSideState> prop = Pipe.getPropertyFor(dir);
        if (state.getValue(prop) == PipeSideState.SOURCE) {
          int cooldown = getTransferCooldown(dir);
          cachedMinCooldown = Math.min(cachedMinCooldown, cooldown);
        }
      }
      cooldownDirty = false;
    }
    return cachedMinCooldown;
  }

  private boolean hasSourceConnection(BlockState state) {
    for (Direction dir : DIRECTIONS) {
      EnumProperty<PipeSideState> prop = Pipe.getPropertyFor(dir);
      if (state.getValue(prop) == PipeSideState.SOURCE) {
        return true;
      }
    }
    return false;
  }

  private void transferItems(Level level, BlockPos pos, BlockState state) {
    List<SourceInfo> sources = findSourceStorages(level, state);
    if (sources.isEmpty()) {
      return;
    }

    sources = deduplicateSources(sources);

    List<DestinationAccess> destinations = getDestinations(level, pos);
    if (destinations.isEmpty())
      return;

    if (cachedStorages == null || cachedStorages.size() != destinations.size()) {
      cachedStorages = new HashMap<>();
      validDestinations = new ArrayList<>();
      for (DestinationAccess dest : destinations) {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, dest.pos(), dest.accessDirection());
        if (storage != null) {
          cachedStorages.put(dest, storage);
          validDestinations.add(dest);
        }
      }
    }

    NetworkState netState = NetworkScanner.getOrCreateNetworkState(level, pos);

    for (SourceInfo source : sources) {
      long extractAmount = getExtractionAmount(source.direction);

      ItemTransferHandler.ItemExtractionResult extraction = ItemTransferHandler.extractItems(
          source.storage,
          extractAmount,
          item -> canTransfer(source.direction, item));

      if (extraction.isEmpty()) {
        continue;
      }

      long remaining = ItemTransferHandler.distributeEvenly(
          extraction.variant(),
          extraction.amount(),
          validDestinations,
          netState,
          cachedStorages::get);

      if (remaining > 0) {
        ItemTransferHandler.returnToSource(source.storage, extraction.variant(), remaining);
      } else {
        setChanged();
      }
    }
  }

  // Uses IdentityHashMap to handle double chests that share the same Storage
  // instance
  private List<SourceInfo> deduplicateSources(List<SourceInfo> sources) {
    if (sources.size() <= 1) {
      return sources;
    }

    Map<Storage<ItemVariant>, SourceInfo> uniqueSources = new IdentityHashMap<>();

    for (SourceInfo source : sources) {
      Storage<ItemVariant> storage = source.storage;

      if (!uniqueSources.containsKey(storage)) {
        uniqueSources.put(storage, source);
      } else {
        SourceInfo existing = uniqueSources.get(storage);
        long existingAmount = getExtractionAmount(existing.direction);
        long newAmount = getExtractionAmount(source.direction);

        if (newAmount > existingAmount) {
          uniqueSources.put(storage, source);
        }
      }
    }

    return new ArrayList<>(uniqueSources.values());
  }

  private record SourceInfo(Storage<ItemVariant> storage, Direction direction) {
  }

  private List<SourceInfo> findSourceStorages(Level level, BlockState state) {
    List<SourceInfo> sources = new ArrayList<>();
    for (Direction dir : DIRECTIONS) {
      EnumProperty<PipeSideState> prop = Pipe.getPropertyFor(dir);
      if (state.getValue(prop) == PipeSideState.SOURCE) {
        BlockPos neighborPos = this.worldPosition.relative(dir);
        Direction accessDir = dir.getOpposite();
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, neighborPos, accessDir);
        if (storage != null) {
          sources.add(new SourceInfo(storage, dir));
        }
      }
    }
    return sources;
  }

  private List<DestinationAccess> getDestinations(Level level, BlockPos pos) {
    if (cachedDestinations == null) {
      cachedDestinations = NetworkScanner.findDestinations(level, pos);
    }
    return cachedDestinations;
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    byte locked = 0;
    for (Direction dir : DIRECTIONS) {
      if (lockedDirections.contains(dir)) {
        locked |= (1 << dir.ordinal());
      }
    }
    tag.putByte("LockedDirections", locked);
    upgradeSlot.save(tag);
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    lockedDirections.clear();
    byte locked = tag.getByte("LockedDirections");
    for (Direction dir : DIRECTIONS) {
      if ((locked & (1 << dir.ordinal())) != 0) {
        lockedDirections.add(dir);
      }
    }
    upgradeSlot.load(tag);
    cachedDestinations = null;
    networkState = null;
  }

  @Override
  public Packet<ClientGamePacketListener> getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }

  @Override
  public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
    return saveWithoutMetadata(registries);
  }
}