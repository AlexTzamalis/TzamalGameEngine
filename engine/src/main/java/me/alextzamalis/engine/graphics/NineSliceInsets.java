package me.alextzamalis.engine.graphics;

/**
 * Border insets for nine-slice UI scaling, in texture pixels within a region.
 */
public final class NineSliceInsets {

    public final int left;
    public final int right;
    public final int top;
    public final int bottom;

    public NineSliceInsets(int left, int right, int top, int bottom) {
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    public static NineSliceInsets uniform(int inset) {
        return new NineSliceInsets(inset, inset, inset, inset);
    }

    public static NineSliceInsets proportional(int regionW, int regionH, float ratio) {
        int h = Math.max(1, Math.round(regionH * ratio));
        int w = Math.max(1, Math.round(regionW * ratio));
        return new NineSliceInsets(w, w, h, h);
    }
}
