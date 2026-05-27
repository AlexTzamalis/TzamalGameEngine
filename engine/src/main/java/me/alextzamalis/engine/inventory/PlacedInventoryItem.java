package me.alextzamalis.engine.inventory;

/**
 * An item placed at a specific origin cell inside a grid inventory.
 */
public class PlacedInventoryItem {

    public final InventoryItem item;
    public final int col;
    public final int row;

    public PlacedInventoryItem(InventoryItem item, int col, int row) {
        this.item = item;
        this.col = col;
        this.row = row;
    }
}
