package bd.bubt.shikkhasetu.notify;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import bd.bubt.shikkhasetu.model.Notification;
import bd.bubt.shikkhasetu.model.User;
import bd.bubt.shikkhasetu.repo.Repositories.NotificationRepository;

/** Stores the notification in the database; the mobile app shows it. */
@Component
public class InAppNotificationChannel implements NotificationChannel {

    private final NotificationRepository notifications;
    private final Clock clock;

    public InAppNotificationChannel(NotificationRepository notifications, Clock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void send(User recipient, String message) {
        notifications.save(new Notification(recipient, message, LocalDateTime.now(clock)));
    }
}
