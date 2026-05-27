package me.alextzamalis.engine.ui;

import me.alextzamalis.engine.core.Input;
import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.text.Font;
import me.alextzamalis.engine.graphics.text.TextRenderer;
import me.alextzamalis.engine.inventory.GridInventory;
import me.alextzamalis.engine.inventory.InventoryIconProvider;
import me.alextzamalis.engine.inventory.InventoryItem;
import me.alextzamalis.engine.inventory.InventoryMergeHandler;
import me.alextzamalis.engine.inventory.PlacedInventoryItem;
import me.alextzamalis.engine.scene.Sprite;

import org.joml.Vector4f;

/**
 * Handles mouse drag-and-drop between offer slots and a grid inventory panel.
 */
public class UIDragDropController {

    public static final int OFFER_SLOT_COUNT = 3;

    private final GridInventory grid;
    private final UIGridInventoryPanel gridPanel;
    private final InventoryItem[] offerSlots = new InventoryItem[OFFER_SLOT_COUNT];
    private final float[] offerSlotX = new float[OFFER_SLOT_COUNT];
    private final float offerSlotY;
    private final float offerCellSize;
    private final InventoryMergeHandler mergeHandler;

    private InventoryIconProvider iconProvider;

    private InventoryItem dragging;
    private boolean draggingFromOffer;
    private int draggingOfferIndex = -1;
    private int restoreCol = -1;
    private int restoreRow = -1;
    private float dragOffsetX;
    private float dragOffsetY;

    private float uiMouseX;
    private float uiMouseY;

    private final Vector4f offerSlotFill = new Vector4f(0.22f, 0.2f, 0.26f, 0.85f);
    private final Vector4f offerSlotBorder = new Vector4f(0.45f, 0.4f, 0.5f, 0.9f);
    private final Vector4f hitboxColor = new Vector4f(0.95f, 0.85f, 0.35f, 0.95f);

    public void setMousePosition(float x, float y) {
        this.uiMouseX = x;
        this.uiMouseY = y;
    }

    public void setIconProvider(InventoryIconProvider iconProvider) {
        this.iconProvider = iconProvider;
    }

    public UIDragDropController(GridInventory grid, UIGridInventoryPanel gridPanel,
                                float offerStartX, float offerStartY, float offerGap,
                                float offerCellSize, InventoryMergeHandler mergeHandler) {
        this.grid = grid;
        this.gridPanel = gridPanel;
        this.offerSlotY = offerStartY;
        this.offerCellSize = offerCellSize;
        this.mergeHandler = mergeHandler;
        for (int i = 0; i < OFFER_SLOT_COUNT; i++) {
            offerSlotX[i] = offerStartX + i * (offerCellSize + offerGap);
        }
    }

    public InventoryItem[] getOfferSlots() {
        return offerSlots;
    }

    public void clearOfferSlots() {
        for (int i = 0; i < OFFER_SLOT_COUNT; i++) {
            offerSlots[i] = null;
        }
    }

    public void update() {
        if (Input.isImGuiCapturingMouse()) {
            gridPanel.clearDragPreview();
            return;
        }

        if (dragging == null && Input.isMouseButtonJustPressed(Input.MOUSE_BUTTON_LEFT)) {
            tryPick();
        }

        if (dragging != null) {
            int[] snap = snapPlacementCell();
            if (snap != null) {
                gridPanel.setDragPreview(dragging, snap[0], snap[1]);
            } else {
                gridPanel.clearDragPreview();
            }
        }

        if (dragging != null && Input.isMouseButtonJustReleased(Input.MOUSE_BUTTON_LEFT)) {
            tryDrop();
            gridPanel.clearDragPreview();
        }
    }

    public void renderOfferSlots(BatchRenderer batch, Font font) {
        for (int i = 0; i < OFFER_SLOT_COUNT; i++) {
            float sx = offerSlotX[i];
            drawOfferSlot(batch, sx, offerSlotY, offerCellSize);
            InventoryItem item = offerSlots[i];
            if (item != null && item != dragging) {
                float cellSize = gridPanel.getCellSize();
                float pw = item.getCellWidth() * cellSize;
                float ph = item.getCellHeight() * cellSize;
                float ox = sx + (offerCellSize - pw) / 2f;
                float oy = offerSlotY + (offerCellSize - ph) / 2f;
                renderItem(batch, font, item, ox, oy, pw, ph);
            }
        }
    }

    public void renderDragging(BatchRenderer batch, Font font) {
        if (dragging == null) {
            return;
        }
        float cellSize = gridPanel.getCellSize();
        float w = dragging.getCellWidth() * cellSize;
        float h = dragging.getCellHeight() * cellSize;

        int[] snap = snapPlacementCell();
        float drawX;
        float drawY;
        if (snap != null) {
            drawX = gridPanel.snappedDrawX(snap[0]);
            drawY = gridPanel.snappedDrawY(snap[1]);
        } else {
            drawX = uiMouseX - dragOffsetX;
            drawY = uiMouseY - dragOffsetY;
        }
        renderItem(batch, font, dragging, drawX, drawY, w, h);
    }

    private void drawOfferSlot(BatchRenderer batch, float sx, float sy, float size) {
        batch.drawQuad(sx, sy, size, size, offerSlotFill);
        batch.drawQuad(sx, sy + size - 2f, size, 2f, offerSlotBorder);
        batch.drawQuad(sx + size - 2f, sy, 2f, size, offerSlotBorder);
    }

