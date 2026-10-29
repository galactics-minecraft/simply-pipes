package com.omarlawatey.blocks.pipe;

import com.omarlawatey.blocks.pipe.filter.DestinationFilterMenu;
import com.omarlawatey.blocks.pipe.upgrade.PipeUpgrade;
import com.omarlawatey.blocks.pipe.upgrade.PipeUpgradeType;
import com.omarlawatey.utils.ModItems;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;

public class Pipe extends Block implements EntityBlock, SimpleWaterloggedBlock {

  private static final VoxelShape[] SHAPE_INDEX = new VoxelShape[64];
  private static final Direction[] DIRECTIONS = Direction.values();

  public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
  public static final EnumProperty<PipeSideState> NORTH = EnumProperty.create("north", PipeSideState.class);
  public static final EnumProperty<PipeSideState> SOUTH = EnumProperty.create("south", PipeSideState.class);
  public static final EnumProperty<PipeSideState> EAST = EnumProperty.create("east", PipeSideState.class);
  public static final EnumProperty<PipeSideState> WEST = EnumProperty.create("west", PipeSideState.class);
  public static final EnumProperty<PipeSideState> UP = EnumProperty.create("up", PipeSideState.class);
  public static final EnumProperty<PipeSideState> DOWN = EnumProperty.create("down", PipeSideState.class);

  private static final double PIPE_MIN = 5.0;
  private static final double PIPE_MAX = 11.0;

  private static final VoxelShape CORE_SHAPE = Block.box(PIPE_MIN, PIPE_MIN, PIPE_MIN, PIPE_MAX, PIPE_MAX, PIPE_MAX);
  private static final VoxelShape NORTH_SHAPE = Block.box(PIPE_MIN, PIPE_MIN, 0, PIPE_MAX, PIPE_MAX, PIPE_MIN);
  private static final VoxelShape SOUTH_SHAPE = Block.box(PIPE_MIN, PIPE_MIN, PIPE_MAX, PIPE_MAX, PIPE_MAX, 16);
  private static final VoxelShape WEST_SHAPE = Block.box(0, PIPE_MIN, PIPE_MIN, PIPE_MIN, PIPE_MAX, PIPE_MAX);
  private static final VoxelShape EAST_SHAPE = Block.box(PIPE_MAX, PIPE_MIN, PIPE_MIN, 16, PIPE_MAX, PIPE_MAX);
  private static final VoxelShape UP_SHAPE = Block.box(PIPE_MIN, PIPE_MAX, PIPE_MIN, PIPE_MAX, 16, PIPE_MAX);
  private static final VoxelShape DOWN_SHAPE = Block.box(PIPE_MIN, 0, PIPE_MIN, PIPE_MAX, PIPE_MIN, PIPE_MAX);

  private static final AABB CORE_AABB = CORE_SHAPE.bounds();

  private static final EnumMap<Direction, VoxelShape> SHAPE_CACHE = new EnumMap<>(Direction.class);
  private static final EnumMap<Direction, EnumProperty<PipeSideState>> PROPERTY_CACHE = new EnumMap<>(Direction.class);
  private static final EnumMap<Direction, AABB> CONNECTION_AABB_CACHE = new EnumMap<>(Direction.class);

  private static final double PIPE_MIN_NORM = PIPE_MIN / 16.0;
  private static final double PIPE_MAX_NORM = PIPE_MAX / 16.0;

