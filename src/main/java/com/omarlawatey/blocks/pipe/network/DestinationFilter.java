package com.omarlawatey.blocks.pipe.network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an item filter for a destination connection.
 * Supports up to 9 filter slots.
 * Can be set to whitelist mode (only matching items pass) or
 * blacklist mode (everything except matching items passes).
 */
public class DestinationFilter {
  public static final int FILTER_SIZE = 9;

  private final NonNullList<ItemStack> filterItems;
  private boolean blacklist;

  public DestinationFilter() {
    this.filterItems = NonNullList.withSize(FILTER_SIZE, ItemStack.EMPTY);
    this.blacklist = false;
  }

  public DestinationFilter(Item item) {
    this();
    if (item != null) {
      this.filterItems.set(0, new ItemStack(item));
    }
  }

  public DestinationFilter(Item item, boolean blacklist) {
    this(item);
    this.blacklist = blacklist;
  }

  /**
   * @return true if no filter is set (all items allowed)
   */
  public boolean isEmpty() {
    for (ItemStack stack : filterItems) {
      if (!stack.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return true if this is a blacklist filter (blocks matching items)
   */
  public boolean isBlacklist() {
    return blacklist;
  }

  /**
   * Sets whether this filter is a blacklist
   */
  public void setBlacklist(boolean blacklist) {
    this.blacklist = blacklist;
  }

  /**
   * Toggles the blacklist mode
   */
  public void toggleBlacklist() {
    this.blacklist = !this.blacklist;
  }

  /**
   * @return the filter items list (9 slots)
   */
  public NonNullList<ItemStack> getFilterItems() {
    return filterItems;
  }

  /**
   * Gets the filter item at the specified slot
   */
  public ItemStack getFilterItem(int slot) {
    if (slot < 0 || slot >= FILTER_SIZE) {
      return ItemStack.EMPTY;
    }
    return filterItems.get(slot);
  }

  /**
   * Sets the filter item at the specified slot
   */
  public void setFilterItem(int slot, ItemStack stack) {
    if (slot >= 0 && slot < FILTER_SIZE) {
      filterItems.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }
  }

  /**
   * @return the first non-empty filter item, or null if filter is empty
   *         (For backwards compatibility and Jade display)
   */
  public Item getFilterItem() {
    for (ItemStack stack : filterItems) {
      if (!stack.isEmpty()) {
        return stack.getItem();
      }
    }
    return null;
  }

  /**
   * Sets the first filter slot (backwards compatibility)
   */
  public void setFilterItem(Item item) {
    filterItems.set(0, item != null ? new ItemStack(item) : ItemStack.EMPTY);
  }

  /**
   * @return set of all items in the filter
   */
  public Set<Item> getFilteredItems() {
    Set<Item> items = new HashSet<>();
    for (ItemStack stack : filterItems) {
      if (!stack.isEmpty()) {
        items.add(stack.getItem());
      }
    }
    return items;
  }

  /**
   * Clears the filter
   */
  public void clear() {
    for (int i = 0; i < FILTER_SIZE; i++) {
      filterItems.set(i, ItemStack.EMPTY);
    }
    this.blacklist = false;
  }

  /**
   * Tests if the given item variant passes through this filter.
   * In whitelist mode: only matching items pass.
   * In blacklist mode: everything except matching items passes.
   */
  public boolean test(ItemVariant variant) {
    if (isEmpty()) {
      return true; // No filter, everything passes
    }
    Set<Item> filteredItems = getFilteredItems();
    boolean matches = filteredItems.contains(variant.getItem());
    return blacklist ? !matches : matches;
  }

  /**
   * Creates a copy of this filter
   */
  public DestinationFilter copy() {
    DestinationFilter copy = new DestinationFilter();
    for (int i = 0; i < FILTER_SIZE; i++) {
      copy.filterItems.set(i, filterItems.get(i).copy());
    }
    copy.blacklist = this.blacklist;
    return copy;
  }

  public CompoundTag save() {
    CompoundTag tag = new CompoundTag();
    ListTag itemsTag = new ListTag();
    for (int i = 0; i < FILTER_SIZE; i++) {
      ItemStack stack = filterItems.get(i);
      if (!stack.isEmpty()) {
        CompoundTag slotTag = new CompoundTag();
        slotTag.putInt("Slot", i);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        slotTag.putString("Item", itemId.toString());
        itemsTag.add(slotTag);
      }
    }
    tag.put("FilterItems", itemsTag);
    tag.putBoolean("Blacklist", blacklist);
    return tag;
  }

  public static DestinationFilter load(CompoundTag tag) {
    DestinationFilter filter = new DestinationFilter();
    if (tag.contains("FilterItems")) {
      ListTag itemsTag = tag.getList("FilterItems", Tag.TAG_COMPOUND);
      for (int i = 0; i < itemsTag.size(); i++) {
        CompoundTag slotTag = itemsTag.getCompound(i);
        int slot = slotTag.getInt("Slot");
        String itemId = slotTag.getString("Item");
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc != null && slot >= 0 && slot < FILTER_SIZE) {
          Item item = BuiltInRegistries.ITEM.get(loc);
          if (item != Items.AIR) {
            filter.filterItems.set(slot, new ItemStack(item));
          }
        }
      }
      filter.setBlacklist(tag.getBoolean("Blacklist"));
    } else if (tag.contains("FilterItem")) {
      // Legacy single-item format
      String itemId = tag.getString("FilterItem");
      ResourceLocation loc = ResourceLocation.tryParse(itemId);
      if (loc != null) {
        Item item = BuiltInRegistries.ITEM.get(loc);
        if (item != Items.AIR) {
          filter.filterItems.set(0, new ItemStack(item));
          filter.setBlacklist(tag.getBoolean("Blacklist"));
        }
      }
    }
    return filter;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null || getClass() != obj.getClass())
      return false;
    DestinationFilter that = (DestinationFilter) obj;
    if (blacklist != that.blacklist)
      return false;
    for (int i = 0; i < FILTER_SIZE; i++) {
      if (!ItemStack.isSameItem(filterItems.get(i), that.filterItems.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = 0;
    for (ItemStack stack : filterItems) {
      if (!stack.isEmpty()) {
        result = 31 * result + stack.getItem().hashCode();
      }
    }
    result = 31 * result + (blacklist ? 1 : 0);
    return result;
  }
}
