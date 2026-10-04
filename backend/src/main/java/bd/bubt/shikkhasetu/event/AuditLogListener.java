package bd.bubt.shikkhasetu.event;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import bd.bubt.shikkhasetu.model.AuditLog;
import bd.bubt.shikkhasetu.repo.Repositories.AuditLogRepository;

/** OBSERVER pattern - a second concrete observer: writes the audit log. */
@Component
public class AuditLogListener implements DomainEventListener {

    private final AuditLogRepository auditLogs;
    private final Clock clock;

    public AuditLogListener(AuditLogRepository auditLogs, Clock clock) {
        this.auditLogs = auditLogs;
        this.clock = clock;
    }

    @Override
    public void onEvent(DomainEvent event) {
        String detail = "item " + event.item().getItemCode()
                + (event.request() == null ? "" : ", request #" + event.request().getId());
        auditLogs.save(new AuditLog(event.actor().getEmail(), event.type().name(), detail,
                LocalDateTime.now(clock)));
    }
}
