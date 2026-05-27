package me.alextzamalis.mygame.data;

import com.google.gson.Gson;
import me.alextzamalis.engine.assets.AssetManager;
import me.alextzamalis.engine.core.Logger;
import me.alextzamalis.engine.graphics.NineSliceInsets;
import me.alextzamalis.engine.graphics.Texture;
import me.alextzamalis.engine.scene.Sprite;

import org.joml.Vector2f;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads Sprout Lands UI sprite regions from ui-atlas.json.
 */
public final class UiAtlasCatalog {

    private static final Gson GSON = new Gson();

    private static Map<String, Texture> textures;
    private static Map<String, AtlasRegion> regions;

    private UiAtlasCatalog() {
    }

    public static void load() {
        if (regions != null) {
            return;
        }
        textures = new HashMap<>();
        regions = new HashMap<>();
        try (InputStream is = UiAtlasCatalog.class.getResourceAsStream("/config/ui-atlas.json")) {
            if (is == null) {
                throw new RuntimeException("ui-atlas.json not found on classpath");
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            UiAtlasFile file = GSON.fromJson(json, UiAtlasFile.class);
            if (file.textures != null) {
                for (Map.Entry<String, String> entry : file.textures.entrySet()) {
                    textures.put(entry.getKey(), AssetManager.loadTextureResource(entry.getValue()));
                }
            }
            if (file.regions != null) {
                for (Map.Entry<String, AtlasRegion> entry : file.regions.entrySet()) {
                    regions.put(entry.getKey(), entry.getValue());
                }
            }
            Logger.info("Data", "Loaded " + regions.size() + " UI atlas regions.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load ui-atlas.json", e);
        }
    }

    public static Sprite getSprite(String regionId) {
        load();
        AtlasRegion region = regions.get(regionId);
        if (region == null) {
            return null;
        }
        Texture texture = textures.get(region.texture);
        if (texture == null) {
            return null;
        }
        return region.toSprite(texture);
    }

    public static AtlasRegion getRegion(String regionId) {
        load();
        return regions.get(regionId);
    }

    public static Texture getTexture(String textureKey) {
        load();
        return textures.get(textureKey);
    }

    public static NineSliceInsets getNineSliceInsets(String regionId) {
        AtlasRegion region = getRegion(regionId);
        if (region == null) {
            return null;
        }
        return region.toNineSliceInsets();
    }

    /** Returns a grid footprint frame from ui-atlas.json (e.g. outline2x1). */
    public static Sprite getGridOutline(int cellWidth, int cellHeight) {
        Sprite outline = getSprite("outline" + cellWidth + "x" + cellHeight);
        if (outline != null) {
            return outline;
        }
        return getSprite("outline1x1");
    }

    public static class AtlasRegion {
        public String texture;
        public int x;
        public int y;
        public int w;
        public int h;
        public int sliceLeft;
        public int sliceRight;
        public int sliceTop;
        public int sliceBottom;

        public Sprite toSprite(Texture texture) {
            float texW = texture.getWidth();
            float texH = texture.getHeight();
            float uvMinX = x / texW;
            float uvMaxX = (x + w) / texW;
            float uvMinY = 1f - (y + h) / texH;
            float uvMaxY = 1f - y / texH;

            Sprite sprite = new Sprite(texture);
            sprite.uvMin = new Vector2f(uvMinX, uvMinY);
            sprite.uvMax = new Vector2f(uvMaxX, uvMaxY);
            return sprite;
        }

        public NineSliceInsets toNineSliceInsets() {
            if (sliceLeft <= 0 && sliceRight <= 0 && sliceTop <= 0 && sliceBottom <= 0) {
                return NineSliceInsets.proportional(w, h, 0.25f);
            }
            return new NineSliceInsets(sliceLeft, sliceRight, sliceTop, sliceBottom);
        }
    }

    private static class UiAtlasFile {
        Map<String, String> textures;
        Map<String, AtlasRegion> regions;
    }
}
