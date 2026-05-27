package me.alextzamalis.engine.graphics;

import static org.lwjgl.opengl.GL11.glViewport;

/**
 * Computes a letterboxed viewport for fixed-aspect play content inside a window.
 */
public final class ViewportScaler {

    private int windowWidth;
    private int windowHeight;
    private int contentWidth;
    private int contentHeight;

    private int viewportX;
    private int viewportY;
    private int viewportWidth;
    private int viewportHeight;

    public ViewportScaler(int contentWidth, int contentHeight) {
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;
    }

    public void updateWindowSize(int windowWidth, int windowHeight) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        computeViewport();
    }

    private void computeViewport() {
        if (windowWidth <= 0 || windowHeight <= 0) {
            viewportX = 0;
            viewportY = 0;
            viewportWidth = Math.max(1, windowWidth);
            viewportHeight = Math.max(1, windowHeight);
            return;
        }

        float windowAspect = (float) windowWidth / windowHeight;
        float contentAspect = (float) contentWidth / contentHeight;

        if (windowAspect > contentAspect) {
            viewportHeight = windowHeight;
            viewportWidth = Math.round(windowHeight * contentAspect);
        } else {
            viewportWidth = windowWidth;
            viewportHeight = Math.round(windowWidth / contentAspect);
        }

        viewportX = (windowWidth - viewportWidth) / 2;
        viewportY = (windowHeight - viewportHeight) / 2;
    }

    /** Applies the letterboxed play viewport. */
    public void applyPlayViewport() {
        glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
    }

    /** Restores the full-window viewport for HUD and UI. */
    public void restoreFullViewport() {
        glViewport(0, 0, Math.max(1, windowWidth), Math.max(1, windowHeight));
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }
}
