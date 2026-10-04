package bd.bubt.shikkhasetu.whitebox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import bd.bubt.shikkhasetu.TestData;
import bd.bubt.shikkhasetu.allocation.AllocationPolicy;
import bd.bubt.shikkhasetu.event.DomainEvent;
import bd.bubt.shikkhasetu.event.EventBus;
import bd.bubt.shikkhasetu.model.Enums.ItemMode;
import bd.bubt.shikkhasetu.model.Enums.ItemStatus;
import bd.bubt.shikkhasetu.model.Enums.RequestStatus;
import bd.bubt.shikkhasetu.model.Enums.Role;
import bd.bubt.shikkhasetu.model.ResourceItem;
import bd.bubt.shikkhasetu.model.ResourceRequest;
import bd.bubt.shikkhasetu.model.User;
import bd.bubt.shikkhasetu.repo.Repositories.ItemRepository;
import bd.bubt.shikkhasetu.repo.Repositories.RequestRepository;
import bd.bubt.shikkhasetu.service.RequestService;
import bd.bubt.shikkhasetu.web.ApiException;

/**
 * WHITE-BOX / BASIS-PATH tests for RequestService.allocate().
 *
 * The method has 4 decisions, so cyclomatic complexity V(G) = 4 + 1 = 5 and
 * the basis set has 5 independent paths. One test per path. The control-flow
 * graph and the path table are in docs/TESTING.md.
 */
class AllocateBasisPathTest {

    private static final long ITEM_ID = 10L;

    private final User coordinator = TestData.user(1, Role.COORDINATOR);
    private final User student = TestData.user(2, Role.STUDENT);
    private final AllocationPolicy policy = new AllocationPolicy.FirstComeFirstServed();

    private ItemRepository items;
    private RequestRepository requests;
    private List<DomainEvent> published;
    private RequestService service;

    @BeforeEach
    void setUp() {
        items = mock(ItemRepository.class);
        requests = mock(RequestRepository.class);
        published = new ArrayList<>();
        service = new RequestService(requests, items, new EventBus(List.of(published::add)),
                Clock.systemDefaultZone());
    }

    /** Path 1: decision 1 true -> actor is not a coordinator. */
    @Test
    void path1_studentIsRefused() {
        ApiException e = assertThrows(ApiException.class,
                () -> service.allocate(student, ITEM_ID, policy, null));

        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        verifyNoInteractions(items, requests); // the database was never touched
    }

    /** Path 2: decision 1 false, decision 2 true -> item does not exist. */
    @Test
    void path2_unknownItem() {
        when(items.findByIdForUpdate(ITEM_ID)).thenReturn(Optional.empty());

        ApiException e = assertThrows(ApiException.class,
                () -> service.allocate(coordinator, ITEM_ID, policy, null));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    /** Path 3: decisions 1,2 false, decision 3 true -> item is not AVAILABLE. */
    @Test
    void path3_itemAlreadyAllocated() {
        ResourceItem item = TestData.item(ITEM_ID, ItemMode.LOAN, ItemStatus.RESERVED, coordinator);
        when(items.findByIdForUpdate(ITEM_ID)).thenReturn(Optional.of(item));

        ApiException e = assertThrows(ApiException.class,
                () -> service.allocate(coordinator, ITEM_ID, policy, null));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals(ItemStatus.RESERVED, item.getStatus());
    }

    /** Path 4: decisions 1,2,3 false, decision 4 true -> the policy finds no request. */
    @Test
    void path4_noPendingRequest() {
        ResourceItem item = TestData.item(ITEM_ID, ItemMode.LOAN, ItemStatus.AVAILABLE, coordinator);
        when(items.findByIdForUpdate(ITEM_ID)).thenReturn(Optional.of(item));
        when(requests.findByItemAndStatusOrderByCreatedAtAscIdAsc(item, RequestStatus.PENDING))
                .thenReturn(List.of());

        ApiException e = assertThrows(ApiException.class,
                () -> service.allocate(coordinator, ITEM_ID, policy, null));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertEquals(ItemStatus.AVAILABLE, item.getStatus());
    }

    /** Path 5: all decisions false -> the request is approved and the item reserved. */
    @Test
    void path5_successfulAllocation() {
        ResourceItem item = TestData.item(ITEM_ID, ItemMode.LOAN, ItemStatus.AVAILABLE, coordinator);
        ResourceRequest request = TestData.request(100, item, student, 7);
        when(items.findByIdForUpdate(ITEM_ID)).thenReturn(Optional.of(item));
        when(requests.findByItemAndStatusOrderByCreatedAtAscIdAsc(item, RequestStatus.PENDING))
                .thenReturn(List.of(request));

        ResourceRequest result = service.allocate(coordinator, ITEM_ID, policy, null);

        assertEquals(request, result);
        assertEquals(ItemStatus.RESERVED, item.getStatus());
        assertEquals(RequestStatus.APPROVED, request.getStatus());
        assertTrue(request.getPickupCode().matches("\\d{6}"));
        assertEquals(1, published.size());
        assertEquals(DomainEvent.Type.REQUEST_APPROVED, published.get(0).type());
    }
}
