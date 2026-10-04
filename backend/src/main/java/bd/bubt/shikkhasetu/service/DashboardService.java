package bd.bubt.shikkhasetu.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import bd.bubt.shikkhasetu.model.Enums.ItemMode;
import bd.bubt.shikkhasetu.model.Enums.ItemStatus;
import bd.bubt.shikkhasetu.model.Enums.RequestStatus;
import bd.bubt.shikkhasetu.model.ResourceRequest;
import bd.bubt.shikkhasetu.repo.Repositories.ItemRepository;
import bd.bubt.shikkhasetu.repo.Repositories.RequestRepository;

/** Impact numbers. Only confirmed handovers are counted, nothing is estimated. */
@Service
public class DashboardService {

    public record Dashboard(long availableItems, long pendingRequests, long completedDonations,
            long activeLoans, long completedLoans, long overdueLoans, long uniqueRecipients) {
    }

    private final ItemRepository items;
    private final RequestRepository requests;
    private final Clock clock;

    public DashboardService(ItemRepository items, RequestRepository requests, Clock clock) {
        this.items = items;
        this.requests = requests;
        this.clock = clock;
    }

    public Dashboard build() {
        LocalDate today = LocalDate.now(clock);
        long pending = 0, donations = 0, activeLoans = 0, completedLoans = 0, overdue = 0;
        Set<Long> recipients = new HashSet<>();

        List<ResourceRequest> all = requests.findAll();
        for (ResourceRequest request : all) {
            boolean isLoan = request.getItem().getMode() == ItemMode.LOAN;
            switch (request.getStatus()) {
                case PENDING -> pending++;
                case HANDED_OVER -> {
                    recipients.add(request.getRequester().getId());
                    if (isLoan) {
                        activeLoans++;
                        if (request.isOverdue(today)) {
                            overdue++;
                        }
                    } else {
                        donations++;
                    }
                }
                case RETURNED -> {
                    recipients.add(request.getRequester().getId());
                    completedLoans++;
                }
                default -> { }
            }
        }
        return new Dashboard(items.countByStatus(ItemStatus.AVAILABLE), pending, donations, activeLoans,
                completedLoans, overdue, recipients.size());
    }
}
