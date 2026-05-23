package me.alextzamalis.engine.assets;

import me.alextzamalis.engine.graphics.Shader;
import me.alextzamalis.engine.graphics.Texture;

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
     * Returns a previously loaded texture, or {@code null} if the
     * path has not been loaded yet.
     *
     * @param filePath the key used when the texture was loaded.
     * @return the cached Texture, or {@code null}.
     */
    public static Texture getTexture(String filePath) {
        return textures.get(filePath);
    }

    /**
     * Disposes every cached shader and texture, then clears the maps.
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
    }
}
