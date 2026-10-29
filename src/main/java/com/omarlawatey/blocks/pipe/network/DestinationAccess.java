package com.omarlawatey.blocks.pipe.network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a destination storage with its position, the direction it
 * should be accessed from, and an optional filter that restricts which items
 * can be transferred to this destination.
 */
public record DestinationAccess(BlockPos pos, Direction accessDirection, @Nullable DestinationFilter filter) {

  /**
   * Creates a DestinationAccess with no filter (accepts all items)
   */
  public DestinationAccess(BlockPos pos, Direction accessDirection) {
    this(pos, accessDirection, null);
  }

  /**
   * Tests if the given item variant can be transferred to this destination
   * based on the filter. Returns true if no filter is set or if the item matches.
   */
  public boolean acceptsItem(ItemVariant variant) {
    if (filter == null || filter.isEmpty()) {
      return true;
    }
    return filter.test(variant);
  }

  /**
   * Creates a copy of this DestinationAccess with the given filter
   */
  public DestinationAccess withFilter(@Nullable DestinationFilter newFilter) {
    return new DestinationAccess(pos, accessDirection, newFilter);
  }
}