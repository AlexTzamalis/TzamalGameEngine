package me.alextzamalis.mygame.data;

import com.google.gson.Gson;
import me.alextzamalis.engine.core.Logger;
import me.alextzamalis.engine.inventory.InventoryItem;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ExpansionCatalog {

    private static final Gson GSON = new Gson();

    private static Map<String, ExpansionDefinition> byId;
    private static List<ExpansionDefinition> all;
    private static int totalWeight;

    private ExpansionCatalog() {
    }

    public static void load() {
        if (byId != null) {
            return;
        }
        byId = new HashMap<>();
        all = new ArrayList<>();
        totalWeight = 0;
        try (InputStream is = ExpansionCatalog.class.getResourceAsStream("/config/expansions.json")) {
            if (is == null) {
                throw new RuntimeException("expansions.json not found");
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            ExpansionsFile file = GSON.fromJson(json, ExpansionsFile.class);
            if (file != null && file.expansions != null) {
                for (ExpansionDefinition def : file.expansions) {
                    byId.put(def.id, def);
                    all.add(def);
                    totalWeight += Math.max(1, def.weight);
                }
            }
            Logger.info("Data", "Loaded " + all.size() + " expansion definitions.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load expansions.json", e);
        }
    }

    public static ExpansionDefinition get(String id) {
        load();
        return byId.get(id);
    }

    public static ExpansionDefinition randomWeighted(Random rng) {
        load();
        if (all.isEmpty()) {
            return null;
        }
        int roll = rng.nextInt(totalWeight);
        int sum = 0;
        for (ExpansionDefinition def : all) {
            sum += Math.max(1, def.weight);
            if (roll < sum) {
                return def;
            }
        }
        return all.get(all.size() - 1);
    }

    public static InventoryItem createItem(ExpansionDefinition def) {
        return InventoryItem.createExpansion(
                def.id,
                def.name,
                def.cellWidth,
                def.cellHeight,
                def.getColor());
    }

    private static class ExpansionsFile {
        ExpansionDefinition[] expansions;
    }
}
