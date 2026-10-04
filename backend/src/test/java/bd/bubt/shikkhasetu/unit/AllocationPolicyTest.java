package bd.bubt.shikkhasetu.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import bd.bubt.shikkhasetu.TestData;
import bd.bubt.shikkhasetu.allocation.AllocationPolicy;
import bd.bubt.shikkhasetu.model.Enums.ItemMode;
import bd.bubt.shikkhasetu.model.Enums.ItemStatus;
import bd.bubt.shikkhasetu.model.Enums.Role;
import bd.bubt.shikkhasetu.model.ResourceItem;
import bd.bubt.shikkhasetu.model.ResourceRequest;
import bd.bubt.shikkhasetu.model.User;

/** UNIT tests - Strategy pattern (allocation policies). */
class AllocationPolicyTest {

    private final User coordinator = TestData.user(1, Role.COORDINATOR);
    private final ResourceItem item = TestData.item(10, ItemMode.LOAN, ItemStatus.AVAILABLE, coordinator);
    private final ResourceRequest oldest = TestData.request(100, item, TestData.user(2, Role.STUDENT), 7);
    private final ResourceRequest newest = TestData.request(101, item, TestData.user(3, Role.STUDENT), 7);
    private final List<ResourceRequest> pending = List.of(oldest, newest);

    @Test
    void firstComeFirstServed_picksTheOldestRequest() {
        assertEquals(oldest, new AllocationPolicy.FirstComeFirstServed().select(pending, null));
    }

    @Test
    void firstComeFirstServed_ignoresTheCoordinatorChoice() {
        assertEquals(oldest, new AllocationPolicy.FirstComeFirstServed().select(pending, 101L));
    }

    @Test
    void firstComeFirstServed_returnsNullWhenNobodyIsWaiting() {
        assertNull(new AllocationPolicy.FirstComeFirstServed().select(List.of(), null));
    }

    @Test
    void coordinatorChoice_picksTheChosenRequest() {
        assertEquals(newest, new AllocationPolicy.CoordinatorChoice().select(pending, 101L));
    }

    @Test
    void coordinatorChoice_returnsNullWhenTheChosenRequestIsNotPending() {
        assertNull(new AllocationPolicy.CoordinatorChoice().select(pending, 999L));
    }
}
