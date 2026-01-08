package com.omarlawatey.blocks.pipe.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Represents a destination storage with its position and the direction it
 * should be accessed from
 */
public record DestinationAccess(BlockPos pos, Direction accessDirection) {
}