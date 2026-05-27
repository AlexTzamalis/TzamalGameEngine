package me.alextzamalis.engine.ui;

import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.text.Font;
import me.alextzamalis.engine.graphics.text.TextRenderer;
import me.alextzamalis.engine.inventory.GridInventory;
import me.alextzamalis.engine.inventory.InventoryIconProvider;
import me.alextzamalis.engine.inventory.InventoryItem;
import me.alextzamalis.engine.inventory.PlacedInventoryItem;
import me.alextzamalis.engine.scene.Sprite;

import org.joml.Vector4f;

/**
 * Renders a 7x7-style grid with locked/unlocked cells and placed weapons.
 */
public class UIGridInventoryPanel extends UIElement {

    private final GridInventory inventory;
    private final float cellSize;
    private InventoryIconProvider iconProvider;

    private final Vector4f lockedFill = new Vector4f(0.12f, 0.11f, 0.14f, 0.95f);
    private final Vector4f lockedBorder = new Vector4f(0.22f, 0.2f, 0.26f, 0.9f);
    private final Vector4f lockedInset = new Vector4f(0.08f, 0.07f, 0.1f, 0.85f);
    private final Vector4f unlockedFill = new Vector4f(0.2f, 0.18f, 0.22f, 0.75f);
    private final Vector4f unlockedBorder = new Vector4f(0.42f, 0.38f, 0.48f, 0.9f);
    private final Vector4f hitboxColor = new Vector4f(0.95f, 0.85f, 0.35f, 0.95f);
    private final Vector4f highlightValid = new Vector4f(0.3f, 0.85f, 0.4f, 0.35f);
    private final Vector4f highlightInvalid = new Vector4f(0.9f, 0.25f, 0.2f, 0.35f);

    private InventoryItem dragPreviewItem;
    private int dragPreviewCol = -1;
    private int dragPreviewRow = -1;

    public UIGridInventoryPanel(float x, float y, GridInventory inventory, float cellSize) {
        super(x, y, inventory.getColumns() * cellSize, inventory.getRows() * cellSize);
        this.inventory = inventory;
        this.cellSize = cellSize;
    }

    public void setIconProvider(InventoryIconProvider iconProvider) {
        this.iconProvider = iconProvider;
    }

    public void setDragPreview(InventoryItem item, int col, int row) {
        this.dragPreviewItem = item;
        this.dragPreviewCol = col;
        this.dragPreviewRow = row;
    }

    public void clearDragPreview() {
        dragPreviewItem = null;
        dragPreviewCol = -1;
        dragPreviewRow = -1;
    }

    /**
     * Snaps a footprint top-left to the grid cell under the mouse, clamped inside the grid.
     */
    public int[] snapCellFromMouse(float mouseX, float mouseY, int footprintW, int footprintH) {
        int[] cell = cellFromPoint(mouseX, mouseY);
        if (cell == null) {
            return null;
        }
        int col = clamp(cell[0], 0, inventory.getColumns() - footprintW);
        int row = clamp(cell[1], 0, inventory.getRows() - footprintH);
        return new int[]{col, row};
    }

    public float snappedDrawX(int col) {
        return x + col * cellSize;
    }

    public float snappedDrawY(int row) {
        return y + row * cellSize;
    }

    @Override
    public void update(float dt) {
    }

    @Override
    public void render(BatchRenderer batch, Font font) {
        if (!visible) {
            return;
        }

        if (dragPreviewItem != null && dragPreviewCol >= 0 && dragPreviewRow >= 0) {
            drawFootprintHighlight(batch, dragPreviewItem, dragPreviewCol, dragPreviewRow);
        }

        for (int row = 0; row < inventory.getRows(); row++) {
            for (int col = 0; col < inventory.getColumns(); col++) {
                float cx = x + col * cellSize;
                float cy = y + row * cellSize;
                if (inventory.isLocked(col, row)) {
                    drawLockedTile(batch, cx, cy, cellSize);
                } else if (inventory.getPlacedAt(col, row) == null) {
                    drawUnlockedSlot(batch, cx, cy, cellSize);
                }
            }
        }

        for (PlacedInventoryItem placed : inventory.getPlacedItems()) {
            InventoryItem item = placed.item;
            float px = x + placed.col * cellSize;
            float py = y + placed.row * cellSize;
            float pw = item.getCellWidth() * cellSize;
            float ph = item.getCellHeight() * cellSize;
            drawPlacedItem(batch, font, item, px, py, pw, ph);
        }
    }

