package me.alextzamalis.engine.inventory;

import org.joml.Vector4f;

import java.util.UUID;

/**
 * An item that occupies one or more cells in a {@link GridInventory}, or unlocks locked cells.
 */
public class InventoryItem {

    private final String instanceId;
    private final ItemKind kind;
    private final String weaponId;
    private final String baseId;
    private final int tier;
    private final String displayName;
    private final int cellWidth;
    private final int cellHeight;
    private final Vector4f color;

    private InventoryItem(String instanceId, ItemKind kind, String weaponId, String baseId, int tier,
                            String displayName, int cellWidth, int cellHeight, Vector4f color) {
        this.instanceId = instanceId;
        this.kind = kind;
        this.weaponId = weaponId;
        this.baseId = baseId;
        this.tier = tier;
        this.displayName = displayName;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.color = new Vector4f(color);
    }

    public static InventoryItem createWeapon(String weaponId, String baseId, int tier,
                                             String displayName, int cellW, int cellH,
                                             Vector4f color) {
        return new InventoryItem(
                UUID.randomUUID().toString(),
                ItemKind.WEAPON,
                weaponId,
                baseId,
                tier,
                displayName,
                cellW,
                cellH,
                color);
    }

    public static InventoryItem createGear(String gearId, String baseId, int tier,
                                           String displayName, int cellW, int cellH,
                                           Vector4f color) {
        return new InventoryItem(
                UUID.randomUUID().toString(),
                ItemKind.GEAR,
                gearId,
                baseId,
                tier,
                displayName,
                cellW,
                cellH,
                color);
    }

    public static InventoryItem createExpansion(String shapeId, String displayName,
                                              int cellW, int cellH, Vector4f color) {
        return new InventoryItem(
                UUID.randomUUID().toString(),
                ItemKind.EXPANSION,
                null,
                shapeId,
                0,
                displayName,
                cellW,
                cellH,
                color);
    }

    public String getInstanceId() { return instanceId; }
    public ItemKind getKind() { return kind; }
    public String getWeaponId() { return weaponId; }
    public String getBaseId() { return baseId; }
    public int getTier() { return tier; }
    public String getDisplayName() { return displayName; }

    public boolean isWeapon() {
        return kind == ItemKind.WEAPON;
    }

    public boolean isGear() {
        return kind == ItemKind.GEAR;
    }

    public boolean isExpansion() {
        return kind == ItemKind.EXPANSION;
    }

    /** Short label for tight UI cells (e.g. "SG T2" or "+1x2"). */
    public String getShortLabel() {
        if (isExpansion()) {
            return "+" + cellWidth + "x" + cellHeight;
        }
        if (isGear()) {
            String prefix = baseId.length() >= 2
                    ? baseId.substring(0, 2).toUpperCase()
                    : baseId.toUpperCase();
            return prefix + " T" + tier;
        }
        String prefix = baseId.length() >= 2
                ? baseId.substring(0, 2).toUpperCase()
                : baseId.toUpperCase();
        return prefix + " T" + tier;
    }

    public int getCellWidth() { return cellWidth; }
    public int getCellHeight() { return cellHeight; }
    public Vector4f getColor() { return color; }
}
