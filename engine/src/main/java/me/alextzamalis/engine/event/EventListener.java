package me.alextzamalis.engine.event;

/**
 * Functional interface for receiving events from the {@link EventBus}.
 *
 * @author Alexandros Tzamalis
 * @see EventBus
 * @see Event
 */
@FunctionalInterface
public interface EventListener {

    /**
     * Called when an event of the subscribed type is published.
     *
     * @param event the published event.
     */
    void onEvent(Event event);
}
