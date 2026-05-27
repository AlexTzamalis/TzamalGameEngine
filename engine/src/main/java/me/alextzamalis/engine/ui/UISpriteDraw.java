package me.alextzamalis.engine.ui;

import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.NineSliceInsets;
import me.alextzamalis.engine.scene.Sprite;

import org.joml.Vector4f;

/**
 * Shared sprite drawing for UI widgets (stretch, nine-slice, fit).
 */
public final class UISpriteDraw {

    private UISpriteDraw() {
    }

    public static void draw(BatchRenderer batch, float x, float y, float width, float height,
                            Sprite sprite, Vector4f tint, UIScaleMode mode,
                            NineSliceInsets insets, int regionPixelW, int regionPixelH) {
        if (sprite == null || sprite.texture == null) {
            return;
        }

        switch (mode) {
            case NINE_SLICE -> {
                NineSliceInsets slice = insets != null
                        ? insets
                        : NineSliceInsets.proportional(regionPixelW, regionPixelH, 0.25f);
                batch.drawNineSlice(x, y, width, height, sprite.texture, tint,
                        sprite.uvMin, sprite.uvMax, slice, regionPixelW, regionPixelH);
            }
            case FIT -> drawFit(batch, x, y, width, height, sprite, tint, regionPixelW, regionPixelH);
            default -> batch.drawQuad(x, y, width, height, sprite.texture, tint,
                    sprite.uvMin, sprite.uvMax);
        }
    }

    private static void drawFit(BatchRenderer batch, float x, float y, float width, float height,
                                Sprite sprite, Vector4f tint, int regionPixelW, int regionPixelH) {
        if (regionPixelW <= 0 || regionPixelH <= 0) {
            batch.drawQuad(x, y, width, height, sprite.texture, tint, sprite.uvMin, sprite.uvMax);
            return;
        }
        float aspect = (float) regionPixelW / (float) regionPixelH;
        float drawW = width;
        float drawH = height;
        if (drawW / drawH > aspect) {
            drawW = drawH * aspect;
        } else {
            drawH = drawW / aspect;
        }
        float drawX = x + (width - drawW) * 0.5f;
        float drawY = y + (height - drawH) * 0.5f;
        batch.drawQuad(drawX, drawY, drawW, drawH, sprite.texture, tint, sprite.uvMin, sprite.uvMax);
    }
}