    private void renderItem(BatchRenderer batch, Font font, InventoryItem item,
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
                    new Vector4f(1f, 1f, 1f, 0.95f), UIScaleMode.FIT, null, regionW, regionH);
            if (font != null) {
                TextRenderer.drawTextCentered(batch, font, item.getShortLabel(),
                        px + pw / 2f, py + 5f, new Vector4f(1f, 1f, 1f, 1f));
            }
        } else {
            batch.drawQuad(innerX, innerY, innerW, innerH, item.getColor());
            if (font != null) {
                TextRenderer.drawTextFullyCentered(batch, font, item.getShortLabel(),
                        px + pw / 2f, py + ph / 2f, new Vector4f(1f, 1f, 1f, 1f));
            }
        }
    }

    private void tryPick() {
        for (int i = 0; i < OFFER_SLOT_COUNT; i++) {
            if (offerSlots[i] != null && hitOfferSlot(i)) {
                dragging = offerSlots[i];
                draggingFromOffer = true;
                draggingOfferIndex = i;
                float cellSize = gridPanel.getCellSize();
                dragOffsetX = dragging.getCellWidth() * cellSize / 2f;
                dragOffsetY = dragging.getCellHeight() * cellSize / 2f;
                return;
            }
        }

        int[] cell = gridPanel.cellFromPoint(uiMouseX, uiMouseY);
        if (cell != null) {
            PlacedInventoryItem placed = grid.getPlacedAt(cell[0], cell[1]);
            if (placed != null && isGridItem(placed.item) && grid.isUnlocked(cell[0], cell[1])) {
                float px = gridPanel.snappedDrawX(placed.col);
                float py = gridPanel.snappedDrawY(placed.row);
                dragging = placed.item;
                draggingFromOffer = false;
                draggingOfferIndex = -1;
                restoreCol = placed.col;
                restoreRow = placed.row;
                grid.remove(placed.item);
                dragOffsetX = uiMouseX - px;
                dragOffsetY = uiMouseY - py;
                return;
            }
        }
    }

    private void tryDrop() {
        Integer offerTarget = findOfferSlotUnderMouse();
        if (offerTarget != null && dragging != null && (dragging.isWeapon() || dragging.isGear())) {
            InventoryItem target = offerSlots[offerTarget];
            if (target != null && target.getKind() == dragging.getKind() && mergeHandler != null) {
                InventoryItem merged = mergeHandler.tryMerge(dragging, target);
                if (merged != null) {
                    offerSlots[offerTarget] = merged;
                    if (draggingFromOffer) {
                        offerSlots[draggingOfferIndex] = null;
                    }
                    clearDragState();
                    return;
                }
            }
        }

        int[] cell = snapPlacementCell();
        if (cell != null && dragging != null) {
            if (dragging.isExpansion()) {
                if (grid.canUnlockRegion(cell[0], cell[1],
                        dragging.getCellWidth(), dragging.getCellHeight())) {
                    grid.unlockRegion(cell[0], cell[1],
                            dragging.getCellWidth(), dragging.getCellHeight());
                    if (draggingFromOffer) {
                        offerSlots[draggingOfferIndex] = null;
                    }
                    clearDragState();
                    return;
                }
            } else if (mergeHandler != null) {
                PlacedInventoryItem target = grid.getPlacedAt(cell[0], cell[1]);
                if (target != null && target.item != dragging
                        && target.item.getKind() == dragging.getKind()
                        && (dragging.isWeapon() || dragging.isGear())) {
                    InventoryItem merged = mergeHandler.tryMerge(dragging, target.item);
                    if (merged != null) {
                        grid.remove(target.item);
                        if (draggingFromOffer) {
                            offerSlots[draggingOfferIndex] = null;
                        }
                        grid.place(merged, target.col, target.row);
                        clearDragState();
                        return;
                    }
                }
            }

            if (isGridItem(dragging) && grid.canPlace(dragging, cell[0], cell[1])) {
                grid.place(dragging, cell[0], cell[1]);
                if (draggingFromOffer) {
                    offerSlots[draggingOfferIndex] = null;
                }
                clearDragState();
                return;
            }
        }

        if (draggingFromOffer) {
            offerSlots[draggingOfferIndex] = dragging;
        } else if (restoreCol >= 0 && dragging != null && isGridItem(dragging)
                && grid.canPlace(dragging, restoreCol, restoreRow)) {
            grid.place(dragging, restoreCol, restoreRow);
        }
        clearDragState();
    }

    private int[] snapPlacementCell() {
        if (dragging == null) {
            return null;
        }
        return gridPanel.snapCellFromMouse(uiMouseX, uiMouseY,
                dragging.getCellWidth(), dragging.getCellHeight());
    }

    private Integer findOfferSlotUnderMouse() {
        for (int i = 0; i < OFFER_SLOT_COUNT; i++) {
            if (hitOfferSlot(i)) {
                return i;
            }
        }
        return null;
    }

    private void clearDragState() {
        dragging = null;
        draggingFromOffer = false;
        draggingOfferIndex = -1;
        restoreCol = -1;
        restoreRow = -1;
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

    private boolean hitOfferSlot(int index) {
        float sx = offerSlotX[index];
        return uiMouseX >= sx && uiMouseX <= sx + offerCellSize
                && uiMouseY >= offerSlotY && uiMouseY <= offerSlotY + offerCellSize;
    }

    private static boolean isGridItem(InventoryItem item) {
        return item != null && (item.isWeapon() || item.isGear());
    }
}
