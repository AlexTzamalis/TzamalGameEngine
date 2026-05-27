package me.alextzamalis.mygame.data;

import com.google.gson.Gson;
import me.alextzamalis.engine.core.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class EnemyCatalog {

    private static final Gson GSON = new Gson();

    private static Map<String, EnemyDefinition> byId;

    private EnemyCatalog() {
    }

    public static void load() {
        if (byId != null) {
            return;
        }
        byId = new HashMap<>();
        try (InputStream is = EnemyCatalog.class.getResourceAsStream("/config/enemies.json")) {
            if (is == null) {
                throw new RuntimeException("enemies.json not found");
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            EnemiesFile file = GSON.fromJson(json, EnemiesFile.class);
            if (file != null && file.enemies != null) {
                for (EnemyDefinition e : file.enemies) {
                    byId.put(e.id, e);
                }
            }
            Logger.info("Data", "Loaded " + byId.size() + " enemy definitions.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load enemies.json", e);
        }
    }

    public static EnemyDefinition get(String id) {
        load();
        return byId.get(id);
    }

    private static class EnemiesFile {
        EnemyDefinition[] enemies;
    }
}
