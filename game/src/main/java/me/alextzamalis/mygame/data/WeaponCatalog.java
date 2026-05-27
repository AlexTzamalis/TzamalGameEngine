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

public final class WeaponCatalog {

    private static final Gson GSON = new Gson();

    private static Map<String, WeaponDefinition> byId;
    private static Map<String, WeaponDefinition> byBaseAndTier;
    private static List<WeaponDefinition> tierOneWeapons;

    private WeaponCatalog() {
    }

    public static void load() {
        if (byId != null) {
            return;
        }
        byId = new HashMap<>();
        byBaseAndTier = new HashMap<>();
        tierOneWeapons = new ArrayList<>();
        try (InputStream is = WeaponCatalog.class.getResourceAsStream("/config/weapons.json")) {
            if (is == null) {
                throw new RuntimeException("weapons.json not found");
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            WeaponsFile file = GSON.fromJson(json, WeaponsFile.class);
            if (file != null && file.weapons != null) {
                for (WeaponDefinition w : file.weapons) {
                    if (w.baseId == null || w.baseId.isEmpty()) {
                        w.baseId = w.id;
                    }
                    byId.put(w.id, w);
                    byBaseAndTier.put(baseTierKey(w.baseId, w.tier), w);
                    if (w.tier == 1) {
                        tierOneWeapons.add(w);
                    }
                }
            }
            Logger.info("Data", "Loaded " + byId.size() + " weapon definitions.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load weapons.json", e);
        }
    }

    public static WeaponDefinition get(String id) {
        load();
        return byId.get(id);
    }

    public static WeaponDefinition getByBaseAndTier(String baseId, int tier) {
        load();
        return byBaseAndTier.get(baseTierKey(baseId, tier));
    }

    public static WeaponDefinition getNextTier(String baseId, int currentTier) {
        return getByBaseAndTier(baseId, currentTier + 1);
    }

    public static WeaponDefinition randomTierOne(Random rng) {
        load();
        if (tierOneWeapons.isEmpty()) {
            return null;
        }
        return tierOneWeapons.get(rng.nextInt(tierOneWeapons.size()));
    }

    private static String baseTierKey(String baseId, int tier) {
        return baseId + ":" + tier;
    }

    private static class WeaponsFile {
        WeaponDefinition[] weapons;
    }
}
