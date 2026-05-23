package me.alextzamalis.engine;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * The central window and game-loop manager for TzamalGameEngine.
 *
 * <p>{@code Window} owns the entire GLFW lifecycle: it initialises the
 * library, creates a decorated OS window with an OpenGL 3.3 Core
 * context, installs resize callbacks, drives the main game loop, and
 * tears everything down when the loop exits.</p>
 *
 * <h2>Why a dedicated class?</h2>
 * <p>Raw GLFW calls are verbose, order-sensitive, and easy to get wrong
 * (forgetting {@code glfwMakeContextCurrent} before any GL call, for
 * instance, will silently corrupt state). By encapsulating the entire
 * sequence behind a single {@link #run(GameLifecycle)} entry-point we
 * guarantee correctness and keep the game module completely free of
 * LWJGL imports.</p>
 *
 * <h2>Thread model</h2>
 * <p>GLFW requires that window creation and event polling happen on the
 * <em>main</em> thread (the one that called {@code main()}).  This class
 * therefore expects {@link #run(GameLifecycle)} to be called from
 * {@code main()} directly. The OpenGL context is made current on that
 * same thread, so all rendering also happens there.</p>
 *
 * @author Alexandros Tzamalis
 * @see GameLifecycle
 * @see Input
 */
public class Window {
    // (GameLifecycle): the contract between engine and game
    /**
     * Lifecycle contract that every game must implement.
     *
     * <p>The engine calls these four methods in a well-defined order
     * during the lifetime of the application:</p>
     * <ol>
     *   <li>{@link #init(Window)} once, after the OpenGL context is ready.</li>
     *   <li>{@link #update(float)} once per frame, with the elapsed
     *       time since the previous frame in seconds.</li>
     *   <li>{@link #render()} once per frame, immediately after
     *       {@code update}.  The framebuffer has already been cleared.</li>
     *   <li>{@link #dispose()} once, after the game loop exits
     *       (window closed or escape pressed), before the GL context
     *       is destroyed. Free GPU resources here.</li>
     * </ol>
     *
     * <p><strong>Important:</strong> All four methods are called on the
     * main thread, which also owns the OpenGL context. You can safely
     * issue GL commands from {@code render()} without synchronisation.</p>
     */
    public interface GameLifecycle {

        /**
         * Called exactly once after the OpenGL context has been created
         * and made current.
         *
         * <p>Use this to load textures, compile shaders, set up vertex
         * buffers, or perform any other one-time initialisation that
         * requires a live GL context.</p>
         *
         * @param window the engine Window instance. Store this reference
         *               if you need to call {@link Window#requestClose()}
         *               or query the window dimensions later.
         */
        void init(Window window);

        /**
         * Called once per frame with the time elapsed since the last
         * frame.
         *
         * <p>All game logic, physics, input handling, AI, animation
         * timers belongs here. Avoid issuing draw calls; those go
         * in {@link #render()}.</p>
         *
         * @param deltaTime seconds since the previous frame. On the
         *                  very first frame this will be a small
         *                  positive value (typically &lt; 0.02&nbsp;s).
         *                  Multiply velocities and accelerations by
         *                  this value to achieve frame-rate-independent
         *                  movement.
         */
        void update(float deltaTime);

        /**
         * Called once per frame, immediately after {@link #update(float)}.
         *
         * <p>The engine has already cleared the colour buffer before
         * calling this method, so you can start issuing draw commands
         * right away. The buffer swap happens automatically after
         * this method returns.</p>
         */
        void render();

        /**
         * Called exactly once when the game loop ends, before the
         * OpenGL context and the GLFW window are destroyed.
         *
         * <p>Free any GPU resources you allocated (textures, shaders,
         * VAOs, VBOs) here to avoid native memory leaks. After this
         * method returns, no further GL calls are safe.</p>
         */
        void dispose();
    }

    // Instance state
    private final String title;
    private int width;
    private int height;

    /**
     * The GLFW window handle. A value of {@link org.lwjgl.system.MemoryUtil#NULL NULL}
     * means the window has not been created yet or has already been destroyed.
     */
    private long glfwWindow = NULL;

    // Construction
    /**
     * Prepares a window descriptor without actually creating the OS
     * window or initialising GLFW.
     *
     * <p>The real work happens inside {@link #run(GameLifecycle)},
     * which must be called from the main thread.</p>
     *
     * @param title  text shown in the window title bar.
     *               Must not be {@code null}.
     * @param width  initial framebuffer width in pixels.
     *               Must be &gt; 0.
     * @param height initial framebuffer height in pixels.
     *               Must be &gt; 0.
     * @throws IllegalArgumentException if width or height are not
     *                                  positive, or title is null.
     */
    public Window(String title, int width, int height) {
        if (title == null) {
            throw new IllegalArgumentException("Window title must not be null.");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Window dimensions must be positive.  Got " + width + "x" + height + ".");
        }
        this.title = title;
        this.width = width;
        this.height = height;
    }

    // Public API
    /**
     * Initialises GLFW, creates the window, enters the game loop,
     * and cleans up when the loop ends.
     *
     * <p>This is a blocking call it will not return until
     * the user closes the window or requests an exit. It must be
     * invoked on the main thread because GLFW mandates that window
     * creation and event polling occur there.</p>
     *
     * <h3>Lifecycle sequence</h3>
     * <ol>
     *   <li>GLFW init &amp; window creation</li>
     *   <li>{@link Input#install(long)} registers key/mouse callbacks</li>
     *   <li>{@link GameLifecycle#init(Window)}</li>
     *   <li>Loop: poll events > compute delta > {@code update(dt)} > clear > {@code render()} > swap > {@link Input#endFrame()}</li>
     *   <li>{@link GameLifecycle#dispose()}</li>
     *   <li>GLFW teardown</li>
     * </ol>
     *
     * @param game the game implementation whose lifecycle methods
     *             will be called.  Must not be {@code null}.
     */
    public void run(GameLifecycle game) {
        try {
            initGlfw();
            Input.install(glfwWindow);
            game.init(this);
            loop(game);
        } finally {
            game.dispose();
            cleanup();
        }
    }

    /**
     * Returns the current framebuffer width in pixels.
     *
     * <p>This value is updated automatically by the resize callback,
     * so it always reflects the most recent size even after the
     * user drags the window border.</p>
     *
     * @return framebuffer width, always &gt; 0 while the window exists.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the current framebuffer height in pixels.
     *
     * @return framebuffer height, always &gt; 0 while the window exists.
     * @see #getWidth()
     */
    public int getHeight() {
        return height;
    }

    /**
     * Convenience method returning width&nbsp;/&nbsp;height as a float.
     *
     * <p>Useful when constructing projection matrices for example,
     * {@code new Matrix4f().ortho(0, window.getWidth(), 0, window.getHeight(), -1, 1)}
     * or a perspective projection that needs the aspect ratio to
     * avoid stretching.</p>
     *
     * @return the aspect ratio of the current framebuffer.
     */
    public float getAspectRatio() {
        return (float) width / height;
    }

    /**
     * Returns the raw GLFW window handle.
     *
     * <p>Engine-internal classes (e.g.&nbsp;{@link Input}) occasionally
     * need the handle to install callbacks. <strong>Game code should
     * never call this.</strong></p>
     *
     * @return the GLFW {@code long} handle, or {@code 0L} if the
     *         window has not been created yet.
     */
    public long getHandle() {
        return glfwWindow;
    }

    /**
     * Signals GLFW that this window should close.
     *
     * <p>The game loop will exit on the next iteration. This lets game
     * code request a shutdown without importing GLFW.</p>
     */
    public void requestClose() {
        glfwSetWindowShouldClose(glfwWindow, true);
    }

    //  GLFW initialisation (private!!)
    /**
     * Boots GLFW, sets window hints, creates the window, centres it
     * on the primary monitor, makes the GL context current, enables
     * V-Sync, and creates the GL capabilities object.
     *
     * <p>The window hints request an <strong>OpenGL 3.3 Core</strong>
     * profile. We deliberately avoid the compatibility profile
     * because:</p>
     * <ul>
     *   <li>Core profile disallows deprecated fixed-function calls,
     *       catching mistakes at development time rather than on a
     *       user's machine.</li>
     *   <li>It is the minimum version that supports VAOs, VBOs with
     *       layout qualifiers, and all the GLSL features we need for
     *       batch rendering and shader abstraction.</li>
     * </ul>
     */
    private void initGlfw() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException(
                    "GLFW failed to initialise. Make sure your GPU drivers are up to date!.");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        glfwWindow = glfwCreateWindow(width, height, title, NULL, NULL);
        if (glfwWindow == NULL) {
            throw new RuntimeException(
                    "GLFW could not create a window.  " + "Your GPU may not support OpenGL 3.3 Core.");
        }

        /*
         * Center the window on the primary monitor. We use a
         * MemoryStack-allocated IntBuffer for the framebuffer query
         * to avoid heap allocation.
         */
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(glfwWindow, pWidth, pHeight);

            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidMode != null) {
                glfwSetWindowPos(
                        glfwWindow,
                        (vidMode.width() - pWidth.get(0)) / 2,
                        (vidMode.height() - pHeight.get(0)) / 2);
            }
        }

        glfwMakeContextCurrent(glfwWindow);
        glfwSwapInterval(1); // V-Sync: swap every monitor refresh
        GL.createCapabilities();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glfwSetFramebufferSizeCallback(glfwWindow, (window, w, h) -> {
            this.width = w;
            this.height = h;
            glViewport(0, 0, w, h);
        });

        glfwShowWindow(glfwWindow);
    }

    // /Game loop (private!)
    /**
     * The main game loop.
     *
     * <p>Each iteration:</p>
     * <ol>
     *   <li>Polls OS events (keyboard, mouse, resize).</li>
     *   <li>Computes {@code deltaTime} the wall-clock seconds that
     *       elapsed since the previous iteration. On the first frame
     *       we clamp it to a small value so the game doesn't see a
     *       multi-second spike caused by asset loading in {@code init()}.</li>
     *   <li>Calls {@link GameLifecycle#update(float)}.</li>
     *   <li>Clears the colour and depth buffers.</li>
     *   <li>Calls {@link GameLifecycle#render()}.</li>
     *   <li>Swaps front/back buffers (V-Synced).</li>
     *   <li>Tells {@link Input} to advance its per-frame state.</li>
     * </ol>
     *
     * @param game the active game lifecycle.
     */
    private void loop(GameLifecycle game) {
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        double lastTime = glfwGetTime();

        while (!glfwWindowShouldClose(glfwWindow)) {
            glfwPollEvents();

            double now = glfwGetTime();
            float deltaTime = (float) (now - lastTime);
            lastTime = now;

            /*
             * Clamp excessively large deltas. This can happen on the
             * very first frame (asset loading) or if the OS suspended
             * the process. A capped delta prevents physics explosions
             * and teleporting sprites.
             */
            if (deltaTime > 0.25f) {
                deltaTime = 0.25f;
            }

            game.update(deltaTime);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            game.render();

            glfwSwapBuffers(glfwWindow);
            Input.endFrame();
        }
    }

    // Cleanup (private!!!)
    /**
     * Releases every GLFW resource in the reverse order of creation.
     *
     * <p>Called inside the {@code finally} block of {@link #run(GameLifecycle)}
     * so resources are freed even if the game throws an exception.</p>
     */
    private void cleanup() {
        if (glfwWindow != NULL) {
            glfwFreeCallbacks(glfwWindow);
            glfwDestroyWindow(glfwWindow);
        }
        glfwTerminate();

        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }
}
