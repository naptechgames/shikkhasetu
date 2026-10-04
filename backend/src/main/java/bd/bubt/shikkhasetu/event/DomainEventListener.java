package bd.bubt.shikkhasetu.event;

/** OBSERVER pattern - the "Observer" interface. */
public interface DomainEventListener {

    void onEvent(DomainEvent event);
}
