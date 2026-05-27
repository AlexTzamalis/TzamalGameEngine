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

/**
 * Resolves themed music track paths from themed-music.json.
 */
public final class ThemedMusicCatalog {

    private static final Gson GSON = new Gson();

    private static Map<String, List<String>> themes;
    private static final Random rng = new Random();

    private ThemedMusicCatalog() {
    }

    public static void load() {
        if (themes != null) {
            return;
        }
        themes = new HashMap<>();
        try (InputStream is = ThemedMusicCatalog.class.getResourceAsStream("/config/themed-music.json")) {
            if (is == null) {
                throw new RuntimeException("themed-music.json not found");
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            ThemedMusicFile file = GSON.fromJson(json, ThemedMusicFile.class);
            if (file.themes != null) {
                themes.putAll(file.themes);
            }
            Logger.info("Data", "Loaded themed music for " + themes.size() + " categories.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load themed-music.json", e);
        }
    }

    public static String randomTrack(String... themeNames) {
        load();
        List<String> pool = new ArrayList<>();
        for (String theme : themeNames) {
            List<String> tracks = themes.get(theme);
            if (tracks != null) {
                pool.addAll(tracks);
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(rng.nextInt(pool.size()));
    }

    private static class ThemedMusicFile {
        Map<String, List<String>> themes = new HashMap<>();
    }
}
