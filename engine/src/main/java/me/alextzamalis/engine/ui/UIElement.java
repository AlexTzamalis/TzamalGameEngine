package me.alextzamalis.engine.ui;

import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.text.Font;

/**
 * Base class for all in-game UI elements. Position uses screen-space
 * coordinates (origin at center, Y-up, matching the engine's Camera2D).
 */
public abstract class UIElement {

    protected float x, y, width, height;
    protected boolean visible = true;
    protected boolean enabled = true;

    protected UIElement(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void update(float dt);
    public abstract void render(BatchRenderer batch, Font font);

    /** Tests if a point (screen-space, Y-up) is inside this element's bounds. */
    public boolean containsPoint(float px, float py) {
        return px >= x && px <= x + width
                && py >= y && py <= y + height;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public void setSize(float w, float h) { this.width = w; this.height = h; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
