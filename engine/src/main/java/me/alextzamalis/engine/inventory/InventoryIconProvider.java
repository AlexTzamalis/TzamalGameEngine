package me.alextzamalis.engine.inventory;

import me.alextzamalis.engine.scene.Sprite;

/**
 * Supplies optional icon sprites for inventory items.
 */
@FunctionalInterface
public interface InventoryIconProvider {
    Sprite getIcon(InventoryItem item);
}
