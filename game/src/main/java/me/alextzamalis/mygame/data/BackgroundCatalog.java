package me.alextzamalis.mygame.data;

import com.google.gson.Gson;
import me.alextzamalis.engine.assets.AssetManager;
import me.alextzamalis.engine.core.Logger;
import me.alextzamalis.engine.graphics.Texture;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads wave-block background textures from backgrounds.json.
 */
public final class BackgroundCatalog {

    private static final Gson GSON = new Gson();

    private static BackgroundsFile data;

    private BackgroundCatalog() {
    }

    public static void load() {
        if (data != null) {
            return;
        }
        try (InputStream is = BackgroundCatalog.class.getResourceAsStream("/config/backgrounds.json")) {
            if (is == null) {
                throw new RuntimeException("backgrounds.json not found");
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            data = GSON.fromJson(json, BackgroundsFile.class);
            Logger.info("Data", "Loaded " + data.blocks.size() + " background theme blocks.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load backgrounds.json", e);
        }
    }

    /** Block index for the given wave (0 = waves 1-10, 1 = 11-20, ...). */
    public static int getBlockIndexForWave(int waveNumber) {
        load();
        if (waveNumber <= 0 || data.blocks.isEmpty()) {
            return 0;
        }
        int idx = (waveNumber - 1) / 10;
        return idx % data.blocks.size();
    }

    /** Block index for the wave that starts after a 10-wave boundary (e.g. 11 -> 1). */
    public static int getBlockIndexForUpcomingWave(int clearedWaveNumber) {
        return getBlockIndexForWave(clearedWaveNumber + 1);
    }

    public static Texture getBackgroundForBlock(int blockIndex) {
        load();
        if (data.blocks.isEmpty()) {
            return null;
        }
        int idx = Math.floorMod(blockIndex, data.blocks.size());
        BackgroundBlock block = data.blocks.get(idx);
        String path = data.basePath + "/" + block.backgroundFolder + "/" + data.defaultFile;
        Texture tex = AssetManager.tryLoadTextureResource(path);
        if (tex != null) {
            return tex;
        }
        path = data.basePath + "/" + block.backgroundFolder + "/" + data.fallbackFile;
        return AssetManager.tryLoadTextureResource(path);
    }

    public static String[] getMusicThemesForBlock(int blockIndex) {
        load();
        if (data.blocks.isEmpty()) {
            return new String[0];
        }
        int idx = Math.floorMod(blockIndex, data.blocks.size());
        BackgroundBlock block = data.blocks.get(idx);
        if (block.musicThemes == null || block.musicThemes.isEmpty()) {
            return new String[0];
        }
        return block.musicThemes.toArray(new String[0]);
    }

    public static class BackgroundBlock {
        public String backgroundFolder;
        public List<String> musicThemes = new ArrayList<>();
    }

    private static class BackgroundsFile {
        String basePath = "/backgrounds";
        String defaultFile = "orig_big.png";
        String fallbackFile = "orig.png";
        List<BackgroundBlock> blocks = new ArrayList<>();
    }
}
