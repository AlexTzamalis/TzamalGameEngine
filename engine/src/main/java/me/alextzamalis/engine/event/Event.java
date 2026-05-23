package me.alextzamalis.engine.event;

/**
 * A lightweight event that carries a type string and can be consumed
 * to stop further propagation.
 *
 * @author Alexandros Tzamalis
 * @see EventBus
 * @see EventListener
 */
public class Event {

    private final String type;
    private boolean consumed;

    /**
     * @param type the event type identifier.
     */
    public Event(String type) {
        this.type = type;
        this.consumed = false;
    }

    /** @return the event type string. */
    public String getType() { return type; }

    /** @return true if this event has been consumed. */
    public boolean isConsumed() { return consumed; }

    /** Marks this event as consumed, stopping further propagation. */
    public void consume() { this.consumed = true; }
}
