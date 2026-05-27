package me.alextzamalis.engine.screen;

import me.alextzamalis.engine.Window;
import me.alextzamalis.engine.event.Event;
import me.alextzamalis.engine.event.EventBus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stack-based screen manager that drives {@link GameScreen} lifecycle.
 *
 * <p>Screens are stored in a stack (Deque). The top screen is the
 * "active" screen that receives update calls. Rendering walks
 * down from the top to find the lowest non-transparent screen,
 * then renders upward so overlays draw on top.</p>
 *
 * @author Alexandros Tzamalis
 * @see GameScreen
 */
public class ScreenManager {

    private final Deque<GameScreen> stack = new ArrayDeque<>();
    private final Set<GameScreen> initialized = new HashSet<>();
    private final Window window;

    /**
     * @param window the engine window, passed to screens on init.
     */
    public ScreenManager(Window window) {
        this.window = window;
    }

    /**
     * Pushes a new screen onto the stack. The previous top screen
     * receives {@code onExit()}, the new screen is initialized (if
     * this is its first time), and then receives {@code onEnter()}.
     *
     * @param screen the screen to push.
     */
    public void pushScreen(GameScreen screen) {
        GameScreen oldTop = stack.peek();
        if (oldTop != null) {
            oldTop.onExit();
        }

        screen.setScreenManager(this);
        stack.push(screen);

        if (!initialized.contains(screen)) {
            screen.init(window);
            initialized.add(screen);
        }

        screen.onEnter();
        EventBus.publish(new Event("screen.pushed"));
    }

    /**
     * Removes the top screen from the stack. It receives
     * {@code onExit()} and {@code dispose()}. The new top screen
     * (if any) receives {@code onEnter()}.
     */
    public void popScreen() {
        if (stack.isEmpty()) {
            return;
        }

        GameScreen top = stack.pop();
        top.onExit();
        top.dispose();
        initialized.remove(top);

        GameScreen newTop = stack.peek();
        if (newTop != null) {
            newTop.onEnter();
        }

        EventBus.publish(new Event("screen.popped"));
    }

    /**
     * Replaces the top screen with a new one. The old top is popped
     * (onExit + dispose), then the new screen is pushed.
     *
     * @param screen the replacement screen.
     */
    public void swapScreen(GameScreen screen) {
        if (!stack.isEmpty()) {
            GameScreen top = stack.pop();
            top.onExit();
            top.dispose();
            initialized.remove(top);
        }

        screen.setScreenManager(this);
        stack.push(screen);

        if (!initialized.contains(screen)) {
            screen.init(window);
            initialized.add(screen);
        }

        screen.onEnter();
        EventBus.publish(new Event("screen.swapped"));
    }

    /**
     * Delegates update to the top (active) screen only.
     *
     * @param dt seconds since the last frame.
     */
    public void update(float dt) {
        GameScreen top = stack.peek();
        if (top != null) {
            top.update(dt);
        }
    }

    /**
     * Renders visible screens from bottom to top. Walks down from
     * the top of the stack to find the first non-transparent screen,
     * then renders from that screen upward.
     */
    public void render() {
        if (stack.isEmpty()) {
            return;
        }

        List<GameScreen> toRender = new ArrayList<>();
        for (GameScreen screen : stack) {
            toRender.add(0, screen);
            if (!screen.isTransparent()) {
                break;
            }
        }

        for (GameScreen screen : toRender) {
            screen.render();
        }
    }

    /** Disposes all screens in the stack and clears it. */
    public void dispose() {
        for (GameScreen screen : stack) {
            screen.dispose();
        }
        stack.clear();
        initialized.clear();
    }

    /** @return the active (top) screen, or null if the stack is empty. */
    public GameScreen getActiveScreen() {
        return stack.peek();
    }

    /** @return true if there are no screens on the stack. */
    public boolean isEmpty() {
        return stack.isEmpty();
    }
}
