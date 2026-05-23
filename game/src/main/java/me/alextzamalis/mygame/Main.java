package me.alextzamalis.mygame;

import me.alextzamalis.engine.Window;
import me.alextzamalis.engine.screen.ScreenManager;

/**
 * Entry point for the TzamalGameEngine demo. Bootstraps the window
 * and delegates all lifecycle calls to a {@link ScreenManager}.
 *
 * @author Alexandros Tzamalis
 */
public class Main implements Window.GameLifecycle {

    private ScreenManager screenManager;

    /**
     * JVM entry point.
     *
     * @param args command line args (unused).
     */
    public static void main(String[] args) {
        Window window = new Window("TzamalGameEngine Demo", 1280, 720);
        window.run(new Main());
    }

    @Override
    public void init(Window window) {
        screenManager = new ScreenManager(window);
        screenManager.pushScreen(new DemoPlayScreen());
    }

    @Override
    public void update(float deltaTime) {
        screenManager.update(deltaTime);
    }

    @Override
    public void render() {
        screenManager.render();
    }

    @Override
    public void dispose() {
        screenManager.dispose();
    }
}
