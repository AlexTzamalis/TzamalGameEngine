package me.alextzamalis.engine.inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed-size grid inventory with locked/unlocked cells and multi-cell weapon placement.
 */
public class GridInventory {

    private final int columns;
    private final int rows;
    private final boolean[][] unlocked;
    private final InventoryItem[][] occupancy;
    private final List<PlacedInventoryItem> placedItems = new ArrayList<>();

    public GridInventory(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
        this.unlocked = new boolean[columns][rows];
        this.occupancy = new InventoryItem[columns][rows];
    }

    public int getColumns() { return columns; }
    public int getRows() { return rows; }

    public List<PlacedInventoryItem> getPlacedItems() {
        return placedItems;
    }

    /**
     * Marks a rectangular region as unlocked (playable for weapons).
     */
    public void initUnlockedRegion(int startCol, int startRow, int width, int height) {
        for (int c = startCol; c < startCol + width; c++) {
            for (int r = startRow; r < startRow + height; r++) {
                if (inBounds(c, r)) {
                    unlocked[c][r] = true;
                }
            }
        }
    }

    public boolean isUnlocked(int col, int row) {
        return inBounds(col, row) && unlocked[col][row];
    }

    public boolean isLocked(int col, int row) {
        return inBounds(col, row) && !unlocked[col][row];
    }

    public boolean canUnlockRegion(int col, int row, int width, int height) {
        if (!regionInBounds(col, row, width, height)) {
            return false;
        }
        for (int c = col; c < col + width; c++) {
            for (int r = row; r < row + height; r++) {
                if (unlocked[c][r] || occupancy[c][r] != null) {
                    return false;
                }
            }
        }
        return true;
    }

    public void unlockRegion(int col, int row, int width, int height) {
        for (int c = col; c < col + width; c++) {
            for (int r = row; r < row + height; r++) {
                if (inBounds(c, r)) {
                    unlocked[c][r] = true;
                }
            }
        }
    }

    public PlacedInventoryItem getPlacedAt(int col, int row) {
        if (!inBounds(col, row)) {
            return null;
        }
        InventoryItem item = occupancy[col][row];
        if (item == null) {
            return null;
        }
        for (PlacedInventoryItem placed : placedItems) {
            if (placed.item == item) {
                return placed;
            }
        }
        return null;
    }

    public boolean canPick(int col, int row) {
        if (!isUnlocked(col, row)) {
            return false;
        }
        return occupancy[col][row] != null;
    }

    public boolean canPlace(InventoryItem item, int col, int row) {
        if (item == null || (!item.isWeapon() && !item.isGear())) {
            return false;
        }
        if (!regionInBounds(col, row, item.getCellWidth(), item.getCellHeight())) {
            return false;
        }
        for (int c = col; c < col + item.getCellWidth(); c++) {
            for (int r = row; r < row + item.getCellHeight(); r++) {
                if (!unlocked[c][r] || occupancy[c][r] != null) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean place(InventoryItem item, int col, int row) {
        if (!canPlace(item, col, row)) {
            return false;
        }
        for (int c = col; c < col + item.getCellWidth(); c++) {
            for (int r = row; r < row + item.getCellHeight(); r++) {
                occupancy[c][r] = item;
            }
        }
        placedItems.add(new PlacedInventoryItem(item, col, row));
        return true;
    }

    public void remove(InventoryItem item) {
        for (int c = 0; c < columns; c++) {
            for (int r = 0; r < rows; r++) {
                if (occupancy[c][r] == item) {
                    occupancy[c][r] = null;
                }
            }
        }
        placedItems.removeIf(p -> p.item == item);
    }

    public int[] findCellAt(float localX, float localY, float cellSize) {
        int col = (int) (localX / cellSize);
        int row = (int) (localY / cellSize);
        if (!inBounds(col, row)) {
            return null;
        }
        return new int[]{col, row};
    }

    /** @deprecated Use {@link InventoryItem#createWeapon}. */
    @Deprecated
    public static InventoryItem createWeaponItem(String weaponId, String baseId, int tier,
                                                 String displayName, int cellW, int cellH,
                                                 org.joml.Vector4f color) {
        return InventoryItem.createWeapon(weaponId, baseId, tier, displayName, cellW, cellH, color);
    }

    private boolean inBounds(int col, int row) {
        return col >= 0 && row >= 0 && col < columns && row < rows;
    }

    private boolean regionInBounds(int col, int row, int width, int height) {
        return col >= 0 && row >= 0
                && col + width <= columns
                && row + height <= rows;
    }
}
