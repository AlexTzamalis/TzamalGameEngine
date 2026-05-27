package me.alextzamalis.engine.core;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A static, polling-based input system that abstracts GLFW callbacks
 * into a simple "ask every frame" API.
 *
 * <h2>Design</h2>
 * <p>GLFW delivers input through callbacks: each key press
 * fires a function pointer registered with
 * {@code glfwSetKeyCallback}. While callbacks are great for text
 * input, game logic typically needs to know "is the player holding
 * the right-arrow key right now?" rather than reacting to
 * individual events. {@code Input} bridges this gap by recording
 * callback events into internal arrays and exposing a polling API
 * that game code can query at any point during the update phase.</p>
 *
 * <h2>Dual-buffer edge detection</h2>
 * <p>Two boolean arrays are maintained for keys (and two for mouse
 * buttons):</p>
 * <ul>
 *   <li><strong>current</strong> the state right now (set by
 *       callbacks as they fire).</li>
 *   <li><strong>previous</strong> a snapshot of the state at the
 *       end of the last frame.</li>
 * </ul>
 * <p>By comparing the two we can detect edges:</p>
 * <ul>
 *   <li>{@link #isKeyJustPressed(int)} {@code current[key] &&
 *       !previous[key]} true only on the <em>first</em> frame the
 *       key is held.</li>
 *   <li>{@link #isKeyJustReleased(int)} {@code !current[key] &&
 *       previous[key]} true only on the frame the key goes up.</li>
 * </ul>
 * <p>At the end of every frame, {@link #endFrame()} copies
 * {@code current > previous} so the cycle can repeat.</p>
 *
 * <h2>Key constants</h2>
 * <p>This class re-exports commonly used GLFW key codes as
 * {@code public static final int} constants (e.g.&nbsp;{@link #KEY_ESCAPE},
 * {@link #KEY_SPACE}). The game module should reference these
 * constants instead of importing {@code org.lwjgl.glfw.GLFW}
 * directly, preserving the strict engine/game decoupling rule.</p>
 *
 * <h2>Thread safety</h2>
 * <p>GLFW guarantees that callbacks fire on the same thread that
 * called {@code glfwPollEvents()} which, in our architecture, is
 * the main thread that also runs {@code update()} and
 * {@code render()}. Because everything executes on one thread,
 * no synchronisation is needed.</p>
 *
 * @author Alexandros Tzamalis
 */
public final class Input {

    //  Key-code constants (mirrors of GLFW values)
    //
    //  We expose these so game code never has to import GLFW.
    //  Values are intentionally identical to the GLFW_KEY_* family
    //  so that any future engine helper that does accept raw GLFW
    //  codes will work interchangeably.

    /** Escape key  */
    public static final int KEY_ESCAPE = GLFW_KEY_ESCAPE;

    /** Spacebar */
    public static final int KEY_SPACE = GLFW_KEY_SPACE;

    /** Enter / Return key */
    public static final int KEY_ENTER = GLFW_KEY_ENTER;

    /** Left Shift key */
    public static final int KEY_LEFT_SHIFT = GLFW_KEY_LEFT_SHIFT;

    /** Left Control key */
    public static final int KEY_LEFT_CONTROL = GLFW_KEY_LEFT_CONTROL;

    /* Arrow keys */
    public static final int KEY_UP    = GLFW_KEY_UP;
    public static final int KEY_DOWN  = GLFW_KEY_DOWN;
    public static final int KEY_LEFT  = GLFW_KEY_LEFT;
    public static final int KEY_RIGHT = GLFW_KEY_RIGHT;

    /* WASD and nearby keys */
    public static final int KEY_W = GLFW_KEY_W;
    public static final int KEY_A = GLFW_KEY_A;
    public static final int KEY_S = GLFW_KEY_S;
    public static final int KEY_D = GLFW_KEY_D;
    public static final int KEY_Q = GLFW_KEY_Q;
    public static final int KEY_E = GLFW_KEY_E;

    /* Number row */
    public static final int KEY_0 = GLFW_KEY_0;
    public static final int KEY_1 = GLFW_KEY_1;
    public static final int KEY_2 = GLFW_KEY_2;
    public static final int KEY_3 = GLFW_KEY_3;
    public static final int KEY_4 = GLFW_KEY_4;
    public static final int KEY_5 = GLFW_KEY_5;
    public static final int KEY_6 = GLFW_KEY_6;
    public static final int KEY_7 = GLFW_KEY_7;
    public static final int KEY_8 = GLFW_KEY_8;
    public static final int KEY_9 = GLFW_KEY_9;

    /* Mouse buttons */
    /** Left mouse button */
    public static final int MOUSE_BUTTON_LEFT   = GLFW_MOUSE_BUTTON_LEFT;
    /** Right mouse button */
    public static final int MOUSE_BUTTON_RIGHT  = GLFW_MOUSE_BUTTON_RIGHT;
    /** Middle mouse button (scroll-wheel click) */
    public static final int MOUSE_BUTTON_MIDDLE = GLFW_MOUSE_BUTTON_MIDDLE;

    /** F1 function key */
    public static final int KEY_F1 = GLFW_KEY_F1;

    //  Internal state

    /**
     * GLFW key codes can go up to {@code GLFW_KEY_LAST} (348).
     * We allocate slightly more to avoid off-by-one issues.
     */
    private static final int NUM_KEYS = GLFW_KEY_LAST + 1;

    /**
     * GLFW supports up to {@code GLFW_MOUSE_BUTTON_LAST} (7) buttons.
     */
    private static final int NUM_MOUSE_BUTTONS = GLFW_MOUSE_BUTTON_LAST + 1;

    private static final boolean[] currentKeys  = new boolean[NUM_KEYS];
    private static final boolean[] previousKeys = new boolean[NUM_KEYS];

    private static final boolean[] currentMouseButtons  = new boolean[NUM_MOUSE_BUTTONS];
    private static final boolean[] previousMouseButtons = new boolean[NUM_MOUSE_BUTTONS];

    private static double mouseX;
    private static double mouseY;

    private static double scrollX;
    private static double scrollY;

    private static boolean imguiWantsMouse = false;
    private static boolean imguiWantsKeyboard = false;

    private Input() {
        // Utility class - no instances.
    }

    //  Callback installation (engine-internal)

    /**
     * Registers GLFW input callbacks on the given window.
     *
     * <p>Engine-internal. Game code should not call this.</p>
     *
     * <p>Called once by {@code Window.run()} right
     * after the window has been created and the GL context is current.</p>
     *
     * <p>Four callbacks are installed:</p>
     * <ol>
     *   <li><strong>Key callback</strong> records press/release
     *       into {@code currentKeys}.</li>
     *   <li><strong>Mouse-button callback</strong> records
     *       press/release into {@code currentMouseButtons}.</li>
     *   <li><strong>Cursor-position callback</strong> updates
     *       {@code mouseX}/{@code mouseY}.</li>
     *   <li><strong>Scroll callback</strong> accumulates into
     *       {@code scrollX}/{@code scrollY} (reset each frame).</li>
     * </ol>
     *
     * @param windowHandle the GLFW window handle obtained from
     *                     {@code glfwCreateWindow}.
     */
    public static void install(long windowHandle) {
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key >= 0 && key < NUM_KEYS) {
                currentKeys[key] = (action != GLFW_RELEASE);
            }
        });

        glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
            if (button >= 0 && button < NUM_MOUSE_BUTTONS) {
                currentMouseButtons[button] = (action != GLFW_RELEASE);
            }
        });

        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            mouseX = xpos;
            mouseY = ypos;
        });

        /*
         * Scroll offsets are additive: if the user scrolls twice
         * between frames we want the total, not just the last event.
         * endFrame() resets both accumulators to zero.
         */
        glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) -> {
            scrollX += xoffset;
            scrollY += yoffset;
        });
    }

    //  Per-frame advancement (engine-internal)
    /**
     * Snapshots the current input state into the "previous" buffers
     * and resets per-frame accumulators.
     *
     * <p>Engine-internal. Game code should not call this.</p>
     *
     * <p>Called by {@code Window} at the very end of each game-loop
     * iteration, after buffer swap. This ensures that
     * edge-detection methods like {@link #isKeyJustPressed(int)}
     * compare against exactly one frame's worth of input.</p>
     */
    public static void endFrame() {
        System.arraycopy(currentKeys, 0, previousKeys, 0, NUM_KEYS);
        System.arraycopy(currentMouseButtons, 0, previousMouseButtons, 0, NUM_MOUSE_BUTTONS);
        scrollX = 0.0;
        scrollY = 0.0;
    }

    //  Keyboard polling API
    /**
     * Returns {@code true} while the given key is held down.
     *
     * <p>This is useful for continuous actions like movement. For
     * one-shot actions ( jump) prefer
     * {@link #isKeyJustPressed(int)}.</p>
     *
     * @param keyCode one of the {@code KEY_*} constants defined in
     *                this class ( {@link #KEY_W}).
     * @return {@code true} if the key is currently pressed.
     */
    public static boolean isKeyPressed(int keyCode) {
        return keyCode >= 0 && keyCode < NUM_KEYS && currentKeys[keyCode];
    }

    /**
     * Returns {@code true} only on the first frame in which
     * the key transitions from released to pressed.
     *
     * <p>Perfect for discrete events: opening a menu, firing a
     * bullet, toggling a debug overlay.</p>
     *
     * @param keyCode one of the {@code KEY_*} constants.
     * @return {@code true} on the leading edge of the key press.
     */
    public static boolean isKeyJustPressed(int keyCode) {
        return keyCode >= 0 && keyCode < NUM_KEYS
                && currentKeys[keyCode] && !previousKeys[keyCode];
    }

    /**
     * Returns {@code true} only on the frame in which the key
     * transitions from pressed to released.
     *
     * @param keyCode one of the {@code KEY_*} constants.
     * @return {@code true} on the trailing edge of the key press.
     */
    public static boolean isKeyJustReleased(int keyCode) {
        return keyCode >= 0 && keyCode < NUM_KEYS
                && !currentKeys[keyCode] && previousKeys[keyCode];
    }

    //  Mouse polling API
    /**
     * Returns {@code true} while the given mouse button is held.
     *
     * @param button one of the {@code MOUSE_BUTTON_*} constants
     *               ( {@link #MOUSE_BUTTON_LEFT}).
     * @return {@code true} if the button is currently pressed.
     */
    public static boolean isMouseButtonPressed(int button) {
        return button >= 0 && button < NUM_MOUSE_BUTTONS && currentMouseButtons[button];
    }

    /**
     * Returns {@code true} only on the first frame the mouse button
     * transitions from released to pressed.
     *
     * @param button one of the {@code MOUSE_BUTTON_*} constants.
     * @return {@code true} on the leading edge of the click.
     */
    public static boolean isMouseButtonJustPressed(int button) {
        return button >= 0 && button < NUM_MOUSE_BUTTONS
                && currentMouseButtons[button] && !previousMouseButtons[button];
    }

    /**
     * Returns {@code true} only on the first frame the mouse button
     * transitions from pressed to released.
     */
    public static boolean isMouseButtonJustReleased(int button) {
        return button >= 0 && button < NUM_MOUSE_BUTTONS
                && !currentMouseButtons[button] && previousMouseButtons[button];
    }

    /**
     * Returns the current X coordinate of the cursor in
     * window-space pixels, with the origin at the top-left corner.
     *
     * @return cursor X position.
     */
    public static double getMouseX() {
        return mouseX;
    }

    /**
     * Returns the current Y coordinate of the cursor in
     * window-space pixels, with the origin at the top-left corner.
     *
     * <p><strong>Note:</strong> In OpenGL the Y axis typically points
     * up, but GLFW reports screen-space Y pointing
     * down.  If you need GL coordinates, compute
     * {@code windowHeight - getMouseY()}.</p>
     *
     * @return cursor Y position (top-left origin, Y-down).
     */
    public static double getMouseY() {
        return mouseY;
    }

    /**
     * Returns the horizontal scroll offset accumulated since the
     * last frame.
     *
     * <p>Most mice only have a vertical wheel, so this is typically
     * zero. Trackpads and tilt-wheels can produce horizontal
     * offsets.</p>
     *
     * @return horizontal scroll delta (positive = right).
     */
    public static double getScrollX() {
        return scrollX;
    }

    /**
     * Returns the vertical scroll offset accumulated since the last
     * frame.
     *
     * @return vertical scroll delta (positive = up/away from user).
     */
    public static double getScrollY() {
        return scrollY;
    }

    // ImGui capture state

    /**
     * Called by the ImGui integration layer each frame to update
     * capture state. When ImGui wants the mouse or keyboard, game
     * code should not process those inputs.
     *
     * Engine-internal. Game code should not call this.
     */
    public static void setImGuiCapture(boolean mouse, boolean keyboard) {
        imguiWantsMouse = mouse;
        imguiWantsKeyboard = keyboard;
    }

    /** @return true if ImGui is consuming mouse input this frame. */
    public static boolean isImGuiCapturingMouse() {
        return imguiWantsMouse;
    }

    /** @return true if ImGui is consuming keyboard input this frame. */
    public static boolean isImGuiCapturingKeyboard() {
        return imguiWantsKeyboard;
    }
}
