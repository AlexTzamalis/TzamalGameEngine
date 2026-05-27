package me.alextzamalis.mygame;

import me.alextzamalis.engine.core.Timer;
import me.alextzamalis.engine.inventory.GridInventory;
import me.alextzamalis.engine.inventory.InventoryItem;
import me.alextzamalis.engine.inventory.PlacedInventoryItem;
import me.alextzamalis.mygame.data.GameUiAssets;
import me.alextzamalis.mygame.data.GearCatalog;
import me.alextzamalis.mygame.data.GearDefinition;
import me.alextzamalis.mygame.data.LootTable;
import me.alextzamalis.mygame.data.WeaponCatalog;
import me.alextzamalis.mygame.data.WeaponDefinition;

import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Holds all state for a single Oil Protection Defense run.
 */
public class RunState {

    public static final int BUY_COST = 30;
    public static final float CELL_SIZE = GameUiAssets.s(40f);
    public static final int GRID_SIZE = 7;
    public static final int START_UNLOCK_SIZE = 3;
    public static final int START_UNLOCK_COL = (GRID_SIZE - START_UNLOCK_SIZE) / 2;
    public static final int START_UNLOCK_ROW = (GRID_SIZE - START_UNLOCK_SIZE) / 2;

    /** @deprecated Use {@link #GRID_SIZE}. */
    @Deprecated
    public static final int GRID_COLS = GRID_SIZE;
    /** @deprecated Use {@link #GRID_ROWS}. */
    @Deprecated
    public static final int GRID_ROWS = GRID_SIZE;

    private final GridInventory inventory = new GridInventory(GRID_SIZE, GRID_SIZE);
    private int bucks = 30;
    private boolean setupPhase = true;
    private boolean firstInventoryShown = false;
    private final Random rng = new Random();

    public RunState() {
        inventory.initUnlockedRegion(
                START_UNLOCK_COL,
                START_UNLOCK_ROW,
                START_UNLOCK_SIZE,
                START_UNLOCK_SIZE);
    }

    public GridInventory getInventory() {
        return inventory;
    }

    public int getBucks() {
        return bucks;
    }

    public void addBucks(int amount) {
        bucks += amount;
    }

    public boolean spendBucks(int amount) {
        if (bucks < amount) {
            return false;
        }
        bucks -= amount;
        return true;
    }

    public boolean isSetupPhase() {
        return setupPhase;
    }

    public void endSetupPhase() {
        setupPhase = false;
    }

    public boolean isFirstInventoryShown() {
        return firstInventoryShown;
    }

    public void markFirstInventoryShown() {
        firstInventoryShown = true;
    }

    public InventoryItem createItemFromWeapon(WeaponDefinition def) {
        return InventoryItem.createWeapon(
                def.id, def.baseId, def.tier, def.name,
                def.cellWidth, def.cellHeight, def.getColor());
    }

    public InventoryItem createItemFromGear(GearDefinition def) {
        return InventoryItem.createGear(
                def.id, def.baseId, def.tier, def.name,
                def.cellWidth, def.cellHeight, def.getColor());
    }

    public InventoryItem tryMergeItems(InventoryItem dragged, InventoryItem target) {
        if (dragged == null || target == null) {
            return null;
        }
        if (dragged.getKind() != target.getKind()) {
            return null;
        }
        if (!dragged.getBaseId().equals(target.getBaseId())) {
            return null;
        }
        if (dragged.getTier() != target.getTier()) {
            return null;
        }
        if (dragged.isWeapon()) {
            WeaponDefinition next = WeaponCatalog.getNextTier(dragged.getBaseId(), dragged.getTier());
            if (next == null) {
                return null;
            }
            return createItemFromWeapon(next);
        }
        if (dragged.isGear()) {
            GearCatalog.load();
            GearDefinition next = GearCatalog.getNextTier(dragged.getBaseId(), dragged.getTier());
            if (next == null) {
                return null;
            }
            return createItemFromGear(next);
        }
        return null;
    }

    public void fillOfferSlots(InventoryItem[] offerSlots) {
        LootTable.load();
        for (int i = 0; i < offerSlots.length; i++) {
            offerSlots[i] = LootTable.roll(rng);
        }
    }

    public CombatLoadout buildCombatLoadout() {
        CombatLoadout loadout = new CombatLoadout();
        WeaponCatalog.load();
        GearCatalog.load();
        for (PlacedInventoryItem placed : inventory.getPlacedItems()) {
            InventoryItem item = placed.item;
            if (item.isWeapon()) {
                WeaponDefinition def = WeaponCatalog.get(item.getWeaponId());
                if (def == null) {
                    continue;
                }
                if (def.isBeam()) {
                    loadout.beams.add(new EquippedBeam(def));
                } else {
                    loadout.projectileWeapons.add(new EquippedWeapon(def));
                }
            } else if (item.isGear()) {
                GearDefinition gear = GearCatalog.get(item.getWeaponId());
                if (gear == null) {
                    continue;
                }
                if (gear.isBoots()) {
                    loadout.hasBoots = true;
                    loadout.bootsMaxHpBonus += gear.maxHpBonus;
                    loadout.healAmount = Math.max(loadout.healAmount, gear.healAmount);
                    loadout.healInterval = Math.min(loadout.healInterval, gear.healInterval);
                    loadout.healCooldownDuration = Math.min(loadout.healCooldownDuration, gear.healCooldown);
                } else if (gear.isBelt()) {
                    loadout.hasBelt = true;
                    loadout.beltMaxShieldBonus += gear.maxShieldBonus;
                    loadout.shieldRegenPerSecond += gear.shieldRegenPerSecond;
                }
            }
        }
        return loadout;
    }

    /** @deprecated Use {@link #buildCombatLoadout()}. */
    @Deprecated
    public List<EquippedWeapon> buildEquippedWeapons() {
        return buildCombatLoadout().projectileWeapons;
    }

    public static class CombatLoadout {
        public final List<EquippedWeapon> projectileWeapons = new ArrayList<>();
        public final List<EquippedBeam> beams = new ArrayList<>();
        public int bootsMaxHpBonus;
        public int beltMaxShieldBonus;
        public boolean hasBoots;
        public boolean hasBelt;
        public int healAmount = 1;
        public float healInterval = 8f;
        public float healCooldownDuration = 5f;
        public float shieldRegenPerSecond;
    }

    public static class EquippedWeapon {
        public final WeaponDefinition def;
        public final Timer cooldownTimer;

        public EquippedWeapon(WeaponDefinition def) {
            this.def = def;
            this.cooldownTimer = new Timer(def.cooldown);
            this.cooldownTimer.setAutoReset(true);
        }

        public Vector4f getColor() {
            return def.getColor();
        }
    }

    public static class EquippedBeam {
        public final WeaponDefinition def;
        public final Timer damageTickTimer;

        public EquippedBeam(WeaponDefinition def) {
            this.def = def;
            this.damageTickTimer = new Timer(def.beamTickInterval);
            this.damageTickTimer.setAutoReset(true);
        }

        public Vector4f getColor() {
            return def.getColor();
        }
    }
}
