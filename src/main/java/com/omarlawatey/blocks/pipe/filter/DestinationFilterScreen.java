package com.omarlawatey.blocks.pipe.filter;

import com.omarlawatey.SimplyPipes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the destination filter GUI.
 */
public class DestinationFilterScreen extends AbstractContainerScreen<DestinationFilterMenu> {
  private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
      SimplyPipes.MOD_ID, "textures/gui/destination_filter.png");

  private Button modeButton;

  public DestinationFilterScreen(DestinationFilterMenu menu, Inventory playerInventory, Component title) {
    super(menu, playerInventory, title);
    this.imageWidth = 176;
    this.imageHeight = 166;
  }

  @Override
  protected void init() {
    super.init();

    // Position labels
    this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    this.titleLabelY = 6;
    this.inventoryLabelY = this.imageHeight - 94;

    // Add whitelist/blacklist toggle button
    int buttonWidth = 60;
    int buttonX = this.leftPos + (this.imageWidth - buttonWidth) / 2;
    int buttonY = this.topPos + 72;

    this.modeButton = Button.builder(getModeButtonText(), button -> {
      // Send button click to server (button ID 0)
      if (this.minecraft != null && this.minecraft.gameMode != null) {
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
      }
    }).bounds(buttonX, buttonY, buttonWidth, 12).build();

    this.addRenderableWidget(modeButton);
  }

  @Override
  protected void containerTick() {
    super.containerTick();
    // Update button text based on current menu state (synced from server via
    // DataSlot)
    if (modeButton != null) {
      modeButton.setMessage(getModeButtonText());
    }
  }

  private Component getModeButtonText() {
    if (menu.isBlacklist()) {
      return Component.translatable("simplypipes.gui.filter.blacklist");
    } else {
      return Component.translatable("simplypipes.gui.filter.whitelist");
    }
  }

  @Override
  protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
  }

  @Override
  public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    super.render(guiGraphics, mouseX, mouseY, partialTick);
    this.renderTooltip(guiGraphics, mouseX, mouseY);
  }
}
