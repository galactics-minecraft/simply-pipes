package com.omarlawatey.blocks.pipe;

import net.minecraft.util.StringRepresentable;

public enum PipeSideState implements StringRepresentable {
  NONE("none"), // 0: no_pipe_part
  DESTINATION("destination"), // 1: pipe_part
  SOURCE("source"); // 2: pipe_part

  private final String name;

  PipeSideState(String name) {
    this.name = name;
  }

  @Override
  public String getSerializedName() {
    return this.name;
  }
}
