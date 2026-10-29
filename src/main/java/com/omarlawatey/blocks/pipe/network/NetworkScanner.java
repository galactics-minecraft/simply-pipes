package com.omarlawatey.blocks.pipe.network;

import com.omarlawatey.blocks.pipe.Pipe;
import com.omarlawatey.blocks.pipe.PipeBlockEntity;
import com.omarlawatey.blocks.pipe.PipeSideState;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class NetworkScanner {
  private static final Direction[] DIRECTIONS = Direction.values();

  /**
   * Internal record to track position, state, and the current active filter
   * during BFS traversal
   */
  private record TraversalEntry(BlockPos pos, BlockState state, @Nullable DestinationFilter activeFilter) {
  }

  public static List<DestinationAccess> findDestinations(Level level, BlockPos startPos) {
    List<DestinationAccess> destinations = new ArrayList<>();
    Set<BlockPos> visited = new HashSet<>();
    Queue<TraversalEntry> toVisit = new ArrayDeque<>();

    BlockState startState = level.getBlockState(startPos);
    toVisit.add(new TraversalEntry(startPos, startState, null));
    visited.add(startPos);

    while (!toVisit.isEmpty()) {
      TraversalEntry current = toVisit.poll();

      if (!(current.state.getBlock() instanceof Pipe)) {
        continue;
      }

      // Get this pipe's block entity to check for filters
      BlockEntity currentBe = level.getBlockEntity(current.pos);
      PipeBlockEntity currentPipe = currentBe instanceof PipeBlockEntity ? (PipeBlockEntity) currentBe : null;

      for (Direction dir : DIRECTIONS) {
        EnumProperty<PipeSideState> prop = Pipe.getPropertyFor(dir);
        PipeSideState sideState = current.state.getValue(prop);

        if (sideState == PipeSideState.NONE) {
          continue;
        }

        BlockPos neighborPos = current.pos.relative(dir);

        if (visited.contains(neighborPos)) {
          continue;
        }

        BlockState neighborState = level.getBlockState(neighborPos);

        if (neighborState.getBlock() instanceof Pipe) {
          EnumProperty<PipeSideState> neighborProp = Pipe.getPropertyFor(dir.getOpposite());
          PipeSideState neighborSideState = neighborState.getValue(neighborProp);

          if (neighborSideState != PipeSideState.NONE) {
            visited.add(neighborPos);

            // Determine the filter to propagate
            // If this destination connection has a filter, use it;
            // otherwise, inherit the current active filter
            DestinationFilter filterToPropagate = current.activeFilter;
            if (sideState == PipeSideState.DESTINATION && currentPipe != null) {
              DestinationFilter connectionFilter = currentPipe.getDestinationFilter(dir);
              if (connectionFilter != null && !connectionFilter.isEmpty()) {
                filterToPropagate = connectionFilter;
              }
            }

            toVisit.add(new TraversalEntry(neighborPos, neighborState, filterToPropagate));
          }
        } else if (sideState == PipeSideState.DESTINATION) {
          Direction accessDir = dir.getOpposite();
          Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, neighborPos, accessDir);
          if (storage != null) {
            visited.add(neighborPos);

            // Check if this destination connection has a filter
            DestinationFilter filterForDestination = current.activeFilter;
            if (currentPipe != null) {
              DestinationFilter connectionFilter = currentPipe.getDestinationFilter(dir);
              if (connectionFilter != null && !connectionFilter.isEmpty()) {
                filterForDestination = connectionFilter;
              }
            }

            destinations.add(new DestinationAccess(neighborPos, accessDir, filterForDestination));
          }
        }
      }
    }

    return destinations;
  }

  private static Set<BlockPos> traverseNetwork(Level level, BlockPos startPos) {
    Set<BlockPos> visited = new HashSet<>();
    Queue<BlockPos> toVisit = new ArrayDeque<>();
    toVisit.add(startPos);
    visited.add(startPos);

    while (!toVisit.isEmpty()) {
      BlockPos currentPos = toVisit.poll();
      BlockState currentState = level.getBlockState(currentPos);

      if (!(currentState.getBlock() instanceof Pipe))
        continue;

      for (Direction dir : DIRECTIONS) {
        EnumProperty<PipeSideState> prop = Pipe.getPropertyFor(dir);
        PipeSideState sideState = currentState.getValue(prop);
        if (sideState == PipeSideState.NONE)
          continue;

        BlockPos neighborPos = currentPos.relative(dir);
        if (visited.contains(neighborPos))
          continue;

        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof Pipe) {
          EnumProperty<PipeSideState> neighborProp = Pipe.getPropertyFor(dir.getOpposite());
          if (neighborState.getValue(neighborProp) != PipeSideState.NONE) {
            visited.add(neighborPos);
            toVisit.add(neighborPos);
          }
        }
      }
    }
    return visited;
  }

  public static void invalidateNetworkCache(Level level, BlockPos startPos) {
    Set<BlockPos> network = traverseNetwork(level, startPos);
    for (BlockPos pos : network) {
      BlockEntity be = level.getBlockEntity(pos);
      if (be instanceof PipeBlockEntity pipeEntity) {
        pipeEntity.invalidateCache();
      }
    }
  }

  // Single-pass BFS that finds or creates a shared NetworkState for all connected
  // pipes
  public static NetworkState getOrCreateNetworkState(Level level, BlockPos startPos) {
    Set<BlockPos> visited = new HashSet<>();
    Queue<BlockPos> toVisit = new ArrayDeque<>();
    List<PipeBlockEntity> pipesInNetwork = new ArrayList<>();

    toVisit.add(startPos);
    visited.add(startPos);
    NetworkState foundState = null;

    while (!toVisit.isEmpty()) {
      BlockPos currentPos = toVisit.poll();
      BlockEntity be = level.getBlockEntity(currentPos);

      if (be instanceof PipeBlockEntity pipeEntity) {
        pipesInNetwork.add(pipeEntity);
        if (foundState == null && pipeEntity.getNetworkState() != null) {
          foundState = pipeEntity.getNetworkState();
        }
      }

      BlockState currentState = level.getBlockState(currentPos);
      if (!(currentState.getBlock() instanceof Pipe)) {
        continue;
      }

      for (Direction dir : DIRECTIONS) {
        EnumProperty<PipeSideState> prop = Pipe.getPropertyFor(dir);
        PipeSideState sideState = currentState.getValue(prop);

        if (sideState == PipeSideState.NONE) {
          continue;
        }

        BlockPos neighborPos = currentPos.relative(dir);
        if (visited.contains(neighborPos)) {
          continue;
        }

        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof Pipe) {
          EnumProperty<PipeSideState> neighborProp = Pipe.getPropertyFor(dir.getOpposite());
          PipeSideState neighborSideState = neighborState.getValue(neighborProp);

          if (neighborSideState != PipeSideState.NONE) {
            visited.add(neighborPos);
            toVisit.add(neighborPos);
          }
        }
      }
    }

    if (foundState == null) {
      foundState = new NetworkState();
    }

    for (PipeBlockEntity pipe : pipesInNetwork) {
      pipe.setNetworkState(foundState);
    }

    return foundState;
  }
}