  static {
    SHAPE_CACHE.put(Direction.NORTH, NORTH_SHAPE);
    SHAPE_CACHE.put(Direction.SOUTH, SOUTH_SHAPE);
    SHAPE_CACHE.put(Direction.EAST, EAST_SHAPE);
    SHAPE_CACHE.put(Direction.WEST, WEST_SHAPE);
    SHAPE_CACHE.put(Direction.UP, UP_SHAPE);
    SHAPE_CACHE.put(Direction.DOWN, DOWN_SHAPE);

    PROPERTY_CACHE.put(Direction.NORTH, NORTH);
    PROPERTY_CACHE.put(Direction.SOUTH, SOUTH);
    PROPERTY_CACHE.put(Direction.EAST, EAST);
    PROPERTY_CACHE.put(Direction.WEST, WEST);
    PROPERTY_CACHE.put(Direction.UP, UP);
    PROPERTY_CACHE.put(Direction.DOWN, DOWN);

    CONNECTION_AABB_CACHE.put(Direction.NORTH,
        new AABB(PIPE_MIN_NORM, PIPE_MIN_NORM, 0, PIPE_MAX_NORM, PIPE_MAX_NORM, PIPE_MIN_NORM));
    CONNECTION_AABB_CACHE.put(Direction.SOUTH,
        new AABB(PIPE_MIN_NORM, PIPE_MIN_NORM, PIPE_MAX_NORM, PIPE_MAX_NORM, PIPE_MAX_NORM, 1));
    CONNECTION_AABB_CACHE.put(Direction.WEST,
        new AABB(0, PIPE_MIN_NORM, PIPE_MIN_NORM, PIPE_MIN_NORM, PIPE_MAX_NORM, PIPE_MAX_NORM));
    CONNECTION_AABB_CACHE.put(Direction.EAST,
        new AABB(PIPE_MAX_NORM, PIPE_MIN_NORM, PIPE_MIN_NORM, 1, PIPE_MAX_NORM, PIPE_MAX_NORM));
    CONNECTION_AABB_CACHE.put(Direction.UP,
        new AABB(PIPE_MIN_NORM, PIPE_MAX_NORM, PIPE_MIN_NORM, PIPE_MAX_NORM, 1, PIPE_MAX_NORM));
    CONNECTION_AABB_CACHE.put(Direction.DOWN,
        new AABB(PIPE_MIN_NORM, 0, PIPE_MIN_NORM, PIPE_MAX_NORM, PIPE_MIN_NORM, PIPE_MAX_NORM));

    for (int i = 0; i < 64; i++) {
      VoxelShape shape = CORE_SHAPE;
      if ((i & 1) != 0)
        shape = Shapes.or(shape, NORTH_SHAPE);
      if ((i & 2) != 0)
        shape = Shapes.or(shape, SOUTH_SHAPE);
      if ((i & 4) != 0)
        shape = Shapes.or(shape, EAST_SHAPE);
      if ((i & 8) != 0)
        shape = Shapes.or(shape, WEST_SHAPE);
      if ((i & 16) != 0)
        shape = Shapes.or(shape, UP_SHAPE);
      if ((i & 32) != 0)
        shape = Shapes.or(shape, DOWN_SHAPE);
      SHAPE_INDEX[i] = shape.optimize();
    }
  }

