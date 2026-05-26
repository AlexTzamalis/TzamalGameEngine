package me.alextzamalis.engine.graphics.text;

import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.Texture;
import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * Static utility for rendering text strings through the engine's
 * {@link BatchRenderer}.
 *
 * <p>Each character is drawn as a textured quad using the font's
 * atlas texture and per-glyph UV coordinates. Text participates
 * in the same draw-call batching as sprites, so there is no
 * separate rendering pipeline for text.</p>
 *
 * <h2>Coordinate system</h2>
 * <p>Text is positioned relative to its <strong>baseline</strong>.
 * The baseline is the line that characters sit on; ascenders
 * (like 'b', 'd') extend above it and descenders (like 'g', 'p')
 * extend below it. For most use cases, pass the desired Y position
 * as the baseline and the text will render upward from there.</p>
 *
 * @author Alexandros Tzamalis
 * @see Font
 * @see BatchRenderer
 */
public final class TextRenderer {

    private TextRenderer() {
        // Static utility class.
    }

    /**
     * Renders a string left-aligned at the given baseline position.
     *
     * <p>The batch must already be in an active begin/end cycle.
     * This method submits glyph quads to the batch but does not
     * call beginBatch/endBatch/flush - the caller controls that.</p>
     *
     * @param batch the active batch renderer.
     * @param font  the font to render with.
     * @param text  the string to render.
     * @param x     left edge X position.
     * @param y     baseline Y position.
     * @param color RGBA text color.
     */
    public static void drawText(BatchRenderer batch, Font font, String text,
                                float x, float y, Vector4f color) {
        Texture atlas = font.getAtlasTexture();
        float cursorX = x;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\n') {
                cursorX = x;
                y -= font.getLineHeight();
                continue;
            }

            Glyph g = font.getGlyph(c);

            if (g.width > 0 && g.height > 0) {
                // Position conversion: STB uses Y-down, engine uses Y-up.
                // STB bearingY is negative for glyphs above the baseline.
                // In Y-up: quad bottom = baseline - stbY1 where stbY1 = bearingY + height
                float quadX = cursorX + g.bearingX;
                float quadY = y - (g.bearingY + g.height);
                float quadW = g.width;
                float quadH = g.height;

                batch.drawQuad(quadX, quadY, quadW, quadH,
                        atlas, color, g.uvMin, g.uvMax);
            }

            cursorX += g.advance;
        }
    }

    /**
     * Renders a string horizontally centered on the given point.
     *
     * <p>The text is measured first, then offset so that the center
     * of the string lands on (cx, cy). The Y coordinate is the
     * baseline.</p>
     *
     * @param batch the active batch renderer.
     * @param font  the font to render with.
     * @param text  the string to render.
     * @param cx    center X position.
     * @param cy    baseline Y position (vertical center of the text
     *              is approximately cy + ascent/2).
     * @param color RGBA text color.
     */
    public static void drawTextCentered(BatchRenderer batch, Font font, String text,
                                        float cx, float cy, Vector4f color) {
        float textW = font.textWidth(text);
        drawText(batch, font, text, cx - textW / 2f, cy, color);
    }

    /**
     * Renders a string centered both horizontally and vertically on
     * the given point. The vertical centering accounts for the font's
     * ascent so the visual center of the text aligns with cy.
     *
     * @param batch the active batch renderer.
     * @param font  the font to render with.
     * @param text  the string to render.
     * @param cx    center X position.
     * @param cy    center Y position.
     * @param color RGBA text color.
     */
    public static void drawTextFullyCentered(BatchRenderer batch, Font font, String text,
                                             float cx, float cy, Vector4f color) {
        float textW = font.textWidth(text);
        float baselineY = cy - font.getAscent() / 2f;
        drawText(batch, font, text, cx - textW / 2f, baselineY, color);
    }
}
