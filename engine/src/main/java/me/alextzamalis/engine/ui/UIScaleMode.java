package me.alextzamalis.engine.ui;

/**
 * How a UI sprite region is scaled to its element bounds.
 */
public enum UIScaleMode {
    /** Stretch the full region to fill width and height. */
    STRETCH,
    /** Nine-slice: corners fixed, edges and center stretch. */
    NINE_SLICE,
    /** Preserve region aspect ratio, letterboxed inside bounds. */
    FIT
}
