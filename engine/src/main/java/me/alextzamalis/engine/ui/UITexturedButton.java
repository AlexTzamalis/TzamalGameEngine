package me.alextzamalis.engine.ui;

import me.alextzamalis.engine.core.Input;
import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.NineSliceInsets;
import me.alextzamalis.engine.graphics.text.Font;
import me.alextzamalis.engine.graphics.text.TextRenderer;
import me.alextzamalis.engine.scene.Sprite;

import org.joml.Vector4f;

/**
 * Button that stretches a normal/hover/press sprite tile with centered text.
 */
public class UITexturedButton extends UIElement {

    private String text;
    private Runnable onClick;

    private Sprite normalSprite;
    private Sprite hoverSprite;
    private Sprite pressSprite;
    private final Vector4f textColor = new Vector4f(1f, 1f, 1f, 1f);
    private final Vector4f fallbackNormal = new Vector4f(0.3f, 0.3f, 0.3f, 0.9f);
    private final Vector4f fallbackHover = new Vector4f(0.4f, 0.4f, 0.5f, 0.95f);
    private final Vector4f fallbackPress = new Vector4f(0.2f, 0.2f, 0.3f, 1f);

    private UIScaleMode scaleMode = UIScaleMode.STRETCH;
    private NineSliceInsets nineSliceInsets;
    private int regionPixelW;
    private int regionPixelH;

    private boolean hovered;
    private boolean pressed;

    float uiMouseX;
    float uiMouseY;

    public UITexturedButton(float x, float y, float width, float height, String text) {
        super(x, y, width, height);
        this.text = text;
    }

    public void setSprites(Sprite normal, Sprite hover, Sprite press) {
        this.normalSprite = normal;
        this.hoverSprite = hover;
        this.pressSprite = press;
    }

    public void setScaleMode(UIScaleMode scaleMode) {
        this.scaleMode = scaleMode != null ? scaleMode : UIScaleMode.STRETCH;
    }

    public void setNineSliceInsets(NineSliceInsets insets) {
        this.nineSliceInsets = insets;
    }

    /** Source region size in pixels (required for NINE_SLICE and FIT). */
    public void setRegionPixelSize(int regionPixelW, int regionPixelH) {
        this.regionPixelW = regionPixelW;
        this.regionPixelH = regionPixelH;
    }

    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public Vector4f getTextColor() {
        return textColor;
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
        if (!visible) {
            return;
        }

        Sprite sprite = normalSprite;
        Vector4f fallback = fallbackNormal;
        if (pressed) {
            sprite = pressSprite != null ? pressSprite : hoverSprite;
            fallback = fallbackPress;
        } else if (hovered) {
            sprite = hoverSprite != null ? hoverSprite : normalSprite;
            fallback = fallbackHover;
        }

        if (sprite != null && sprite.texture != null) {
            Vector4f tint = new Vector4f(1f, 1f, 1f, 1f);
            UISpriteDraw.draw(batch, x, y, width, height, sprite, tint, scaleMode,
                    nineSliceInsets, regionPixelW, regionPixelH);
        } else {
            batch.drawQuad(x, y, width, height, fallback);
        }

        if (text != null && !text.isEmpty() && font != null) {
            TextRenderer.drawTextFullyCentered(batch, font, text,
                    x + width / 2f, y + height / 2f, textColor);
        }
    }
}
