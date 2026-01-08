package com.omarlawatey.utils;

import com.omarlawatey.SimplyPipes;
import com.omarlawatey.blocks.pipe.PipeBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
  public static final BlockEntityType<PipeBlockEntity> PIPE_BLOCK_ENTITY = Registry.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE,
      ResourceLocation.fromNamespaceAndPath(SimplyPipes.MOD_ID, "pipe"),
      BlockEntityType.Builder.of(
          PipeBlockEntity::new,
          com.omarlawatey.utils.ModBlocks.PIPE).build(null));

  public static void initialize() {

  }
}