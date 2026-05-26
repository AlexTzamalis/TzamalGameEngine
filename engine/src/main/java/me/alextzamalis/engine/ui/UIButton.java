package me.alextzamalis.engine.ui;

import me.alextzamalis.engine.core.Input;
import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.text.Font;
import me.alextzamalis.engine.graphics.text.TextRenderer;
import org.joml.Vector4f;

/**
 * A clickable button with text label and visual states (normal, hovered, pressed).
 *
 * <p>The button performs its own hit-testing using the mouse position
 * from Input. The caller must convert mouse coordinates to the UI's
 * screen-space coordinate system before calling update, or use
 * UIManager which handles this automatically.</p>
 */
public class UIButton extends UIElement {

    private String text;
    private Runnable onClick;

    private final Vector4f normalColor;
    private final Vector4f hoverColor;
    private final Vector4f pressColor;
    private final Vector4f textColor;

    private boolean hovered;
    private boolean pressed;

    // Mouse position in UI screen-space, set by UIManager each frame
    float uiMouseX;
    float uiMouseY;

    public UIButton(float x, float y, float width, float height, String text) {
        super(x, y, width, height);
        this.text = text;
        this.normalColor = new Vector4f(0.3f, 0.3f, 0.3f, 0.9f);
        this.hoverColor = new Vector4f(0.4f, 0.4f, 0.5f, 0.95f);
        this.pressColor = new Vector4f(0.2f, 0.2f, 0.3f, 1.0f);
        this.textColor = new Vector4f(1f, 1f, 1f, 1f);
    }

    @Override
    public void update(float dt) {
        if (!visible || !enabled) {
            hovered = false;
            pressed = false;
            return;
        }

        hovered = containsPoint(uiMouseX, uiMouseY);
        pressed = hovered && Input.isMouseButtonPressed(Input.MOUSE_BUTTON_LEFT);

        if (hovered && Input.isMouseButtonJustPressed(Input.MOUSE_BUTTON_LEFT)) {
            if (onClick != null && !Input.isImGuiCapturingMouse()) {
                onClick.run();
            }
        }
    }

    @Override
    public void render(BatchRenderer batch, Font font) {
        if (!visible) return;

        Vector4f bg;
        if (pressed) {
            bg = pressColor;
        } else if (hovered) {
            bg = hoverColor;
        } else {
            bg = normalColor;
        }

        batch.drawQuad(x, y, width, height, bg);

        if (text != null && !text.isEmpty() && font != null) {
            float textX = x + width / 2f;
            float textY = y + height / 2f;
            TextRenderer.drawTextFullyCentered(batch, font, text, textX, textY, textColor);
        }
    }

    public void setOnClick(Runnable onClick) { this.onClick = onClick; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Vector4f getNormalColor() { return normalColor; }
    public Vector4f getHoverColor() { return hoverColor; }
    public Vector4f getPressColor() { return pressColor; }
    public Vector4f getTextColor() { return textColor; }
    public boolean isHovered() { return hovered; }
    public boolean isPressed() { return pressed; }
}
