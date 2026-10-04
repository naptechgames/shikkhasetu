package bd.bubt.shikkhasetu.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bd.bubt.shikkhasetu.allocation.AllocationPolicy;
import bd.bubt.shikkhasetu.event.DomainEvent;
import bd.bubt.shikkhasetu.event.EventBus;
import bd.bubt.shikkhasetu.model.Enums.ItemCondition;
import bd.bubt.shikkhasetu.model.Enums.ItemMode;
import bd.bubt.shikkhasetu.model.Enums.ItemStatus;
import bd.bubt.shikkhasetu.model.Enums.RequestStatus;
import bd.bubt.shikkhasetu.model.ResourceItem;
import bd.bubt.shikkhasetu.model.ResourceRequest;
import bd.bubt.shikkhasetu.model.User;
import bd.bubt.shikkhasetu.repo.Repositories.ItemRepository;
import bd.bubt.shikkhasetu.repo.Repositories.RequestRepository;
import bd.bubt.shikkhasetu.web.ApiException;
import bd.bubt.shikkhasetu.workflow.RequestWorkflow;
import bd.bubt.shikkhasetu.workflow.WorkflowCreator;

/** The request-to-return workflow. Every rule and permission is checked here, on the server. */
@Service
public class RequestService {

    private final RequestRepository requests;
    private final ItemRepository items;
    private final EventBus eventBus;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public RequestService(RequestRepository requests, ItemRepository items, EventBus eventBus, Clock clock) {
        this.requests = requests;
        this.items = items;
        this.eventBus = eventBus;
        this.clock = clock;
    }

