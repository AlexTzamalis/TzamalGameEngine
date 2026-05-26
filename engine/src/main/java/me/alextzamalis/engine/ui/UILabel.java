package me.alextzamalis.engine.ui;

import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.text.Font;
import me.alextzamalis.engine.graphics.text.TextRenderer;
import org.joml.Vector4f;

/**
 * A text label UI element. Renders a string at the element's position.
 */
public class UILabel extends UIElement {

    private String text;
    private final Vector4f color;
    private boolean centered;

    public UILabel(float x, float y, String text, Vector4f color) {
        super(x, y, 0, 0);
        this.text = text;
        this.color = new Vector4f(color);
        this.centered = false;
    }

    @Override
    public void update(float dt) {
        // Labels have no interactive behavior.
    }

    @Override
    public void render(BatchRenderer batch, Font font) {
        if (!visible || text == null || text.isEmpty()) return;

        if (centered) {
            TextRenderer.drawTextCentered(batch, font, text, x, y, color);
        } else {
            TextRenderer.drawText(batch, font, text, x, y, color);
        }
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Vector4f getColor() { return color; }
    public void setColor(float r, float g, float b, float a) { color.set(r, g, b, a); }
    public boolean isCentered() { return centered; }
    public void setCentered(boolean centered) { this.centered = centered; }
}
