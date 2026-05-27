package me.alextzamalis.engine.ui;

import me.alextzamalis.engine.core.Input;
import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.text.Font;
import me.alextzamalis.engine.graphics.text.TextRenderer;
import me.alextzamalis.engine.scene.Sprite;

import org.joml.Vector4f;

/**
 * Clickable toggle that swaps between off and on sprites.
 */
public class UIToggle extends UIElement {

    private boolean value;
    private Sprite offSprite;
    private Sprite onSprite;
    private Runnable onChange;
    private String label;
    private final Vector4f labelColor = new Vector4f(1f, 1f, 1f, 1f);
    private UIScaleMode scaleMode = UIScaleMode.STRETCH;
    private int regionPixelW;
    private int regionPixelH;

    float uiMouseX;
    float uiMouseY;

    public UIToggle(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    public void setSprites(Sprite offSprite, Sprite onSprite) {
        this.offSprite = offSprite;
        this.onSprite = onSprite;
    }

    public void setScaleMode(UIScaleMode scaleMode) {
        this.scaleMode = scaleMode != null ? scaleMode : UIScaleMode.STRETCH;
    }

    public void setRegionPixelSize(int regionPixelW, int regionPixelH) {
        this.regionPixelW = regionPixelW;
        this.regionPixelH = regionPixelH;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    @Override
    public void update(float dt) {
        if (!visible || !enabled) {
            return;
        }
        if (containsPoint(uiMouseX, uiMouseY)
                && Input.isMouseButtonJustPressed(Input.MOUSE_BUTTON_LEFT)
                && !Input.isImGuiCapturingMouse()) {
            value = !value;
            if (onChange != null) {
                onChange.run();
            }
        }
    }

    @Override
    public void render(BatchRenderer batch, Font font) {
        if (!visible) {
            return;
        }
        Sprite sprite = value ? onSprite : offSprite;
        if (sprite != null && sprite.texture != null) {
            Vector4f tint = new Vector4f(1f, 1f, 1f, 1f);
            UISpriteDraw.draw(batch, x, y, width, height, sprite, tint, scaleMode,
                    null, regionPixelW, regionPixelH);
        }
        if (label != null && !label.isEmpty() && font != null) {
            TextRenderer.drawText(batch, font, label, x + width + 8f, y + height * 0.35f, labelColor);
        }
    }
}
