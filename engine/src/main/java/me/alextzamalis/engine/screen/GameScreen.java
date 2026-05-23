package me.alextzamalis.engine.screen;

import me.alextzamalis.engine.Window;

/**
 * Abstract base class for game screens in a stack-based screen manager.
 *
 * <p>Each screen manages its own rendering resources (BatchRenderer,
 * Camera, etc.) internally. The ScreenManager calls lifecycle methods
 * in the correct order.</p>
 *
 * @author Alexandros Tzamalis
 * @see ScreenManager
 */
public abstract class GameScreen {

    protected ScreenManager screenManager;

    /**
     * Called once when the screen is first pushed onto the stack.
     *
     * @param window the engine window instance.
     */
    public abstract void init(Window window);

    /**
     * Called every frame while this screen is the active (top) screen.
     *
     * @param dt seconds since the last frame.
     */
    public abstract void update(float dt);

    /** Called every frame to render this screen. */
    public abstract void render();

    /** Called when the screen is permanently removed from the stack. */
    public abstract void dispose();

    /**
     * Called each time this screen becomes the top (active) screen.
     * Override to resume music, animations, etc.
     */
    public void onEnter() {}

    /**
     * Called each time this screen is deactivated (another pushed on
     * top, or this screen is removed).
     */
    public void onExit() {}

    /**
     * Return true if screens below this one should also render.
     * Useful for overlays like a pause screen.
     *
     * @return true if transparent, false otherwise.
     */
    public boolean isTransparent() { return false; }

    /**
     * Injected by ScreenManager when the screen is pushed.
     *
     * @param manager the owning screen manager.
     */
    public void setScreenManager(ScreenManager manager) {
        this.screenManager = manager;
    }
}
