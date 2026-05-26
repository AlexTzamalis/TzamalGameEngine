package me.alextzamalis.engine.graphics.text;

import org.joml.Vector2f;

/**
 * Per-character layout metrics and UV coordinates for a rasterized glyph.
 *
 * <p>All positional values are in pixels at the font's rasterized size.
 * UV values are normalized 0-1 coordinates into the font's atlas
 * texture, already converted to OpenGL's bottom-left origin.</p>
 *
 * @author Alexandros Tzamalis
 * @see Font
 * @see TextRenderer
 */
public class Glyph {

    /** Bottom-left UV in the atlas texture (OpenGL coordinates). */
    public final Vector2f uvMin;

    /** Top-right UV in the atlas texture (OpenGL coordinates). */
    public final Vector2f uvMax;

    /** Glyph bitmap width in pixels. */
    public final float width;

    /** Glyph bitmap height in pixels. */
    public final float height;

    /**
     * Horizontal offset from the cursor to the left edge of the glyph.
     * Can be negative for glyphs that overhang to the left.
     */
    public final float bearingX;

    /**
     * Vertical offset from the baseline to the top of the glyph,
     * in STB's Y-down coordinate system. Typically negative (glyph
     * extends above the baseline). TextRenderer converts this to
     * the engine's Y-up system when positioning quads.
     */
    public final float bearingY;

    /** Horizontal distance to advance the cursor after this glyph. */
    public final float advance;

    /**
     * @param uvMin    bottom-left UV in atlas (OpenGL coords).
     * @param uvMax    top-right UV in atlas (OpenGL coords).
     * @param width    glyph bitmap width in pixels.
     * @param height   glyph bitmap height in pixels.
     * @param bearingX horizontal offset from cursor to glyph left edge.
     * @param bearingY vertical offset from baseline (STB Y-down).
     * @param advance  horizontal cursor advance after this glyph.
     */
    public Glyph(Vector2f uvMin, Vector2f uvMax, float width, float height,
                 float bearingX, float bearingY, float advance) {
        this.uvMin = uvMin;
        this.uvMax = uvMax;
        this.width = width;
        this.height = height;
        this.bearingX = bearingX;
        this.bearingY = bearingY;
        this.advance = advance;
    }
}
