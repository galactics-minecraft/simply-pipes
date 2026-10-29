package com.omarlawatey;

import com.omarlawatey.blocks.pipe.filter.DestinationFilterScreen;
import com.omarlawatey.utils.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class SimplyPipesClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    // Register screens
    MenuScreens.register(ModMenus.DESTINATION_FILTER_MENU, DestinationFilterScreen::new);
  }
}
