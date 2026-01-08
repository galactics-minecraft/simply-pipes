package com.omarlawatey.utils;

import com.omarlawatey.SimplyPipes;
import com.omarlawatey.blocks.pipe.Pipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {
  // Direct references - clean and simple
  public static final Block PIPE = register("pipe",
      new Pipe(BlockBehaviour.Properties.of()
          .strength(1.0f)
          .noOcclusion()
          .explosionResistance(10)),
      true);

  private static Block register(String name, Block block, boolean shouldRegisterItem) {
    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SimplyPipes.MOD_ID, name);
    Block registered = Registry.register(BuiltInRegistries.BLOCK, id, block);

    if (shouldRegisterItem) {
      Registry.register(BuiltInRegistries.ITEM, id,
          new BlockItem(registered, new Item.Properties()));
    }

    return registered;
  }

  public static void initialize() {
  }
}