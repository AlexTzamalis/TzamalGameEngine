package me.alextzamalis.engine.ui;

import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.text.Font;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * A rectangular background panel that can contain child UI elements.
 * Children are positioned in world/screen space (not relative to the panel).
 */
public class UIPanel extends UIElement {

    private final Vector4f backgroundColor;
    private final List<UIElement> children = new ArrayList<>();

    public UIPanel(float x, float y, float width, float height, Vector4f backgroundColor) {
        super(x, y, width, height);
        this.backgroundColor = new Vector4f(backgroundColor);
    }

    public void addChild(UIElement child) {
        children.add(child);
    }

    public void removeChild(UIElement child) {
        children.remove(child);
    }

    public List<UIElement> getChildren() {
        return children;
    }

    @Override
    public void update(float dt) {
        if (!visible || !enabled) return;
        for (int i = 0; i < children.size(); i++) {
            children.get(i).update(dt);
        }
    }

    @Override
    public void render(BatchRenderer batch, Font font) {
        if (!visible) return;

        batch.drawQuad(x, y, width, height, backgroundColor);

        for (int i = 0; i < children.size(); i++) {
            children.get(i).render(batch, font);
        }
    }

    public Vector4f getBackgroundColor() { return backgroundColor; }
}
