package me.alextzamalis.engine.scene;

import me.alextzamalis.engine.graphics.Texture;

import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * Visual appearance data for a 2D entity.
 *
 * <p>When a {@link Texture} is present, the {@link #color} acts as a tint
 * that is multiplied with the sampled texel color. When no texture is set,
 * the color is used as a solid fill.</p>
 *
 * <p>UV coordinates allow rendering a sub-region of a texture (sprite
 * sheet support).</p>
 *
 * @author Alexandros Tzamalis
 */
public class Sprite {

    /** Texture to render, or null for solid-color sprites. */
    public Texture texture;

    /** RGBA color: acts as tint when textured, solid color otherwise. */
    public Vector4f color;

    /** Bottom-left UV coordinate for sprite sheet sub-regions. */
    public Vector2f uvMin;

    /** Top-right UV coordinate for sprite sheet sub-regions. */
    public Vector2f uvMax;

    /** Default sprite: no texture, white color, full UV range. */
    public Sprite() {
        this.texture = null;
        this.color = new Vector4f(1f, 1f, 1f, 1f);
        this.uvMin = new Vector2f(0f, 0f);
        this.uvMax = new Vector2f(1f, 1f);
    }

    /**
     * Creates a solid-color sprite with no texture.
     *
     * @param color RGBA color.
     */
    public Sprite(Vector4f color) {
        this.texture = null;
        this.color = new Vector4f(color);
        this.uvMin = new Vector2f(0f, 0f);
        this.uvMax = new Vector2f(1f, 1f);
    }

    /**
     * Creates a textured sprite with white tint (no tinting).
     *
     * @param texture the texture to render.
     */
    public Sprite(Texture texture) {
        this.texture = texture;
        this.color = new Vector4f(1f, 1f, 1f, 1f);
        this.uvMin = new Vector2f(0f, 0f);
        this.uvMax = new Vector2f(1f, 1f);
    }

    /**
     * Creates a textured sprite with a custom tint color.
     *
     * @param texture the texture to render.
     * @param color   RGBA tint applied to the texture.
     */
    public Sprite(Texture texture, Vector4f color) {
        this.texture = texture;
        this.color = new Vector4f(color);
        this.uvMin = new Vector2f(0f, 0f);
        this.uvMax = new Vector2f(1f, 1f);
    }
}
