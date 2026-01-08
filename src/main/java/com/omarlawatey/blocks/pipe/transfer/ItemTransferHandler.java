package com.omarlawatey.blocks.pipe.transfer;

import com.omarlawatey.blocks.pipe.network.DestinationAccess;
import com.omarlawatey.blocks.pipe.network.NetworkState;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import java.util.List;
import java.util.function.Predicate;

public class ItemTransferHandler {

  public static ItemExtractionResult extractItems(
      Storage<ItemVariant> source,
      long maxAmount,
      Predicate<ItemVariant> filter) {
    ItemVariant extractedVariant = null;
    long extractedAmount = 0;

    try (Transaction extractTransaction = Transaction.openOuter()) {
      for (StorageView<ItemVariant> view : source) {
        if (!view.isResourceBlank() && view.getAmount() > 0) {
          ItemVariant variant = view.getResource();

          if (!filter.test(variant)) {
            continue;
          }

          extractedVariant = variant;
          extractedAmount = source.extract(extractedVariant, maxAmount, extractTransaction);
          if (extractedAmount > 0) {
            extractTransaction.commit();
            break;
          }
        }
      }
    }

    return new ItemExtractionResult(extractedVariant, extractedAmount);
  }

  public static ItemExtractionResult extractOneItem(Storage<ItemVariant> source) {
    return extractItems(source, 1, item -> true);
  }

  // Splits extracted items evenly across all destinations, using round-robin
  // start position
  public static long distributeEvenly(
      ItemVariant variant,
      long amount,
      List<DestinationAccess> destinations,
      NetworkState networkState,
      java.util.function.Function<DestinationAccess, Storage<ItemVariant>> storageResolver) {
    if (destinations.isEmpty()) {
      return amount;
    }

    long remaining = amount;
    int startIndex = networkState.getNextDestinationIndex(destinations.size());

    long itemsPerDestination = amount / destinations.size();
    long extraItems = amount % destinations.size();

    for (int i = 0; i < destinations.size(); i++) {
      if (remaining == 0)
        break;

      int destIndex = (startIndex + i) % destinations.size();
      DestinationAccess destAccess = destinations.get(destIndex);

      Storage<ItemVariant> destination = storageResolver.apply(destAccess);
      if (destination == null) {
        continue;
      }

      long toInsert = itemsPerDestination;
      if (i < extraItems) {
        toInsert++;
      }

      if (toInsert > remaining) {
        toInsert = remaining;
      }

      try (Transaction insertTransaction = Transaction.openOuter()) {
        long inserted = destination.insert(variant, toInsert, insertTransaction);
        if (inserted > 0) {
          insertTransaction.commit();
          remaining -= inserted;
        }
      }
    }

    return remaining;
  }

  public static boolean insertWithRoundRobin(
      ItemVariant variant,
      long amount,
      List<DestinationAccess> destinations,
      NetworkState networkState,
      java.util.function.Function<DestinationAccess, Storage<ItemVariant>> storageResolver) {
    if (destinations.isEmpty()) {
      return false;
    }

    int destIndex = networkState.getNextDestinationIndex(destinations.size());
    long remaining = amount;

    for (int i = 0; i < destinations.size(); i++) {
      int currentDestIndex = (destIndex + i) % destinations.size();
      DestinationAccess destAccess = destinations.get(currentDestIndex);

      Storage<ItemVariant> destination = storageResolver.apply(destAccess);
      if (destination == null) {
        continue;
      }

      try (Transaction insertTransaction = Transaction.openOuter()) {
        long insertedAmount = destination.insert(variant, remaining, insertTransaction);
        if (insertedAmount > 0) {
          insertTransaction.commit();
          remaining -= insertedAmount;

          if (remaining == 0) {
            return true;
          }
        }
      }
    }

    return remaining < amount;
  }

  public static void returnToSource(Storage<ItemVariant> source, ItemVariant variant, long amount) {
    try (Transaction returnTransaction = Transaction.openOuter()) {
      source.insert(variant, amount, returnTransaction);
      returnTransaction.commit();
    }
  }

  public record ItemExtractionResult(ItemVariant variant, long amount) {
    public boolean isEmpty() {
      return variant == null || amount == 0;
    }
  }
}