    /** A student asks for an item. Several students may ask for the same item. */
    @Transactional
    public ResourceRequest create(User requester, Long itemId, Integer loanDays, String note) {
        if (requester.isCoordinator()) {
            throw ApiException.forbidden("Only students can request items");
        }
        ResourceItem item = items.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("Item not found"));
        if (item.getStatus() != ItemStatus.AVAILABLE) {
            throw ApiException.conflict("This item is not available right now");
        }
        if (item.getOwner().getId().equals(requester.getId())) {
            throw ApiException.badRequest("You cannot request your own item");
        }
        if (requests.existsByItemAndRequesterAndStatusIn(item, requester,
                List.of(RequestStatus.PENDING, RequestStatus.APPROVED))) {
            throw ApiException.conflict("You already have an active request for this item");
        }
        Integer days = workflowFor(item).validateLoanDays(loanDays);
        ResourceRequest request = requests.save(
                new ResourceRequest(item, requester, days, note, LocalDateTime.now(clock)));
        eventBus.publish(new DomainEvent(DomainEvent.Type.REQUEST_SUBMITTED, item, request, requester));
        return request;
    }

    /**
     * Approves one pending request and reserves the item for it.
     *
     * Double allocation is prevented in three steps:
     *  1. the whole method is ONE database transaction;
     *  2. the item row is read with a lock, so a second approval for the same
     *     item waits here until the first one has committed;
     *  3. after waiting, the second approval sees status RESERVED and is refused.
     * (The @Version column on the item is a second safety net.)
     *
     * This method is the subject of the basis-path test: it has 4 decisions,
     * so its cyclomatic complexity is 5. See docs/TESTING.md.
     */
    @Transactional
    public ResourceRequest allocate(User actor, Long itemId, AllocationPolicy policy, Long chosenRequestId) {
        if (!actor.isCoordinator()) {                                             // decision 1
            throw ApiException.forbidden("Only a coordinator can approve requests");
        }
        ResourceItem item = items.findByIdForUpdate(itemId).orElse(null);
        if (item == null) {                                                       // decision 2
            throw ApiException.notFound("Item not found");
        }
        if (item.getStatus() != ItemStatus.AVAILABLE) {                           // decision 3
            throw ApiException.conflict("This item is already allocated to another request");
        }
        List<ResourceRequest> pending =
                requests.findByItemAndStatusOrderByCreatedAtAscIdAsc(item, RequestStatus.PENDING);
        ResourceRequest chosen = policy.select(pending, chosenRequestId);
        if (chosen == null) {                                                     // decision 4
            throw ApiException.badRequest("There is no pending request to approve for this item");
        }
        item.setStatus(ItemStatus.RESERVED);
        chosen.setStatus(RequestStatus.APPROVED);
        chosen.setPickupCode(newPickupCode());
        eventBus.publish(new DomainEvent(DomainEvent.Type.REQUEST_APPROVED, item, chosen, actor));
        return chosen;
    }

    /** Coordinator approves one specific request (coordinator-choice policy). */
    @Transactional
    public ResourceRequest approve(User actor, Long requestId) {
        // Only the item id is read here, so that allocate() is the first to load
        // the item and gets its newest state together with the row lock.
        Long itemId = requests.findItemIdByRequestId(requestId)
                .orElseThrow(() -> ApiException.notFound("Request not found"));
        return allocate(actor, itemId, new AllocationPolicy.CoordinatorChoice(), requestId);
    }

    @Transactional
    public ResourceRequest reject(User actor, Long requestId) {
        requireCoordinator(actor);
        ResourceRequest request = load(requestId);
        if (request.getStatus() != RequestStatus.PENDING) {
            throw ApiException.conflict("Only a pending request can be rejected");
        }
        request.setStatus(RequestStatus.REJECTED);
        eventBus.publish(new DomainEvent(DomainEvent.Type.REQUEST_REJECTED, request.getItem(), request, actor));
        return request;
    }

    /** The requester (or a coordinator) cancels before handover. A reservation is released. */
    @Transactional
    public ResourceRequest cancel(User actor, Long requestId) {
        ResourceRequest request = load(requestId);
        if (!actor.isCoordinator() && !request.getRequester().getId().equals(actor.getId())) {
            throw ApiException.forbidden("You can only cancel your own request");
        }
        if (request.getStatus() == RequestStatus.APPROVED) {
            request.getItem().setStatus(ItemStatus.AVAILABLE);
            request.setPickupCode(null);
        } else if (request.getStatus() != RequestStatus.PENDING) {
            throw ApiException.conflict("This request can no longer be cancelled");
        }
        request.setStatus(RequestStatus.CANCELLED);
        eventBus.publish(new DomainEvent(DomainEvent.Type.REQUEST_CANCELLED, request.getItem(), request, actor));
        return request;
    }

    /** Coordinator gives the item to the student after checking the single-use pickup code. */
    @Transactional
    public ResourceRequest handOver(User actor, Long requestId, String pickupCode) {
        requireCoordinator(actor);
        ResourceRequest request = load(requestId);
        if (request.getStatus() != RequestStatus.APPROVED) {
            throw ApiException.conflict("Only an approved request can be handed over");
        }
        if (pickupCode == null || !pickupCode.trim().equals(request.getPickupCode())) {
            throw ApiException.badRequest("Wrong pickup code");
        }
        ResourceItem item = request.getItem();
        workflowFor(item).handOver(request, LocalDate.now(clock));
        request.setPickupCode(null); // single use
        request.setHandedOverAt(LocalDateTime.now(clock));
        eventBus.publish(new DomainEvent(DomainEvent.Type.ITEM_HANDED_OVER, item, request, actor));

        if (item.getMode() == ItemMode.DONATION) {
            // The item is gone for good, so the other waiting requests are closed.
            for (ResourceRequest other : requests.findByItemAndStatusOrderByCreatedAtAscIdAsc(item,
                    RequestStatus.PENDING)) {
                other.setStatus(RequestStatus.REJECTED);
                eventBus.publish(new DomainEvent(DomainEvent.Type.REQUEST_REJECTED, item, other, actor));
            }
        }
        return request;
    }

    /** Coordinator records that a borrowed item came back and in which condition. */
    @Transactional
    public ResourceRequest returnItem(User actor, Long requestId, ItemCondition condition) {
        requireCoordinator(actor);
        ResourceRequest request = load(requestId);
        if (request.getStatus() != RequestStatus.HANDED_OVER) {
            throw ApiException.conflict("Only a handed-over loan can be returned");
        }
        workflowFor(request.getItem()).returnItem(request, condition);
        request.setReturnedAt(LocalDateTime.now(clock));
        eventBus.publish(new DomainEvent(DomainEvent.Type.ITEM_RETURNED, request.getItem(), request, actor));
        return request;
    }

    public List<ResourceRequest> listMine(User requester) {
        return requests.findByRequesterOrderByIdDesc(requester);
    }

    public List<ResourceRequest> listAll(User actor, RequestStatus status) {
        requireCoordinator(actor);
        return status == null ? requests.findAllByOrderByIdDesc() : requests.findByStatusOrderByIdDesc(status);
    }

    /** A request record is private: only its requester and coordinators may read it. */
    public ResourceRequest get(User viewer, Long requestId) {
        ResourceRequest request = load(requestId);
        if (!viewer.isCoordinator() && !request.getRequester().getId().equals(viewer.getId())) {
            throw ApiException.forbidden("You cannot view another student's request");
        }
        return request;
    }

    private ResourceRequest load(Long requestId) {
        return requests.findById(requestId).orElseThrow(() -> ApiException.notFound("Request not found"));
    }

    private void requireCoordinator(User actor) {
        if (!actor.isCoordinator()) {
            throw ApiException.forbidden("Only a coordinator can do this");
        }
    }

    /** Factory Method in use: the creator decides which workflow object we get. */
    private RequestWorkflow workflowFor(ResourceItem item) {
        return WorkflowCreator.forMode(item.getMode()).workflow();
    }

    private String newPickupCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
