package me.alextzamalis.engine.ui;

import me.alextzamalis.engine.graphics.BatchRenderer;

import org.joml.Vector4f;

/**
 * Procedural corner-bracket footprint indicator for multi-cell inventory items.
 */
public final class InventoryHitboxDraw {

    private static final float BRACKET_THICK = 2f;
    private static final float BRACKET_LEN = 10f;

    private InventoryHitboxDraw() {
    }

    public static void drawFootprint(BatchRenderer batch, float x, float y,
                                     float width, float height, Vector4f color) {
        float t = BRACKET_THICK;
        float len = Math.min(BRACKET_LEN, Math.min(width, height) * 0.35f);

        batch.drawQuad(x, y + height - t, len, t, color);
        batch.drawQuad(x, y + height - len, t, len, color);

        batch.drawQuad(x + width - len, y + height - t, len, t, color);
        batch.drawQuad(x + width - t, y + height - len, t, len, color);

        batch.drawQuad(x, y, len, t, color);
        batch.drawQuad(x, y, t, len, color);

        batch.drawQuad(x + width - len, y, len, t, color);
        batch.drawQuad(x + width - t, y, t, len, color);
    }
}
