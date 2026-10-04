package bd.bubt.shikkhasetu.notify;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import bd.bubt.shikkhasetu.model.User;

/**
 * ADAPTER pattern - the "Adapter".
 * Converts a call to NotificationChannel.send(user, message) into the call the
 * external e-mail client understands: deliver(address, subject, htmlBody).
 * It is switched on with app.email.enabled=true.
 */
@Component
public class EmailNotificationAdapter implements NotificationChannel {

    private final ExternalEmailClient emailClient;
    private final boolean enabled;

    public EmailNotificationAdapter(ExternalEmailClient emailClient,
            @Value("${app.email.enabled:false}") boolean enabled) {
        this.emailClient = emailClient;
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void send(User recipient, String message) {
        String htmlBody = "<p>Dear " + recipient.getName() + ",</p><p>" + message + "</p>";
        emailClient.deliver(recipient.getEmail(), "ShikkhaSetu update", htmlBody);
    }
}
