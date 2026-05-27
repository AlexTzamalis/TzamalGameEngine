package me.alextzamalis.mygame.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.alextzamalis.engine.core.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SettingsStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SAVE_PATH = Path.of(
            System.getProperty("user.home"), ".oilprotectiondefense", "settings.json");

    private static GameSettings cached;

    private SettingsStore() {
    }

    public static GameSettings load() {
        if (cached != null) {
            return cached;
        }
        if (Files.exists(SAVE_PATH)) {
            try (Reader reader = Files.newBufferedReader(SAVE_PATH, StandardCharsets.UTF_8)) {
                cached = GSON.fromJson(reader, GameSettings.class);
                if (cached != null) {
                    return cached;
                }
            } catch (IOException e) {
                Logger.warn("Settings", "Failed to read save file, using defaults: " + e.getMessage());
            }
        }
        cached = loadDefaults();
        return cached;
    }

    public static void save(GameSettings settings) {
        cached = settings;
        try {
            Files.createDirectories(SAVE_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(SAVE_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(settings, writer);
            }
        } catch (IOException e) {
            Logger.error("Settings", "Failed to save settings", e);
        }
    }

    private static GameSettings loadDefaults() {
        try (InputStream is = SettingsStore.class.getResourceAsStream("/config/default_settings.json")) {
            if (is == null) {
                return new GameSettings();
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            GameSettings defaults = GSON.fromJson(json, GameSettings.class);
            return defaults != null ? defaults : new GameSettings();
        } catch (IOException e) {
            Logger.warn("Settings", "Failed to load default settings: " + e.getMessage());
            return new GameSettings();
        }
    }
}
