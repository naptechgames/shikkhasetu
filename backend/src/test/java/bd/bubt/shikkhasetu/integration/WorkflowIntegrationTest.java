package bd.bubt.shikkhasetu.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import bd.bubt.shikkhasetu.allocation.AllocationPolicy;
import bd.bubt.shikkhasetu.event.DomainEvent;
import bd.bubt.shikkhasetu.event.DomainEventListener;
import bd.bubt.shikkhasetu.event.EventBus;
import bd.bubt.shikkhasetu.model.Enums.Category;
import bd.bubt.shikkhasetu.model.Enums.ItemCondition;
import bd.bubt.shikkhasetu.model.Enums.ItemMode;
import bd.bubt.shikkhasetu.model.Enums.ItemStatus;
import bd.bubt.shikkhasetu.model.Enums.RequestStatus;
import bd.bubt.shikkhasetu.model.Enums.Role;
import bd.bubt.shikkhasetu.model.Notification;
import bd.bubt.shikkhasetu.model.ResourceItem;
import bd.bubt.shikkhasetu.model.ResourceRequest;
import bd.bubt.shikkhasetu.model.User;
import bd.bubt.shikkhasetu.notify.NotificationService;
import bd.bubt.shikkhasetu.repo.Repositories.AuditLogRepository;
import bd.bubt.shikkhasetu.repo.Repositories.ItemRepository;
import bd.bubt.shikkhasetu.repo.Repositories.RequestRepository;
import bd.bubt.shikkhasetu.repo.Repositories.UserRepository;
import bd.bubt.shikkhasetu.service.AuthService;
import bd.bubt.shikkhasetu.service.DashboardService;
import bd.bubt.shikkhasetu.service.ItemService;
import bd.bubt.shikkhasetu.service.RequestService;
import bd.bubt.shikkhasetu.web.ApiException;

/**
 * INTEGRATION tests: services + JPA + a real (in-memory H2) database + the
 * observers. Nothing is mocked.
 */
@SpringBootTest
class WorkflowIntegrationTest {

    @Autowired AuthService authService;
    @Autowired ItemService itemService;
    @Autowired RequestService requestService;
    @Autowired NotificationService notificationService;
    @Autowired DashboardService dashboardService;
    @Autowired EventBus eventBus;
    @Autowired UserRepository users;
    @Autowired ItemRepository items;
    @Autowired RequestRepository requests;
    @Autowired AuditLogRepository auditLogs;

    private User coordinator;

    @BeforeEach
    void findCoordinator() {
        coordinator = users.findByRole(Role.COORDINATOR).get(0); // created by DataSeeder
    }

