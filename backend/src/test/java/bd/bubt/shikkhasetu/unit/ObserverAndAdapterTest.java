package bd.bubt.shikkhasetu.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import bd.bubt.shikkhasetu.TestData;
import bd.bubt.shikkhasetu.event.DomainEvent;
import bd.bubt.shikkhasetu.event.DomainEventListener;
import bd.bubt.shikkhasetu.event.EventBus;
import bd.bubt.shikkhasetu.model.Enums.ItemMode;
import bd.bubt.shikkhasetu.model.Enums.ItemStatus;
import bd.bubt.shikkhasetu.model.Enums.Role;
import bd.bubt.shikkhasetu.model.ResourceItem;
import bd.bubt.shikkhasetu.model.User;
import bd.bubt.shikkhasetu.notify.EmailNotificationAdapter;
import bd.bubt.shikkhasetu.notify.ExternalEmailClient;

/** UNIT tests - Observer pattern (EventBus) and Adapter pattern (e-mail). */
class ObserverAndAdapterTest {

    private final User student = TestData.user(2, Role.STUDENT);
    private final ResourceItem item = TestData.item(10, ItemMode.LOAN, ItemStatus.AVAILABLE, student);
    private final DomainEvent event = new DomainEvent(DomainEvent.Type.ITEM_SUBMITTED, item, null, student);

    @Test
    void observer_everySubscriberReceivesThePublishedEvent() {
        List<DomainEvent> first = new ArrayList<>();
        List<DomainEvent> second = new ArrayList<>();
        EventBus bus = new EventBus(List.of(first::add));
        bus.subscribe(second::add);

        bus.publish(event);

        assertEquals(List.of(event), first);
        assertEquals(List.of(event), second);
    }

    @Test
    void observer_unsubscribedListenerReceivesNothing() {
        List<DomainEvent> received = new ArrayList<>();
        DomainEventListener listener = received::add;
        EventBus bus = new EventBus(List.of());
        bus.subscribe(listener);
        bus.unsubscribe(listener);

        bus.publish(event);

        assertTrue(received.isEmpty());
    }

    @Test
    void adapter_translatesANotificationIntoAnEmailDelivery() {
        ExternalEmailClient emailClient = new ExternalEmailClient();
        EmailNotificationAdapter adapter = new EmailNotificationAdapter(emailClient, true);

        adapter.send(student, "Your request was approved.");

        assertEquals(1, emailClient.getOutbox().size());
        String mail = emailClient.getOutbox().get(0);
        assertTrue(mail.contains("To: user2@test.local"));
        assertTrue(mail.contains("Subject: ShikkhaSetu update"));
        assertTrue(mail.contains("Your request was approved."));
    }

    @Test
    void adapter_isDisabledByDefaultSetting() {
        assertFalse(new EmailNotificationAdapter(new ExternalEmailClient(), false).isEnabled());
    }
}
