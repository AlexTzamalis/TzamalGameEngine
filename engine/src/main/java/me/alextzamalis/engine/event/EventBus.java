package me.alextzamalis.engine.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A static publish/subscribe event bus.
 *
 * <p>Listeners subscribe to event types by string key. When an event
 * is published, all listeners for that type are notified in
 * registration order. If a listener consumes the event, propagation
 * stops.</p>
 *
 * @author Alexandros Tzamalis
 * @see Event
 * @see EventListener
 */
public final class EventBus {

    private static final Map<String, List<EventListener>> listeners = new HashMap<>();

    private EventBus() {
        // Static utility class.
    }

    /**
     * Subscribes a listener to events of the given type.
     *
     * @param eventType the event type string to listen for.
     * @param listener  the listener to register.
     */
    public static void subscribe(String eventType, EventListener listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    /**
     * Removes a previously registered listener for the given event type.
     *
     * @param eventType the event type string.
     * @param listener  the listener to remove.
     */
    public static void unsubscribe(String eventType, EventListener listener) {
        List<EventListener> list = listeners.get(eventType);
        if (list != null) {
            list.remove(listener);
        }
    }

    /**
     * Publishes an event to all listeners registered for its type.
     * Stops propagation if a listener consumes the event.
     *
     * @param event the event to publish.
     */
    public static void publish(Event event) {
        List<EventListener> list = listeners.get(event.getType());
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            list.get(i).onEvent(event);
            if (event.isConsumed()) {
                break;
            }
        }
    }

    /** Removes all subscriptions. */
    public static void clear() {
        listeners.clear();
    }
}
