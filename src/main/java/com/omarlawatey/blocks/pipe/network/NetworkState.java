package com.omarlawatey.blocks.pipe.network;

/**
 * Shared network state for synchronized round-robin distribution across all
 * pipes in a network
 */
public class NetworkState {
  private int currentDestinationIndex = 0;

  /**
   * Gets the next destination index and increments the counter
   */
  public int getNextDestinationIndex(int totalDestinations) {
    if (totalDestinations == 0)
      return 0;
    int index = currentDestinationIndex % totalDestinations;
    currentDestinationIndex = (currentDestinationIndex + 1) % totalDestinations;
    return index;
  }

  /**
   * Resets the round-robin counter
   */
  public void reset() {
    currentDestinationIndex = 0;
  }
}
