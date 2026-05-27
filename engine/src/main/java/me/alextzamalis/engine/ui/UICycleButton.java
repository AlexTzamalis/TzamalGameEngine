package me.alextzamalis.engine.ui;

import me.alextzamalis.engine.core.Input;
import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.NineSliceInsets;
import me.alextzamalis.engine.graphics.text.Font;
import me.alextzamalis.engine.graphics.text.TextRenderer;
import me.alextzamalis.engine.scene.Sprite;

import org.joml.Vector4f;

/**
 * Wide settings row: nine-slice background with centered value text and a cycle affordance.
 */
public class UICycleButton extends UIElement {

    private String text = "";
    private Runnable onClick;

    private Sprite normalSprite;
    private Sprite hoverSprite;
    private Sprite pressSprite;
    private Sprite cycleSprite;

    private UIScaleMode scaleMode = UIScaleMode.NINE_SLICE;
    private NineSliceInsets nineSliceInsets;
    private int regionPixelW;
    private int regionPixelH;

    private final Vector4f textColor = new Vector4f(1f, 1f, 1f, 1f);
    private final Vector4f fallback = new Vector4f(0.25f, 0.35f, 0.5f, 0.95f);

    private boolean hovered;
    private boolean pressed;

    float uiMouseX;
    float uiMouseY;

    public UICycleButton(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    public void setSprites(Sprite normal, Sprite hover, Sprite press) {
        this.normalSprite = normal;
        this.hoverSprite = hover;
        this.pressSprite = press;
    }

    public void setCycleSprite(Sprite cycleSprite) {
        this.cycleSprite = cycleSprite;
    }

    public void setNineSliceInsets(NineSliceInsets insets) {
        this.nineSliceInsets = insets;
    }

    public void setRegionPixelSize(int regionPixelW, int regionPixelH) {
        this.regionPixelW = regionPixelW;
        this.regionPixelH = regionPixelH;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
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
        if (hovered && Input.isMouseButtonJustPressed(Input.MOUSE_BUTTON_LEFT)
                && onClick != null && !Input.isImGuiCapturingMouse()) {
            onClick.run();
        }
    }

    @Override
    public void render(BatchRenderer batch, Font font) {
        if (!visible) {
            return;
        }

        Sprite sprite = normalSprite;
        if (pressed) {
            sprite = pressSprite != null ? pressSprite : hoverSprite;
        } else if (hovered) {
            sprite = hoverSprite != null ? hoverSprite : normalSprite;
        }

        if (sprite != null && sprite.texture != null) {
            UISpriteDraw.draw(batch, x, y, width, height, sprite,
                    new Vector4f(1f, 1f, 1f, 1f), scaleMode, nineSliceInsets,
                    regionPixelW, regionPixelH);
        } else {
            batch.drawQuad(x, y, width, height, fallback);
        }

        float cycleSize = Math.min(height - 8f, 32f);
        if (cycleSprite != null && cycleSprite.texture != null) {
            batch.drawQuad(x + width - cycleSize - 6f, y + (height - cycleSize) / 2f,
                    cycleSize, cycleSize, cycleSprite.texture,
                    new Vector4f(1f, 1f, 1f, 1f), cycleSprite.uvMin, cycleSprite.uvMax);
        }

        if (text != null && !text.isEmpty() && font != null) {
            float textCenterX = x + (width - cycleSize - 12f) / 2f;
            TextRenderer.drawTextFullyCentered(batch, font, text,
                    textCenterX, y + height / 2f, textColor);
        }
    }
}
