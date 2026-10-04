package bd.bubt.shikkhasetu.event;

import java.util.List;

import org.springframework.stereotype.Component;

import bd.bubt.shikkhasetu.model.Enums.Role;
import bd.bubt.shikkhasetu.model.ResourceItem;
import bd.bubt.shikkhasetu.model.User;
import bd.bubt.shikkhasetu.notify.NotificationService;
import bd.bubt.shikkhasetu.repo.Repositories.UserRepository;

/** OBSERVER pattern - a concrete observer: turns events into notifications. */
@Component
public class NotificationListener implements DomainEventListener {

    private final NotificationService notifications;
    private final UserRepository users;

    public NotificationListener(NotificationService notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @Override
    public void onEvent(DomainEvent event) {
        ResourceItem item = event.item();
        String title = "\"" + item.getTitle() + "\" (" + item.getItemCode() + ")";
        User owner = item.getOwner();
        User requester = event.request() == null ? null : event.request().getRequester();

        switch (event.type()) {
            case ITEM_SUBMITTED ->
                toCoordinators(owner.getName() + " offered " + title + ". Please review the listing.");
            case ITEM_LISTING_APPROVED ->
                notifications.send(owner, "Your listing " + title + " was approved and is now visible.");
            case ITEM_LISTING_REJECTED ->
                notifications.send(owner, "Your listing " + title + " was not accepted.");
            case REQUEST_SUBMITTED ->
                toCoordinators(requester.getName() + " requested " + title + ".");
            case REQUEST_APPROVED ->
                notifications.send(requester, "Your request for " + title
                        + " was approved. Show your pickup code to the coordinator.");
            case REQUEST_REJECTED ->
                notifications.send(requester, "Your request for " + title + " was not approved.");
            case REQUEST_CANCELLED ->
                toCoordinators("The request of " + requester.getName() + " for " + title + " was cancelled.");
            case ITEM_HANDED_OVER -> {
                notifications.send(requester, "You received " + title + "."
                        + (event.request().getDueDate() == null ? ""
                                : " Please return it by " + event.request().getDueDate() + "."));
                if (!owner.getId().equals(requester.getId())) {
                    notifications.send(owner, "Your item " + title + " was handed over to a student.");
                }
            }
            case ITEM_RETURNED ->
                notifications.send(requester, "Return of " + title + " was recorded. Thank you!");
        }
    }

    private void toCoordinators(String message) {
        List<User> coordinators = users.findByRole(Role.COORDINATOR);
        for (User coordinator : coordinators) {
            notifications.send(coordinator, message);
        }
    }
}
