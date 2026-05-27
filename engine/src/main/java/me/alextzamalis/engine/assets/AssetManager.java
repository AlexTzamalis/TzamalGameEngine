package me.alextzamalis.engine.assets;

import me.alextzamalis.engine.graphics.Shader;
import me.alextzamalis.engine.graphics.Texture;
import me.alextzamalis.engine.graphics.text.Font;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A static asset cache that prevents duplicate GPU resource allocation.
 *
 * <h2>Why cache assets?</h2>
 * <p>Every {@link Shader} and {@link Texture} allocates GPU-side objects
 * (programs, texture IDs). Loading the same asset twice wastes VRAM and
 * makes disposal error-prone. {@code AssetManager} guarantees that each
 * unique shader name or texture path maps to exactly one GPU object.</p>
 *
 * <h2>Ownership</h2>
 * <p>Assets added to the manager are owned by it. Call
 * {@link #disposeAll()} during engine shutdown to release every cached
 * resource in one shot.</p>
 *
 * @author Alexandros Tzamalis
 * @see Shader
 * @see Texture
 */
public final class AssetManager {

    private static final Map<String, Shader> shaders = new HashMap<>();
    private static final Map<String, Texture> textures = new HashMap<>();
    private static final Map<String, Font> fonts = new HashMap<>();

    private AssetManager() {
        // Static utility class.
    }

    // Shader management
    /**
     * Registers a shader under the given name.
     *
     * @param name   a unique key for this shader (e.g. "default").
     * @param shader the compiled shader program.
     */
    public static void addShader(String name, Shader shader) {
        shaders.put(name, shader);
    }

    /**
     * Returns the shader registered under the given name.
     *
     * @param name the key used in {@link #addShader(String, Shader)}.
     * @return the cached Shader, or {@code null} if not found.
     */
    public static Shader getShader(String name) {
        return shaders.get(name);
    }

    /**
     * Returns the "default" batch-rendering shader, loading it from
     * classpath resources and uploading the 16 texture slot uniforms
     * if this is the first call. Subsequent calls return the cached
     * instance immediately.
     *
     * @return the default shader, ready to use with {@code BatchRenderer}.
     */
    public static Shader getOrLoadDefaultShader() {
        Shader cached = shaders.get("default");
        if (cached != null) {
            return cached;
        }
        Shader shader = Shader.fromResource("/shaders/default.vert", "/shaders/default.frag");
        shader.bind();
        for (int i = 0; i < 16; i++) {
            shader.uploadTexture("uTextures[" + i + "]", i);
        }
        shader.unbind();
        shaders.put("default", shader);
        return shader;
    }

    // Texture management
    /**
     * Returns a cached texture for the given file path, loading it
     * from disk if this is the first request.
     *
     * @param filePath path to the image file.
     * @return the cached (or newly loaded) Texture.
     */
    public static Texture loadTexture(String filePath) {
        Texture cached = textures.get(filePath);
        if (cached != null) {
            return cached;
        }
        Texture tex = new Texture(filePath);
        textures.put(filePath, tex);
        return tex;
    }

    /**
     * Returns a cached texture for the given classpath resource path,
     * loading it from the classpath if this is the first request.
     *
     * @param resourcePath classpath path (e.g. {@code "/atlas/img.png"}).
     * @return the cached (or newly loaded) Texture.
     */
    public static Texture loadTextureResource(String resourcePath) {
        Texture cached = textures.get(resourcePath);
        if (cached != null) {
            return cached;
        }
        Texture tex = Texture.fromResource(resourcePath);
        textures.put(resourcePath, tex);
        return tex;
    }

    /**
     * Loads a classpath texture if present, or returns {@code null} when the
     * resource is missing or cannot be decoded.
     *
     * @param resourcePath classpath path (e.g. {@code "/atlas/img.png"}).
     * @return the cached (or newly loaded) Texture, or {@code null}.
     */
    public static Texture tryLoadTextureResource(String resourcePath) {
        Texture cached = textures.get(resourcePath);
        if (cached != null) {
            return cached;
        }
        try (InputStream is = AssetManager.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        Texture tex = Texture.fromResource(resourcePath);
        textures.put(resourcePath, tex);
        return tex;
    }

    /**
     * Returns a previously loaded texture, or {@code null} if the
     * path has not been loaded yet.
     *
     * @param filePath the key used when the texture was loaded.
     * @return the cached Texture, or {@code null}.
     */
    public static Texture getTexture(String filePath) {
        return textures.get(filePath);
    }

    // Font management

    /**
     * Loads a font from a file path at the given pixel size and caches
     * it under a composite key of "path:size". Returns the cached font
     * if it was already loaded at that size.
     *
     * @param ttfPath   path to the {@code .ttf} file.
     * @param pixelSize desired rasterization size in pixels.
     * @return the cached (or newly loaded) Font.
     */
    public static Font loadFont(String ttfPath, float pixelSize) {
        String key = ttfPath + ":" + pixelSize;
        Font cached = fonts.get(key);
        if (cached != null) {
            return cached;
        }
        Font font = new Font(ttfPath, pixelSize);
        fonts.put(key, font);
        return font;
    }

    /**
     * Loads a font from a classpath resource at the given pixel size
     * and caches it under a composite key.
     *
     * @param resourcePath classpath path (e.g. {@code "/fonts/default.ttf"}).
     * @param pixelSize    desired rasterization size in pixels.
     * @return the cached (or newly loaded) Font.
     */
    public static Font loadFontResource(String resourcePath, float pixelSize) {
        String key = resourcePath + ":" + pixelSize;
        Font cached = fonts.get(key);
        if (cached != null) {
            return cached;
        }
        Font font = Font.fromResource(resourcePath, pixelSize);
        fonts.put(key, font);
        return font;
    }

    /**
     * Returns a previously loaded font, or {@code null} if the
     * key has not been loaded yet.
     *
     * @param key the composite key used when the font was loaded
     *            (format: "path:size").
     * @return the cached Font, or {@code null}.
     */
    public static Font getFont(String key) {
        return fonts.get(key);
    }

    /**
     * Disposes every cached shader, texture, and font, then clears the maps.
     *
     * <p>Call this once during engine shutdown (typically from the
     * game's dispose method).</p>
     */
    public static void disposeAll() {
        for (Shader s : shaders.values()) {
            s.dispose();
        }
        shaders.clear();

        for (Texture t : textures.values()) {
            t.dispose();
        }
        textures.clear();

        for (Font f : fonts.values()) {
            f.dispose();
        }
        fonts.clear();
    }
}
