package me.alextzamalis.mygame.data;

import com.google.gson.Gson;
import me.alextzamalis.engine.assets.AssetManager;
import me.alextzamalis.engine.core.Logger;
import me.alextzamalis.engine.graphics.Texture;
import me.alextzamalis.engine.scene.Sprite;

import org.joml.Vector2f;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves weapon icon sprites from weapons-atlas.json.
 */
public final class WeaponIconCatalog {

    private static final Gson GSON = new Gson();

    private static Texture atlasTexture;
    private static Map<String, UiAtlasCatalog.AtlasRegion> icons;

    private WeaponIconCatalog() {
    }

    public static void load() {
        if (icons != null) {
            return;
        }
        icons = new HashMap<>();
        try (InputStream is = WeaponIconCatalog.class.getResourceAsStream("/config/weapons-atlas.json")) {
            if (is == null) {
                throw new RuntimeException("weapons-atlas.json not found on classpath");
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            WeaponsAtlasFile file = GSON.fromJson(json, WeaponsAtlasFile.class);
            if (file.texture != null) {
                atlasTexture = AssetManager.loadTextureResource(file.texture);
            }
            if (file.icons != null) {
                icons.putAll(file.icons);
            }
            Logger.info("Data", "Loaded " + icons.size() + " weapon icon regions.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load weapons-atlas.json", e);
        }
    }

    public static Sprite getIcon(String baseId, int tier) {
        load();
        if (atlasTexture == null) {
            return null;
        }
        UiAtlasCatalog.AtlasRegion region = icons.get(baseId + ":" + tier);
        if (region == null) {
            region = icons.get(baseId + ":1");
        }
        if (region == null) {
            return null;
        }
        return region.toSprite(atlasTexture);
    }

    private static class WeaponsAtlasFile {
        String texture;
        Map<String, UiAtlasCatalog.AtlasRegion> icons;
    }
}