    private void drawFootprintHighlight(BatchRenderer batch, InventoryItem item, int col, int row) {
        int w = item.getCellWidth();
        int h = item.getCellHeight();
        float px = x + col * cellSize;
        float py = y + row * cellSize;
        float pw = w * cellSize;
        float ph = h * cellSize;

        boolean valid;
        if (item.isExpansion()) {
            valid = inventory.canUnlockRegion(col, row, w, h);
        } else {
            valid = inventory.canPlace(item, col, row);
        }
        batch.drawQuad(px, py, pw, ph, valid ? highlightValid : highlightInvalid);
    }

    private void drawLockedTile(BatchRenderer batch, float cx, float cy, float size) {
        batch.drawQuad(cx, cy, size, size, lockedFill);
        batch.drawQuad(cx + 2f, cy + 2f, size - 4f, size - 4f, lockedInset);
        batch.drawQuad(cx, cy + size - 2f, size, 2f, lockedBorder);
        batch.drawQuad(cx + size - 2f, cy, 2f, size, lockedBorder);
    }

    private void drawUnlockedSlot(BatchRenderer batch, float cx, float cy, float size) {
        batch.drawQuad(cx + 1f, cy + 1f, size - 2f, size - 2f, unlockedFill);
        batch.drawQuad(cx + 1f, cy + 1f, size - 2f, 2f, unlockedBorder);
        batch.drawQuad(cx + 1f, cy + 1f, 2f, size - 2f, unlockedBorder);
    }

    private void drawPlacedItem(BatchRenderer batch, Font font, InventoryItem item,
                                float px, float py, float pw, float ph) {
        if (item.isWeapon() || item.isGear()) {
            InventoryHitboxDraw.drawFootprint(batch, px, py, pw, ph, hitboxColor);
        }

        float innerPad = 4f;
        float innerX = px + innerPad;
        float innerY = py + innerPad;
        float innerW = pw - innerPad * 2f;
        float innerH = ph - innerPad * 2f;

        if (item.isExpansion()) {
            batch.drawQuad(innerX, innerY, innerW, innerH, item.getColor());
            if (font != null) {
                TextRenderer.drawTextFullyCentered(batch, font, item.getShortLabel(),
                        px + pw / 2f, py + ph / 2f, new Vector4f(1f, 1f, 1f, 1f));
            }
            return;
        }

        Sprite icon = iconProvider != null ? iconProvider.getIcon(item) : null;
        if (icon != null && icon.texture != null) {
            float labelBand = font != null ? 12f : 0f;
            float iconAreaH = Math.max(8f, innerH - labelBand);
            float iconAreaY = innerY + labelBand;
            int regionW = iconRegionPixels(icon, true);
            int regionH = iconRegionPixels(icon, false);
            UISpriteDraw.draw(batch, innerX, iconAreaY, innerW, iconAreaH, icon,
                    new Vector4f(1f, 1f, 1f, 1f), UIScaleMode.FIT, null, regionW, regionH);
            if (font != null) {
                TextRenderer.drawTextCentered(batch, font, item.getShortLabel(),
                        px + pw / 2f, py + 5f, new Vector4f(0.95f, 0.95f, 0.85f, 0.95f));
            }
        } else {
            batch.drawQuad(innerX, innerY, innerW, innerH, item.getColor());
            if (font != null) {
                TextRenderer.drawTextFullyCentered(batch, font, item.getShortLabel(),
                        px + pw / 2f, py + ph / 2f, new Vector4f(1f, 1f, 1f, 1f));
            }
        }
    }

    private static int iconRegionPixels(Sprite icon, boolean width) {
        if (icon.texture == null) {
            return 1;
        }
        float span = width
                ? icon.uvMax.x - icon.uvMin.x
                : icon.uvMax.y - icon.uvMin.y;
        int tex = width ? icon.texture.getWidth() : icon.texture.getHeight();
        return Math.max(1, Math.round(span * tex));
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public int[] cellFromPoint(float px, float py) {
        if (!containsPoint(px, py)) {
            return null;
        }
        float localX = px - x;
        float localY = py - y;
        return inventory.findCellAt(localX, localY, cellSize);
    }

    public float getCellSize() { return cellSize; }
    public GridInventory getInventory() { return inventory; }
}
