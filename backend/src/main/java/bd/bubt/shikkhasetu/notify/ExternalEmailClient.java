package bd.bubt.shikkhasetu.notify;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * ADAPTER pattern - the "Adaptee".
 * This stands in for the client library of an external e-mail provider. Its
 * method signature is different from NotificationChannel and we pretend we
 * cannot change it.
 *
 * NOTE: this is a STUB. It prints to the console and keeps the messages in a
 * list; it does not send real e-mail. A real provider would replace this class
 * only - nothing else in the application would change.
 */
@Component
public class ExternalEmailClient {

    private final List<String> outbox = new ArrayList<>();

    public synchronized void deliver(String toAddress, String subject, String htmlBody) {
        String line = "To: " + toAddress + " | Subject: " + subject + " | Body: " + htmlBody;
        outbox.add(line);
        System.out.println("[EMAIL STUB] " + line);
    }

    public synchronized List<String> getOutbox() {
        return List.copyOf(outbox);
    }
}
