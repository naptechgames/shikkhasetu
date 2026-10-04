package bd.bubt.shikkhasetu.notify;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bd.bubt.shikkhasetu.model.Notification;
import bd.bubt.shikkhasetu.model.User;
import bd.bubt.shikkhasetu.repo.Repositories.NotificationRepository;
import bd.bubt.shikkhasetu.web.ApiException;

@Service
public class NotificationService {

    private final List<NotificationChannel> channels;
    private final NotificationRepository notifications;

    public NotificationService(List<NotificationChannel> channels, NotificationRepository notifications) {
        this.channels = channels;
        this.notifications = notifications;
    }

    /** Sends the message through every enabled channel. */
    public void send(User recipient, String message) {
        for (NotificationChannel channel : channels) {
            if (channel.isEnabled()) {
                channel.send(recipient, message);
            }
        }
    }

    public List<Notification> listFor(User user) {
        return notifications.findByUserOrderByIdDesc(user);
    }

    @Transactional
    public Notification markRead(User user, Long notificationId) {
        Notification notification = notifications.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("This notification belongs to another user");
        }
        notification.markRead();
        return notification;
    }
}
