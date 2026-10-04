package bd.bubt.shikkhasetu.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

/**
 * OBSERVER pattern - the "Subject".
 * Services publish events here and do not know who listens. Listeners are
 * called inside the caller's database transaction, so if a listener fails
 * the whole operation is rolled back.
 */
@Component
public class EventBus {

    private final List<DomainEventListener> listeners = new CopyOnWriteArrayList<>();

    /** Spring passes in every DomainEventListener bean (notifications, audit log). */
    public EventBus(List<DomainEventListener> initialListeners) {
        listeners.addAll(initialListeners);
    }

    public void subscribe(DomainEventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(DomainEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(DomainEvent event) {
        for (DomainEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
