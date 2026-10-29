package com.omarlawatey.blocks.pipe.filter;

import com.omarlawatey.blocks.pipe.PipeBlockEntity;
import com.omarlawatey.blocks.pipe.network.DestinationFilter;
import com.omarlawatey.utils.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Menu for the destination filter GUI.
 * Allows players to configure which items can pass through a destination
 * connection.
 */
public class DestinationFilterMenu extends AbstractContainerMenu {
  private final PipeBlockEntity pipeEntity;
  private final Direction filterDirection;
  private final DestinationFilter filter;

  /**
   * Data record for sending menu opening data to client
   */
  public record FilterData(BlockPos pos, Direction direction) {
    public static final StreamCodec<RegistryFriendlyByteBuf, FilterData> STREAM_CODEC = StreamCodec.of(
        (buf, data) -> {
          buf.writeBlockPos(data.pos);
          buf.writeByte(data.direction.get3DDataValue());
        },
        buf -> new FilterData(buf.readBlockPos(), Direction.from3DDataValue(buf.readByte())));
  }

  // Client constructor (called by screen handler type)
  public DestinationFilterMenu(int syncId, Inventory playerInventory, FilterData data) {
    this(syncId, playerInventory, data.pos(), data.direction());
  }

  // Server constructor
  public DestinationFilterMenu(int syncId, Inventory playerInventory, BlockPos pos, Direction direction) {
    super(ModMenus.DESTINATION_FILTER_MENU, syncId);

    Level level = playerInventory.player.level();
    BlockEntity be = level.getBlockEntity(pos);

    if (be instanceof PipeBlockEntity pipe) {
      this.pipeEntity = pipe;
      this.filterDirection = direction;

      // Get or create filter for this direction
      DestinationFilter existing = pipe.getDestinationFilter(direction);
      if (existing == null) {
        existing = new DestinationFilter();
        pipe.setDestinationFilter(direction, existing);
      }
      this.filter = existing;
    } else {
      this.pipeEntity = null;
      this.filterDirection = direction;
      this.filter = new DestinationFilter();
    }

    // Add filter slots (3x3 grid)
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        int slotIndex = row * 3 + col;
        this.addSlot(new FilterSlot(filter, slotIndex, 62 + col * 18, 17 + row * 18));
      }
    }

    // Add player inventory slots
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
      }
    }

    // Add hotbar slots
    for (int col = 0; col < 9; col++) {
      this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }

    // Add data slot for blacklist state synchronization
    this.addDataSlot(new DataSlot() {
      @Override
      public int get() {
        return filter.isBlacklist() ? 1 : 0;
      }

      @Override
      public void set(int value) {
        filter.setBlacklist(value != 0);
      }
    });
  }

  public DestinationFilter getFilter() {
    return filter;
  }

  public Direction getFilterDirection() {
    return filterDirection;
  }

  public boolean isBlacklist() {
    return filter.isBlacklist();
  }

  public void toggleBlacklist() {
    filter.toggleBlacklist();
    // Mark as changed and sync
    if (pipeEntity != null) {
      Level level = pipeEntity.getLevel();
      if (level != null && !level.isClientSide()) {
        pipeEntity.setChanged();
        pipeEntity.syncToClient();
      }
    }
  }

  @Override
  public boolean clickMenuButton(Player player, int id) {
    if (id == 0) {
      // Toggle blacklist/whitelist mode
      toggleBlacklist();
      return true;
    }
    return false;
  }

  @Override
  public void clicked(int slotId, int button, ClickType clickType, Player player) {
    // Handle filter slots specially - just copy the item type, don't move items
    if (slotId >= 0 && slotId < DestinationFilter.FILTER_SIZE) {
      Slot slot = this.slots.get(slotId);
      ItemStack carried = getCarried();

      if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) {
        if (button == 1) {
          // Right-click = clear slot
          filter.setFilterItem(slotId, ItemStack.EMPTY);
        } else if (!carried.isEmpty()) {
          // Left-click with item = set filter
          filter.setFilterItem(slotId, carried.copyWithCount(1));
        } else if (!slot.getItem().isEmpty()) {
          // Left-click on filled slot with empty hand = clear
          filter.setFilterItem(slotId, ItemStack.EMPTY);
        }
      }

      // Mark as changed and sync (only on server side)
      if (pipeEntity != null && !player.level().isClientSide()) {
        pipeEntity.setChanged();
        pipeEntity.syncToClient();
      }
      return;
    }

    super.clicked(slotId, button, clickType, player);
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    // For filter slots: clicking on player inventory copies item to first empty
    // filter slot
    if (index >= DestinationFilter.FILTER_SIZE) {
      Slot slot = this.slots.get(index);
      if (slot != null && slot.hasItem()) {
        ItemStack stack = slot.getItem();
        // Find first empty filter slot
        for (int i = 0; i < DestinationFilter.FILTER_SIZE; i++) {
          if (filter.getFilterItem(i).isEmpty()) {
            filter.setFilterItem(i, stack.copyWithCount(1));
            // Mark as changed and sync (only on server side)
            if (pipeEntity != null && !player.level().isClientSide()) {
              pipeEntity.setChanged();
              pipeEntity.syncToClient();
            }
            break;
          }
        }
      }
    }
    return ItemStack.EMPTY;
  }

  @Override
  public boolean stillValid(Player player) {
    if (pipeEntity == null)
      return false;
    BlockPos pos = pipeEntity.getBlockPos();
    return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
  }

  @Override
  public void removed(Player player) {
    super.removed(player);

    // Save filter state when menu is closed
    if (pipeEntity != null && !player.level().isClientSide()) {
      pipeEntity.setDestinationFilter(filterDirection, filter);
      pipeEntity.setChanged();
      pipeEntity.syncToClient();
    }
  }

  /**
   * Custom slot for filter items - doesn't actually hold items, just displays
   * them. Uses a ghost slot approach.
   */
  private static class FilterSlot extends Slot {
    private final DestinationFilter filter;
    private final int filterIndex;

    public FilterSlot(DestinationFilter filter, int index, int x, int y) {
      super(new SimpleContainer(1), 0, x, y);
      this.filter = filter;
      this.filterIndex = index;
    }

    @Override
    public ItemStack getItem() {
      return filter.getFilterItem(filterIndex);
    }

    @Override
    public void set(ItemStack stack) {
      filter.setFilterItem(filterIndex, stack);
    }

    @Override
    public void setChanged() {
      // No-op, we handle changes manually
    }

    @Override
    public ItemStack remove(int amount) {
      ItemStack current = getItem().copy();
      filter.setFilterItem(filterIndex, ItemStack.EMPTY);
      return current;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
      return false; // Don't allow normal placement
    }

    @Override
    public boolean mayPickup(Player player) {
      return false; // Don't allow normal pickup
    }

    @Override
    public int getMaxStackSize() {
      return 1;
    }

    @Override
    public boolean hasItem() {
      return !getItem().isEmpty();
    }

    @Override
    public boolean isFake() {
      return true; // This is a ghost/fake slot
    }
  }
}
