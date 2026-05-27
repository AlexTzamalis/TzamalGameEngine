package me.alextzamalis.engine.ui;

import me.alextzamalis.engine.graphics.BatchRenderer;
import me.alextzamalis.engine.graphics.NineSliceInsets;
import me.alextzamalis.engine.graphics.text.Font;
import me.alextzamalis.engine.scene.Sprite;

import org.joml.Vector4f;

/**
 * Draws a textured sprite region at a fixed screen position.
 */
public class UISpriteElement extends UIElement {

    private Sprite sprite;
    private final Vector4f tint = new Vector4f(1f, 1f, 1f, 1f);
    private UIScaleMode scaleMode = UIScaleMode.STRETCH;
    private NineSliceInsets nineSliceInsets;
    private int regionPixelW;
    private int regionPixelH;

    public UISpriteElement(float x, float y, float width, float height, Sprite sprite) {
        super(x, y, width, height);
        this.sprite = sprite;
    }

    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
    }

    public void setScaleMode(UIScaleMode scaleMode) {
        this.scaleMode = scaleMode != null ? scaleMode : UIScaleMode.STRETCH;
    }

    public void setNineSliceInsets(NineSliceInsets insets) {
        this.nineSliceInsets = insets;
    }

    public void setRegionPixelSize(int regionPixelW, int regionPixelH) {
        this.regionPixelW = regionPixelW;
        this.regionPixelH = regionPixelH;
    }

    public Vector4f getTint() {
        return tint;
    }

    @Override
    public void update(float dt) {
    }

    @Override
    public void render(BatchRenderer batch, Font font) {
        if (!visible || sprite == null || sprite.texture == null) {
            return;
        }
        UISpriteDraw.draw(batch, x, y, width, height, sprite, tint, scaleMode,
                nineSliceInsets, regionPixelW, regionPixelH);
    }
}
