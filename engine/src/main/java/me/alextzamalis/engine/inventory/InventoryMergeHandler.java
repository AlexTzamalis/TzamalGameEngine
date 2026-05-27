package me.alextzamalis.engine.inventory;

/**
 * Attempts to merge two inventory items when one is dropped onto another.
 */
@FunctionalInterface
public interface InventoryMergeHandler {
    /**
     * @return merged item, or null if merge is not allowed.
     */
    InventoryItem tryMerge(InventoryItem dragged, InventoryItem target);
}
