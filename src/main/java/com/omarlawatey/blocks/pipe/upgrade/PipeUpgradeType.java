package com.omarlawatey.blocks.pipe.upgrade;

import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PipeUpgradeType {
  private static final Map<Item, PipeUpgradeType> ITEM_TO_TYPE = new HashMap<>();
  private static final Map<String, PipeUpgradeType> ID_TO_TYPE = new HashMap<>();

  private final String id;
  private final Item item;
  private final PipeUpgradeFactory factory;

  public PipeUpgradeType(String id, Item item, PipeUpgradeFactory factory) {
    this.id = id;
    this.item = item;
    this.factory = factory;
  }

  public String getId() {
    return id;
  }

  public Item getItem() {
    return item;
  }

  public PipeUpgrade createUpgrade() {
    return factory.create(this);
  }

  public static PipeUpgradeType register(String id, Item item, PipeUpgradeFactory factory) {
    PipeUpgradeType type = new PipeUpgradeType(id, item, factory);
    ITEM_TO_TYPE.put(item, type);
    ID_TO_TYPE.put(id, type);
    return type;
  }

  @Nullable
  public static PipeUpgradeType fromItem(Item item) {
    return ITEM_TO_TYPE.get(item);
  }

  @Nullable
  public static PipeUpgradeType fromId(String id) {
    return ID_TO_TYPE.get(id);
  }

  @FunctionalInterface
  public interface PipeUpgradeFactory {
    PipeUpgrade create(PipeUpgradeType type);
  }
}