package com.omarlawatey.utils;

import com.omarlawatey.SimplyPipes;
import com.omarlawatey.blocks.pipe.filter.DestinationFilterMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

public class ModMenus {
  public static final MenuType<DestinationFilterMenu> DESTINATION_FILTER_MENU = new ExtendedScreenHandlerType<>(
      DestinationFilterMenu::new, DestinationFilterMenu.FilterData.STREAM_CODEC);

  public static void initialize() {
    Registry.register(BuiltInRegistries.MENU,
        ResourceLocation.fromNamespaceAndPath(SimplyPipes.MOD_ID, "destination_filter"),
        DESTINATION_FILTER_MENU);
  }
}
