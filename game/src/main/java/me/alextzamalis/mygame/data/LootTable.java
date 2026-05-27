package me.alextzamalis.mygame.data;

import com.google.gson.Gson;
import me.alextzamalis.engine.core.Logger;
import me.alextzamalis.engine.inventory.InventoryItem;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Weighted random loot for inventory offer slots (weapons, expansions, future types).
 */
public final class LootTable {

    private static final Gson GSON = new Gson();

    private static List<LootEntry> entries;
    private static int totalWeight;

    private LootTable() {
    }

    public static void load() {
        if (entries != null) {
            return;
        }
        entries = new ArrayList<>();
        totalWeight = 0;
        try (InputStream is = LootTable.class.getResourceAsStream("/config/loot-table.json")) {
            if (is == null) {
                throw new RuntimeException("loot-table.json not found");
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            LootFile file = GSON.fromJson(json, LootFile.class);
            if (file != null && file.entries != null) {
                for (LootEntry entry : file.entries) {
                    entries.add(entry);
                    totalWeight += Math.max(1, entry.weight);
                }
            }
            Logger.info("Data", "Loaded loot table with " + entries.size() + " entry types.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load loot-table.json", e);
        }
    }

    public static InventoryItem roll(Random rng) {
        load();
        WeaponCatalog.load();
        ExpansionCatalog.load();

        String type = pickType(rng);
        if ("expansion".equals(type)) {
            ExpansionDefinition def = ExpansionCatalog.randomWeighted(rng);
            if (def != null) {
                return ExpansionCatalog.createItem(def);
            }
        }
        if ("gear".equals(type)) {
            GearDefinition gear = GearCatalog.randomTierOne(rng);
            if (gear != null) {
                return InventoryItem.createGear(
                        gear.id, gear.baseId, gear.tier, gear.name,
                        gear.cellWidth, gear.cellHeight, gear.getColor());
            }
        }

        WeaponDefinition weapon = WeaponCatalog.randomTierOne(rng);
        if (weapon != null) {
            return InventoryItem.createWeapon(
                    weapon.id,
                    weapon.baseId,
                    weapon.tier,
                    weapon.name,
                    weapon.cellWidth,
                    weapon.cellHeight,
                    weapon.getColor());
        }
        return null;
    }

    private static String pickType(Random rng) {
        if (entries.isEmpty()) {
            return "weapon";
        }
        int roll = rng.nextInt(totalWeight);
        int sum = 0;
        for (LootEntry entry : entries) {
            sum += Math.max(1, entry.weight);
            if (roll < sum) {
                return entry.type;
            }
        }
        return entries.get(entries.size() - 1).type;
    }

    private static class LootFile {
        LootEntry[] entries;
    }

    private static class LootEntry {
        String type;
        int weight;
    }
}
