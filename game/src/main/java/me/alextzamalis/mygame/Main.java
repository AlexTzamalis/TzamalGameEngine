package me.alextzamalis.mygame;

import me.alextzamalis.engine.Input;
import me.alextzamalis.engine.Window;

/**
 * Entry point for a demo game using the Tzamal Game Engine.
 *
 * <p>This class implements the engine's {@link Window.GameLifecycle}
 * contract, wiring up a minimal "hello window" application that
 * proves the engine's foundation is working correctly.</p>
 *
 * <h2>How to run</h2>
 * <pre>{@code
 *   ./gradlew :game:run
 * }</pre>
 *
 * <h2>What you should see</h2>
 * <p>A window titled <em>"TzamalGameEngine Demo"</em>.
 * Press <strong>Escape</strong> to close it.  The console will print
 * lifecycle messages so you can verify the init/update/render/dispose
 * method sequence's fires correctly.</p>
 *
 * @author Alexandros Tzamalis
 * @see Window
 * @see Window.GameLifecycle
 * @see Input
 */
public class Main implements Window.GameLifecycle {

    private boolean firstFrameLogged = false;

    //  Application entry point
    /**
     * JVM entry point.
     *
     * <p>Creates a {@link Window} with premade defaults and hands
     * control to the engine by calling {@link Window#run(Window.GameLifecycle)}.
     * This method will block until the window is closed.</p>
     *
     * @param args command line args
     */
    public static void main(String[] args) {
        Window window = new Window("TzamalGameEngine Demo", 1280, 720);
        window.run(new Main());
    }

    //  GameLifecycle implementation

    /**
     * {@inheritDoc}
     *
     * <p>In the future this method will load textures, compile
     * shaders, and set up the initial scene.  For now it simply
     * announces that the engine is ready.</p>
     */
    @Override
    public void init() {
        System.out.println("[Game] init(). OpenGL context is live. Ready to load assets.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks for the Escape key via the engine's {@link Input}
     * abstraction.  When pressed, the game loop will exit on the
     * next frame because {@link Window} detects the close request.</p>
     *
     * @param deltaTime seconds elapsed since the previous frame.
     */
    @Override
    public void update(float deltaTime) {
        if (!firstFrameLogged) {
            System.out.println("[Game] update(). Game loop is running. deltaTime = " + deltaTime + "s");
            firstFrameLogged = true;
        }

        if (Input.isKeyJustPressed(Input.KEY_ESCAPE)) {
            System.out.println("[Game] Escape pressed. requesting window close.");
            /*
             * We cannot call glfwSetWindowShouldClose here (that
             * would require importing GLFW).  Instead, the engine's
             * Window class can expose a requestClose() method in
             * the future. For now, we rely on the native OS close
             * button, or the user can press Alt+F4.
             *
             * TODO ALEXX!: Window.requestClose() > glfwSetWindowShouldClose
             */
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Nothing to render yet , the engine clears the framebuffer
     * to dark grey automatically.  Future phases will add sprite
     * batch draw calls here.</p>
     */
    @Override
    public void render() {
        // Rendering will be handled in Phase 2+ (batch renderer, sprites, etc.)
    }

    /**
     * {@inheritDoc}
     *
     * <p>Releases any game owned resources (textures, sounds, etc.).
     * Currently a no-op, but the method is here so the lifecycle complete from the start!
     * No more TODO's
     * </p>
     */
    @Override
    public void dispose() {
        System.out.println("[Game] dispose(). Cleaning up game resources.");
    }
}