    private User newStudent() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        return authService.registerStudent("Student " + unique, unique + "@test.local", "secret123");
    }

    private ResourceItem newItem(ItemMode mode) {
        return itemService.create(coordinator, "Test item", "integration test", Category.BOOK, mode,
                ItemCondition.GOOD);
    }

    private ItemStatus statusOf(ResourceItem item) {
        return items.findById(item.getId()).orElseThrow().getStatus();
    }

    private RequestStatus statusOf(ResourceRequest request) {
        return requests.findById(request.getId()).orElseThrow().getStatus();
    }

    private List<String> messagesFor(User user) {
        return notificationService.listFor(user).stream().map(Notification::getMessage).toList();
    }

    @Test
    void approval_reservesItem_notifiesStudent_andWritesAuditLog() {
        User student = newStudent();
        ResourceItem item = newItem(ItemMode.LOAN);
        ResourceRequest request = requestService.create(student, item.getId(), 7, "for exam");
        long auditBefore = auditLogs.count();

        ResourceRequest approved = requestService.approve(coordinator, request.getId());

        assertEquals(RequestStatus.APPROVED, statusOf(request));
        assertEquals(ItemStatus.RESERVED, statusOf(item));
        assertTrue(approved.getPickupCode().matches("\\d{6}"));
        assertTrue(messagesFor(student).stream().anyMatch(m -> m.contains("was approved")));
        assertTrue(auditLogs.count() > auditBefore);
    }

    @Test
    void studentListing_needsCoordinatorReview_beforeItIsVisible() {
        User donor = newStudent();
        User other = newStudent();
        ResourceItem offered = itemService.create(donor, "Offered book", null, Category.BOOK,
                ItemMode.DONATION, ItemCondition.GOOD);

        assertEquals(ItemStatus.PENDING_REVIEW, statusOf(offered));
        assertTrue(itemService.search(other, "Offered book", null, null, null).stream()
                .noneMatch(i -> i.getId().equals(offered.getId())), "hidden from students until reviewed");
        assertThrows(ApiException.class, () -> requestService.create(other, offered.getId(), null, null));

        itemService.review(coordinator, offered.getId(), true);

        assertEquals(ItemStatus.AVAILABLE, statusOf(offered));
        assertTrue(messagesFor(donor).stream().anyMatch(m -> m.contains("was approved")));
    }

    @Test
    void loan_fullLifecycle_requestToReturn() {
        User student = newStudent();
        ResourceItem item = newItem(ItemMode.LOAN);
        long loansBefore = dashboardService.build().completedLoans();

        ResourceRequest request = requestService.create(student, item.getId(), 14, null);
        String code = requestService.approve(coordinator, request.getId()).getPickupCode();

        ApiException wrongCode = assertThrows(ApiException.class,
                () -> requestService.handOver(coordinator, request.getId(), "000000x"));
        assertEquals(HttpStatus.BAD_REQUEST, wrongCode.getStatus());

        ResourceRequest handedOver = requestService.handOver(coordinator, request.getId(), code);
        assertEquals(ItemStatus.ON_LOAN, statusOf(item));
        assertEquals(LocalDate.now().plusDays(14), handedOver.getDueDate());
        assertNull(handedOver.getPickupCode(), "pickup code is single-use");

        requestService.returnItem(coordinator, request.getId(), ItemCondition.FAIR);
        assertEquals(RequestStatus.RETURNED, statusOf(request));
        assertEquals(ItemStatus.AVAILABLE, statusOf(item));
        assertEquals(ItemCondition.FAIR, items.findById(item.getId()).orElseThrow().getCondition());
        assertEquals(loansBefore + 1, dashboardService.build().completedLoans());
    }

    @Test
    void donation_fullLifecycle_andOtherPendingRequestsAreClosed() {
        User first = newStudent();
        User second = newStudent();
        ResourceItem item = newItem(ItemMode.DONATION);
        long donationsBefore = dashboardService.build().completedDonations();

        ResourceRequest winner = requestService.create(first, item.getId(), null, null);
        ResourceRequest waiting = requestService.create(second, item.getId(), null, null);

        // Strategy: first come, first served picks the oldest request.
        ResourceRequest approved = requestService.allocate(coordinator, item.getId(),
                new AllocationPolicy.FirstComeFirstServed(), null);
        assertEquals(winner.getId(), approved.getId());

        requestService.handOver(coordinator, winner.getId(), approved.getPickupCode());

        assertEquals(ItemStatus.DONATED, statusOf(item));
        assertEquals(RequestStatus.HANDED_OVER, statusOf(winner));
        assertEquals(RequestStatus.REJECTED, statusOf(waiting));
        assertEquals(donationsBefore + 1, dashboardService.build().completedDonations());
        ApiException e = assertThrows(ApiException.class,
                () -> requestService.returnItem(coordinator, winner.getId(), ItemCondition.GOOD));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void cancellingAnApprovedRequest_releasesTheReservation() {
        User student = newStudent();
        ResourceItem item = newItem(ItemMode.LOAN);
        ResourceRequest request = requestService.create(student, item.getId(), 3, null);
        requestService.approve(coordinator, request.getId());
        assertEquals(ItemStatus.RESERVED, statusOf(item));

        requestService.cancel(student, request.getId());

        assertEquals(RequestStatus.CANCELLED, statusOf(request));
        assertEquals(ItemStatus.AVAILABLE, statusOf(item));
    }

    @Test
    void secondApprovalForTheSameItem_isRefused() {
        User first = newStudent();
        User second = newStudent();
        ResourceItem item = newItem(ItemMode.LOAN);
        ResourceRequest a = requestService.create(first, item.getId(), 5, null);
        ResourceRequest b = requestService.create(second, item.getId(), 5, null);

        requestService.approve(coordinator, a.getId());
        ApiException e = assertThrows(ApiException.class, () -> requestService.approve(coordinator, b.getId()));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals(RequestStatus.PENDING, statusOf(b));
    }

    /** If a notification handler fails, the approval must be rolled back completely. */
    @Test
    void failureInAnObserver_rollsBackTheWholeApproval() {
        User student = newStudent();
        ResourceItem item = newItem(ItemMode.LOAN);
        ResourceRequest request = requestService.create(student, item.getId(), 7, null);
        int notificationsBefore = messagesFor(student).size();

        DomainEventListener failing = event -> {
            if (event.type() == DomainEvent.Type.REQUEST_APPROVED) {
                throw new IllegalStateException("simulated notification failure");
            }
        };
        eventBus.subscribe(failing);
        try {
            assertThrows(IllegalStateException.class, () -> requestService.approve(coordinator, request.getId()));
        } finally {
            eventBus.unsubscribe(failing);
        }

        assertEquals(RequestStatus.PENDING, statusOf(request));
        assertEquals(ItemStatus.AVAILABLE, statusOf(item));
        assertNull(requests.findById(request.getId()).orElseThrow().getPickupCode());
        assertEquals(notificationsBefore, messagesFor(student).size());
    }

    /**
     * ACCEPTANCE CRITERION: two approvals at the same moment for one item must
     * produce exactly one active allocation. Repeated to make a timing-dependent
     * bug more likely to show.
     */
    @RepeatedTest(10)
    void concurrentApprovals_produceExactlyOneAllocation() throws Exception {
        User first = newStudent();
        User second = newStudent();
        ResourceItem item = newItem(ItemMode.LOAN);
        ResourceRequest a = requestService.create(first, item.getId(), 5, null);
        ResourceRequest b = requestService.create(second, item.getId(), 5, null);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<Boolean> approveA = () -> tryApprove(start, a.getId());
        Callable<Boolean> approveB = () -> tryApprove(start, b.getId());
        Future<Boolean> resultA = pool.submit(approveA);
        Future<Boolean> resultB = pool.submit(approveB);
        start.countDown(); // both threads go at the same time
        boolean okA = resultA.get();
        boolean okB = resultB.get();
        pool.shutdown();

        assertTrue(okA ^ okB, "exactly one approval must succeed, got A=" + okA + " B=" + okB);
        long approved = List.of(a, b).stream().filter(r -> statusOf(r) == RequestStatus.APPROVED).count();
        assertEquals(1, approved);
        assertEquals(ItemStatus.RESERVED, statusOf(item));
        assertNotNull(requests.findById((okA ? a : b).getId()).orElseThrow().getPickupCode());
    }

    private boolean tryApprove(CountDownLatch start, Long requestId) throws InterruptedException {
        start.await();
        try {
            requestService.approve(coordinator, requestId);
            return true;
        } catch (ApiException | org.springframework.dao.ConcurrencyFailureException refused) {
            return false;
        }
    }
}
