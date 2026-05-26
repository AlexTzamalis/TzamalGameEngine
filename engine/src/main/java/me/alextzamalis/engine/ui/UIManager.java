package me.alextzamalis.engine.ui;

import me.alextzamalis.engine.core.Input;
import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.text.Font;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a collection of UI elements: updates hit-testing with
 * mouse coordinates converted to UI screen-space, and renders all
 * visible elements.
 */
public class UIManager {

    private final List<UIElement> elements = new ArrayList<>();

    public void addElement(UIElement element) {
        elements.add(element);
    }

    public void removeElement(UIElement element) {
        elements.remove(element);
    }

    /**
     * Updates all elements. Converts the window-space mouse position
     * to the UI camera's coordinate system and passes it to buttons
     * for hit-testing.
     *
     * @param dt         delta time in seconds.
     * @param windowWidth  current window width in pixels.
     * @param windowHeight current window height in pixels.
     */
    public void update(float dt, int windowWidth, int windowHeight) {
        float uiMouseX = (float) Input.getMouseX() - windowWidth / 2f;
        float uiMouseY = (windowHeight / 2f) - (float) Input.getMouseY();

        for (int i = 0; i < elements.size(); i++) {
            UIElement el = elements.get(i);
            if (el instanceof UIButton btn) {
                btn.uiMouseX = uiMouseX;
                btn.uiMouseY = uiMouseY;
            }
            propagateMouseToPanel(el, uiMouseX, uiMouseY);
            el.update(dt);
        }
    }

    private void propagateMouseToPanel(UIElement el, float mx, float my) {
        if (el instanceof UIPanel panel) {
            for (UIElement child : panel.getChildren()) {
                if (child instanceof UIButton btn) {
                    btn.uiMouseX = mx;
                    btn.uiMouseY = my;
                }
                propagateMouseToPanel(child, mx, my);
            }
        }
    }

    /**
     * Renders all visible elements using the provided batch renderer.
     * The caller should set up the batch (projection, beginBatch) before
     * calling this, and call endBatch/flush after.
     */
    public void render(BatchRenderer batch, Font font) {
        for (int i = 0; i < elements.size(); i++) {
            elements.get(i).render(batch, font);
        }
    }

    public List<UIElement> getElements() {
        return elements;
    }

    /** Removes all elements. */
    public void clear() {
        elements.clear();
    }
}