  public Pipe(Properties properties) {
    super(properties);
    this.registerDefaultState(this.stateDefinition.any()
        .setValue(WATERLOGGED, false)
        .setValue(NORTH, PipeSideState.NONE)
        .setValue(SOUTH, PipeSideState.NONE)
        .setValue(EAST, PipeSideState.NONE)
        .setValue(WEST, PipeSideState.NONE)
        .setValue(UP, PipeSideState.NONE)
        .setValue(DOWN, PipeSideState.NONE));
  }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    return SHAPE_INDEX[getShapeIndex(state)];
  }

  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    return SHAPE_INDEX[getShapeIndex(state)];
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(WATERLOGGED, NORTH, SOUTH, EAST, WEST, UP, DOWN);
  }

  @Override
  public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
      LevelAccessor world, BlockPos pos, BlockPos neighborPos) {

    if (state.getValue(WATERLOGGED))
      world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));

    BlockEntity be = world.getBlockEntity(pos);
    Block neighborBlock = neighborState.getBlock();

    boolean neighborCanConnect = canConnectTo(world, neighborPos, dir.getOpposite());
    PipeSideState currentSideState = state.getValue(getPropertyFor(dir));

    if (!neighborCanConnect) {
      if (be instanceof PipeBlockEntity pipeEntity) {
        if (currentSideState == PipeSideState.SOURCE) {
          dropUpgradeAt(world, pos, pipeEntity, dir);
        }
        // Clear destination filter when connection is removed
        if (currentSideState == PipeSideState.DESTINATION) {
          pipeEntity.clearDestinationFilter(dir);
        }

        if (pipeEntity.isDirectionLocked(dir)) {
          pipeEntity.unlockDirection(dir);
        }
        pipeEntity.invalidateNetworkCache();
      }
      return state.setValue(getPropertyFor(dir), PipeSideState.NONE);
    }

    if (be instanceof PipeBlockEntity pipeEntity && pipeEntity.isDirectionLocked(dir)) {
      return state;
    }

    if (neighborBlock instanceof Pipe) {
      Direction oppositeDir = dir.getOpposite();
      EnumProperty<PipeSideState> neighborProp = getPropertyFor(oppositeDir);
      PipeSideState neighborConnection = neighborState.getValue(neighborProp);

      if (neighborConnection == PipeSideState.NONE) {
        if (be instanceof PipeBlockEntity pipeEntity) {
          if (currentSideState == PipeSideState.SOURCE) {
            dropUpgradeAt(world, pos, pipeEntity, dir);
          }
          // Clear destination filter when connection is removed
          if (currentSideState == PipeSideState.DESTINATION) {
            pipeEntity.clearDestinationFilter(dir);
          }
          pipeEntity.invalidateNetworkCache();
        }
        return state.setValue(getPropertyFor(dir), PipeSideState.NONE);
      }

      PipeSideState currentState = state.getValue(getPropertyFor(dir));
      if (currentState == PipeSideState.NONE && neighborConnection != PipeSideState.NONE) {
        if (be instanceof PipeBlockEntity pipeEntity) {
          pipeEntity.invalidateNetworkCache();
        }
        return state.setValue(getPropertyFor(dir), PipeSideState.DESTINATION);
      }

      return state;
    }

    // Preserve SOURCE state when storage neighbors update (e.g., furnaces changing
    // state)
    PipeSideState currentState = state.getValue(getPropertyFor(dir));
    if (currentState == PipeSideState.NONE) {
      if (be instanceof PipeBlockEntity pipeEntity) {
        pipeEntity.invalidateNetworkCache();
      }
      return state.setValue(getPropertyFor(dir), PipeSideState.DESTINATION);
    }

    return state;
  }

  // Safely drops upgrade, handling both Level and LevelAccessor contexts
  private void dropUpgradeAt(LevelAccessor world, BlockPos pos, PipeBlockEntity pipeEntity, Direction dir) {
    ItemStack upgradeDrop = pipeEntity.getUpgradeSlot().removeUpgrade(dir);
    if (upgradeDrop != null && !upgradeDrop.isEmpty()) {
      if (world instanceof Level level) {
        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, upgradeDrop);
      }
    }
  }

  @Override
  public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
    if (!state.is(newState.getBlock())) {
      BlockEntity be = level.getBlockEntity(pos);
      if (be instanceof PipeBlockEntity pipeEntity) {
        for (ItemStack drop : pipeEntity.getAllUpgradeDrops()) {
          Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
        }
        pipeEntity.invalidateNetworkCache();
      }
    }
    super.onRemove(state, level, pos, newState, movedByPiston);
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext ctx) {
    LevelAccessor world = ctx.getLevel();
    BlockPos pos = ctx.getClickedPos();
    BlockState state = this.defaultBlockState();

    FluidState fluidState = world.getFluidState(pos);
    state = state.setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);

    for (Direction dir : DIRECTIONS) {
      BlockPos neighborPos = pos.relative(dir);
      boolean connect = canConnectTo(world, neighborPos, dir.getOpposite());
      PipeSideState pipeState = connect ? PipeSideState.DESTINATION : PipeSideState.NONE;
      state = state.setValue(getPropertyFor(dir), pipeState);
    }
    return state;
  }

  @Override
  public FluidState getFluidState(BlockState state) {
    return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
  }

  @Override
  protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
      Player player, InteractionHand hand, BlockHitResult hit) {
    if (world.isClientSide())
      return ItemInteractionResult.SUCCESS;

    if (stack.is(ModItems.WRENCH)) {
      InteractionResult result = handleWrenchInteraction(state, world, pos, player, hit);
      return result == InteractionResult.CONSUME ? ItemInteractionResult.CONSUME
          : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    PipeUpgradeType upgradeType = PipeUpgradeType.fromItem(stack.getItem());
    if (upgradeType != null) {
      InteractionResult result = handleUpgradeInteraction(state, world, pos, player, hit, upgradeType, stack);
      return result == InteractionResult.CONSUME ? ItemInteractionResult.CONSUME
          : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
  }

  @Override
  public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
      Player player, BlockHitResult hit) {
    if (world.isClientSide())
      return InteractionResult.SUCCESS;

    // Empty hand right-click on destination = open filter GUI
    // Empty hand + sneak = clear filter
    if (player.isShiftKeyDown()) {
      return handleClearFilterInteraction(state, world, pos, player, hit);
    }

    return handleFilterInteraction(state, world, pos, player, hit);
  }

  private InteractionResult handleFilterInteraction(BlockState state, Level world, BlockPos pos,
      Player player, BlockHitResult hit) {
    Direction closestDirection = findClosestConnection(state, hit);
    if (closestDirection == null)
      return InteractionResult.PASS;

    EnumProperty<PipeSideState> prop = getPropertyFor(closestDirection);
    PipeSideState sideState = state.getValue(prop);

    // Only allow filters on destination connections (not source, not none)
    if (sideState != PipeSideState.DESTINATION) {
      return InteractionResult.PASS;
    }

    BlockEntity be = world.getBlockEntity(pos);
    if (!(be instanceof PipeBlockEntity)) {
      return InteractionResult.PASS;
    }

    // Open filter GUI
    final Direction filterDir = closestDirection;
    player.openMenu(new ExtendedScreenHandlerFactory<DestinationFilterMenu.FilterData>() {
      @Override
      public DestinationFilterMenu.FilterData getScreenOpeningData(ServerPlayer serverPlayer) {
        return new DestinationFilterMenu.FilterData(pos, filterDir);
      }

      @Override
      public Component getDisplayName() {
        return Component.translatable("simplypipes.gui.filter.title");
      }

      @Override
      public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player p) {
        return new DestinationFilterMenu(syncId, inventory, pos, filterDir);
      }
    });

    return InteractionResult.CONSUME;
  }

  private InteractionResult handleClearFilterInteraction(BlockState state, Level world, BlockPos pos,
      Player player, BlockHitResult hit) {
    Direction closestDirection = findClosestConnection(state, hit);
    if (closestDirection == null)
      return InteractionResult.PASS;

    EnumProperty<PipeSideState> prop = getPropertyFor(closestDirection);
    PipeSideState sideState = state.getValue(prop);

    // Only clear filters on destination connections
    if (sideState != PipeSideState.DESTINATION) {
      return InteractionResult.PASS;
    }

    BlockEntity be = world.getBlockEntity(pos);
    if (!(be instanceof PipeBlockEntity pipeEntity)) {
      return InteractionResult.PASS;
    }

    // Only consume interaction if there was a filter to clear
    if (pipeEntity.hasDestinationFilter(closestDirection)) {
      pipeEntity.clearDestinationFilter(closestDirection);
      return InteractionResult.CONSUME;
    }

    return InteractionResult.PASS;
  }

  private InteractionResult handleWrenchInteraction(BlockState state, Level world, BlockPos pos,
      Player player, BlockHitResult hit) {
    Direction closestDirection = findClosestConnection(state, hit);
    if (closestDirection == null)
      return InteractionResult.FAIL;

    BlockPos neighborPos = pos.relative(closestDirection);

    if (!canConnectTo(world, neighborPos, closestDirection.getOpposite()))
      return InteractionResult.FAIL;

    EnumProperty<PipeSideState> prop = getPropertyFor(closestDirection);
    PipeSideState current = state.getValue(prop);

    BlockState neighborState = world.getBlockState(neighborPos);
    Block neighborBlock = neighborState.getBlock();
    boolean isNeighborPipe = neighborBlock instanceof Pipe;

    PipeSideState next = nextState(current, isNeighborPipe);

    BlockEntity be = world.getBlockEntity(pos);
    if (be instanceof PipeBlockEntity pipeEntity) {
      if (current == PipeSideState.SOURCE && next != PipeSideState.SOURCE) {
        ItemStack upgradeDrop = pipeEntity.getUpgradeSlot().removeUpgrade(closestDirection);
        if (upgradeDrop != null && !upgradeDrop.isEmpty()) {
          Containers.dropItemStack(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, upgradeDrop);
        }
      }

      // Clear destination filter when changing away from DESTINATION
      if (current == PipeSideState.DESTINATION && next != PipeSideState.DESTINATION) {
        pipeEntity.clearDestinationFilter(closestDirection);
      }

      if (next == PipeSideState.NONE)
        pipeEntity.lockDirection(closestDirection);
      else if (current == PipeSideState.NONE)
        pipeEntity.unlockDirection(closestDirection);
    }

    world.setBlock(pos, state.setValue(prop, next), Block.UPDATE_ALL);

    if (isNeighborPipe) {
      Direction oppositeDirection = closestDirection.getOpposite();
      EnumProperty<PipeSideState> neighborProp = getPropertyFor(oppositeDirection);

      BlockEntity neighborBe = world.getBlockEntity(neighborPos);
      if (neighborBe instanceof PipeBlockEntity neighborPipeEntity) {
        if (next == PipeSideState.NONE)
          neighborPipeEntity.lockDirection(oppositeDirection);
        else {
          BlockState currentNeighborState = world.getBlockState(neighborPos);
          PipeSideState currentNeighborConnection = currentNeighborState.getValue(neighborProp);
          if (currentNeighborConnection == PipeSideState.NONE)
            neighborPipeEntity.unlockDirection(oppositeDirection);
        }
      }

      world.setBlock(neighborPos, neighborState.setValue(neighborProp, next), Block.UPDATE_ALL);
    }

    return InteractionResult.CONSUME;
  }

  private InteractionResult handleUpgradeInteraction(BlockState state, Level world, BlockPos pos,
      Player player, BlockHitResult hit, PipeUpgradeType upgradeType, ItemStack heldItem) {

    Direction closestDirection = findClosestConnection(state, hit);
    if (closestDirection == null)
      return InteractionResult.FAIL;

    EnumProperty<PipeSideState> prop = getPropertyFor(closestDirection);
    PipeSideState sideState = state.getValue(prop);
    if (sideState != PipeSideState.SOURCE) {
      return InteractionResult.FAIL;
    }

    BlockEntity be = world.getBlockEntity(pos);
    if (!(be instanceof PipeBlockEntity pipeEntity)) {
      return InteractionResult.FAIL;
    }

    PipeUpgrade existingUpgrade = pipeEntity.getUpgradeSlot().getUpgrade(closestDirection);
    if (existingUpgrade != null && existingUpgrade.getType() == upgradeType) {
      return InteractionResult.PASS;
    }

    PipeUpgrade newUpgrade = upgradeType.createUpgrade();
    ItemStack dropStack = pipeEntity.getUpgradeSlot().setUpgrade(closestDirection, newUpgrade);

    if (dropStack != null && !dropStack.isEmpty()) {
      Containers.dropItemStack(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, dropStack);
    }

    if (!player.isCreative()) {
      heldItem.shrink(1);
    }

    return InteractionResult.CONSUME;
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new PipeBlockEntity(com.omarlawatey.utils.ModBlockEntities.PIPE_BLOCK_ENTITY, pos, state);
  }

  @Nullable
  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
      BlockEntityType<T> type) {
    if (level.isClientSide()) {
      return null;
    }
    return type == com.omarlawatey.utils.ModBlockEntities.PIPE_BLOCK_ENTITY
        ? (lvl, pos, st, be) -> PipeBlockEntity.serverTick(lvl, pos, st, (PipeBlockEntity) be)
        : null;
  }

  private Direction findClosestConnection(BlockState state, BlockHitResult hit) {
    double hitX = hit.getLocation().x - hit.getBlockPos().getX();
    double hitY = hit.getLocation().y - hit.getBlockPos().getY();
    double hitZ = hit.getLocation().z - hit.getBlockPos().getZ();

    for (Direction dir : DIRECTIONS) {
      PipeSideState sideState = state.getValue(getPropertyFor(dir));
      if (sideState == PipeSideState.NONE)
        continue;

      AABB box = CONNECTION_AABB_CACHE.get(dir);

      if (hitX >= box.minX && hitX <= box.maxX &&
          hitY >= box.minY && hitY <= box.maxY &&
          hitZ >= box.minZ && hitZ <= box.maxZ)
        return dir;
    }

    boolean hitCore = hitX >= CORE_AABB.minX && hitX <= CORE_AABB.maxX &&
        hitY >= CORE_AABB.minY && hitY <= CORE_AABB.maxY &&
        hitZ >= CORE_AABB.minZ && hitZ <= CORE_AABB.maxZ;

    if (hitCore) {
      Direction hitFace = hit.getDirection();
      if (state.getValue(getPropertyFor(hitFace)) == PipeSideState.NONE)
        return hitFace;
    }

    return null;
  }

  private int getShapeIndex(BlockState state) {
    int index = 0;
    if (state.getValue(NORTH) != PipeSideState.NONE)
      index |= 1;
    if (state.getValue(SOUTH) != PipeSideState.NONE)
      index |= 2;
    if (state.getValue(EAST) != PipeSideState.NONE)
      index |= 4;
    if (state.getValue(WEST) != PipeSideState.NONE)
      index |= 8;
    if (state.getValue(UP) != PipeSideState.NONE)
      index |= 16;
    if (state.getValue(DOWN) != PipeSideState.NONE)
      index |= 32;
    return index;
  }

  private boolean canConnectTo(LevelAccessor world, BlockPos neighborPos, Direction direction) {
    BlockState neighborState = world.getBlockState(neighborPos);
    Block neighborBlock = neighborState.getBlock();

    if (neighborBlock instanceof Pipe)
      return true;

    if (world instanceof Level level) {
      Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, neighborPos, direction);
      return storage != null;
    }

    return false;
  }

  public static EnumProperty<PipeSideState> getPropertyFor(Direction direction) {
    return PROPERTY_CACHE.get(direction);
  }

  private PipeSideState nextState(PipeSideState current, boolean isNeighborPipe) {
    if (isNeighborPipe) {
      return switch (current) {
        case NONE -> PipeSideState.DESTINATION;
        case DESTINATION -> PipeSideState.NONE;
        case SOURCE -> PipeSideState.NONE;
      };
    }

    return switch (current) {
      case NONE -> PipeSideState.DESTINATION;
      case DESTINATION -> PipeSideState.SOURCE;
      case SOURCE -> PipeSideState.NONE;
    };
  }
}