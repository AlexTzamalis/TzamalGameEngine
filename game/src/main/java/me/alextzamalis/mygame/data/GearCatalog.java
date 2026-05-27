package me.alextzamalis.mygame.data;

import com.google.gson.Gson;
import me.alextzamalis.engine.core.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class GearCatalog {

    private static final Gson GSON = new Gson();

    private static Map<String, GearDefinition> byId;
    private static Map<String, GearDefinition> byBaseAndTier;
    private static List<GearDefinition> tierOneGear;

    private GearCatalog() {
    }

    public static void load() {
        if (byId != null) {
            return;
        }
        byId = new HashMap<>();
        byBaseAndTier = new HashMap<>();
        tierOneGear = new ArrayList<>();
        try (InputStream is = GearCatalog.class.getResourceAsStream("/config/gear.json")) {
            if (is == null) {
                throw new RuntimeException("gear.json not found");
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            GearFile file = GSON.fromJson(json, GearFile.class);
            if (file != null && file.gear != null) {
                for (GearDefinition g : file.gear) {
                    if (g.baseId == null || g.baseId.isEmpty()) {
                        g.baseId = g.id;
                    }
                    byId.put(g.id, g);
                    byBaseAndTier.put(baseTierKey(g.baseId, g.tier), g);
                    if (g.tier == 1) {
                        tierOneGear.add(g);
                    }
                }
            }
            Logger.info("Data", "Loaded " + byId.size() + " gear definitions.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load gear.json", e);
        }
    }

    public static GearDefinition get(String id) {
        load();
        return byId.get(id);
    }

    public static GearDefinition getByBaseAndTier(String baseId, int tier) {
        load();
        return byBaseAndTier.get(baseTierKey(baseId, tier));
    }

    public static GearDefinition getNextTier(String baseId, int currentTier) {
        return getByBaseAndTier(baseId, currentTier + 1);
    }

    public static GearDefinition randomTierOne(Random rng) {
        load();
        if (tierOneGear.isEmpty()) {
            return null;
        }
        return tierOneGear.get(rng.nextInt(tierOneGear.size()));
    }

    private static String baseTierKey(String baseId, int tier) {
        return baseId + ":" + tier;
    }

    private static class GearFile {
        GearDefinition[] gear;
    }
}
