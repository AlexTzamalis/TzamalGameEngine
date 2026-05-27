package me.alextzamalis.engine.inventory;

import me.alextzamalis.engine.scene.Sprite;

/**
 * Supplies inventory frame sprites for multi-cell weapon footprints.
 */
@FunctionalInterface
public interface InventoryOutlineProvider {
    Sprite getOutline(int cellWidth, int cellHeight);
}
