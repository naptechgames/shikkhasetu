package bd.bubt.shikkhasetu.notify;

import bd.bubt.shikkhasetu.model.User;

/**
 * ADAPTER pattern - the "Target" interface that the application uses.
 * Every way of reaching a user (in-app, e-mail, ...) looks the same to the
 * rest of the code.
 */
public interface NotificationChannel {

    boolean isEnabled();

    void send(User recipient, String message);
}
