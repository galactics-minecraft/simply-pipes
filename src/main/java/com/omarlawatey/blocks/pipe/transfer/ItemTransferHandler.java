package com.omarlawatey.blocks.pipe.transfer;

import com.omarlawatey.blocks.pipe.network.DestinationAccess;
import com.omarlawatey.blocks.pipe.network.NetworkState;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class ItemTransferHandler {

  /**
   * Moves up to maxAmount of one item type from source to the destinations.
   * Items only leave the source after a destination has accepted them, all in
   * one transaction, so nothing can be lost. Returns the amount moved.
   */
  public static long transfer(
      Storage<ItemVariant> source,
      long maxAmount,
      Predicate<ItemVariant> upgradeFilter,
      List<DestinationAccess> destinations,
      NetworkState networkState,
      Function<DestinationAccess, Storage<ItemVariant>> storageResolver) {

    if (destinations.isEmpty()) {
      return 0;
    }

    for (ItemVariant variant : findCandidates(source, upgradeFilter)) {
      List<DestinationAccess> valid = filterDestinationsForItem(destinations, variant);
      if (valid.isEmpty()) {
        continue;
      }

      long moved = tryMove(source, variant, maxAmount, valid, networkState, storageResolver);
      if (moved > 0) {
        return moved;
      }
    }
    return 0;
  }

  private static List<ItemVariant> findCandidates(
      Storage<ItemVariant> source, Predicate<ItemVariant> upgradeFilter) {
    List<ItemVariant> result = new ArrayList<>();
    for (StorageView<ItemVariant> view : source) {
      if (view.isResourceBlank() || view.getAmount() <= 0) {
        continue;
      }
      ItemVariant variant = view.getResource();
      if (upgradeFilter.test(variant) && !result.contains(variant)) {
        result.add(variant);
      }
    }
    return result;
  }

  private static long tryMove(
      Storage<ItemVariant> source,
      ItemVariant variant,
      long maxAmount,
      List<DestinationAccess> destinations,
      NetworkState networkState,
      Function<DestinationAccess, Storage<ItemVariant>> storageResolver) {

    // 1. Simulate extraction (never committed)
    long extractable;
    try (Transaction sim = Transaction.openOuter()) {
      extractable = source.extract(variant, maxAmount, sim);
    }
    if (extractable <= 0) {
      return 0;
    }

    // 2. Insert first, then extract exactly what was accepted.
    // Closing the transaction without commit() rolls everything back.
    try (Transaction tx = Transaction.openOuter()) {
      long inserted = distributeEvenly(variant, extractable, destinations, networkState, storageResolver, tx);
      if (inserted <= 0) {
        return 0;
      }

      long extracted = source.extract(variant, inserted, tx);
      if (extracted != inserted) {
        return 0;
      }

      tx.commit();
      return inserted;
    }
  }

  private static List<DestinationAccess> filterDestinationsForItem(
      List<DestinationAccess> destinations, ItemVariant variant) {
    List<DestinationAccess> filtered = new ArrayList<>();
    for (DestinationAccess dest : destinations) {
      if (dest.acceptsItem(variant)) {
        filtered.add(dest);
      }
    }
    return filtered;
  }

  // Even split using a round-robin start, then a second pass so a full
  // destination's share goes to the others. Returns the amount inserted.
  private static long distributeEvenly(
      ItemVariant variant,
      long amount,
      List<DestinationAccess> destinations,
      NetworkState networkState,
      Function<DestinationAccess, Storage<ItemVariant>> storageResolver,
      TransactionContext tx) {

    int size = destinations.size();
    int start = networkState.getNextDestinationIndex(size);
    long perDest = amount / size;
    long extra = amount % size;
    long remaining = amount;

    for (int i = 0; i < size && remaining > 0; i++) {
      Storage<ItemVariant> dest = storageResolver.apply(destinations.get((start + i) % size));
      if (dest == null) {
        continue;
      }
      long share = Math.min(perDest + (i < extra ? 1 : 0), remaining);
      if (share > 0) {
        remaining -= insert(dest, variant, share, tx);
      }
    }

    for (int i = 0; i < size && remaining > 0; i++) {
      Storage<ItemVariant> dest = storageResolver.apply(destinations.get((start + i) % size));
      if (dest != null) {
        remaining -= insert(dest, variant, remaining, tx);
      }
    }

    return amount - remaining;
  }

  private static long insert(
      Storage<ItemVariant> dest, ItemVariant variant, long amount, TransactionContext tx) {
    try (Transaction nested = Transaction.openNested(tx)) {
      long inserted = dest.insert(variant, amount, nested);
      if (inserted > 0) {
        nested.commit();
      }
      return inserted;
    }
  }
}