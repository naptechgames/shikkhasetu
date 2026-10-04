package bd.bubt.shikkhasetu.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import bd.bubt.shikkhasetu.TestData;
import bd.bubt.shikkhasetu.model.Enums.ItemCondition;
import bd.bubt.shikkhasetu.model.Enums.ItemMode;
import bd.bubt.shikkhasetu.model.Enums.ItemStatus;
import bd.bubt.shikkhasetu.model.Enums.RequestStatus;
import bd.bubt.shikkhasetu.model.Enums.Role;
import bd.bubt.shikkhasetu.model.ResourceItem;
import bd.bubt.shikkhasetu.model.ResourceRequest;
import bd.bubt.shikkhasetu.model.User;
import bd.bubt.shikkhasetu.web.ApiException;
import bd.bubt.shikkhasetu.workflow.DonationWorkflow;
import bd.bubt.shikkhasetu.workflow.LoanWorkflow;
import bd.bubt.shikkhasetu.workflow.RequestWorkflow;
import bd.bubt.shikkhasetu.workflow.WorkflowCreator;

/** UNIT tests - Factory Method pattern, loan-period rule, due date and status transitions. */
class WorkflowTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);

    private final User owner = TestData.user(1, Role.COORDINATOR);
    private final User student = TestData.user(2, Role.STUDENT);

    @Test
    void factoryMethod_createsLoanWorkflowForLoanItems() {
        assertInstanceOf(LoanWorkflow.class, WorkflowCreator.forMode(ItemMode.LOAN).workflow());
    }

    @Test
    void factoryMethod_createsDonationWorkflowForDonationItems() {
        assertInstanceOf(DonationWorkflow.class, WorkflowCreator.forMode(ItemMode.DONATION).workflow());
    }

    // Boundary values of the loan period: valid range is 1..30 days.
    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 15, 29, 30 })
    void loanDays_insideTheRangeAreAccepted(int days) {
        assertEquals(days, new LoanWorkflow().validateLoanDays(days));
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 0, 31, 365 })
    void loanDays_outsideTheRangeAreRefused(int days) {
        assertThrows(ApiException.class, () -> new LoanWorkflow().validateLoanDays(days));
    }

    @Test
    void loanDays_areRequiredForALoan() {
        assertThrows(ApiException.class, () -> new LoanWorkflow().validateLoanDays(null));
    }

    @Test
    void loanDays_areIgnoredForADonation() {
        assertNull(new DonationWorkflow().validateLoanDays(99));
    }

    @Test
    void loanHandover_putsItemOnLoanAndSetsDueDate() {
        ResourceItem item = TestData.item(10, ItemMode.LOAN, ItemStatus.RESERVED, owner);
        ResourceRequest request = TestData.request(100, item, student, 7);

        new LoanWorkflow().handOver(request, TODAY);

        assertEquals(ItemStatus.ON_LOAN, item.getStatus());
        assertEquals(RequestStatus.HANDED_OVER, request.getStatus());
        assertEquals(LocalDate.of(2026, 10, 12), request.getDueDate());
    }

    @Test
    void loanReturn_makesItemAvailableAgainAndRecordsCondition() {
        ResourceItem item = TestData.item(10, ItemMode.LOAN, ItemStatus.ON_LOAN, owner);
        ResourceRequest request = TestData.request(100, item, student, 7);

        new LoanWorkflow().returnItem(request, ItemCondition.FAIR);

        assertEquals(ItemStatus.AVAILABLE, item.getStatus());
        assertEquals(ItemCondition.FAIR, item.getCondition());
        assertEquals(RequestStatus.RETURNED, request.getStatus());
        assertEquals(ItemCondition.FAIR, request.getReturnCondition());
    }

    @Test
    void donationHandover_marksItemDonatedWithoutDueDate() {
        ResourceItem item = TestData.item(11, ItemMode.DONATION, ItemStatus.RESERVED, owner);
        ResourceRequest request = TestData.request(101, item, student, null);

        new DonationWorkflow().handOver(request, TODAY);

        assertEquals(ItemStatus.DONATED, item.getStatus());
        assertEquals(RequestStatus.HANDED_OVER, request.getStatus());
        assertNull(request.getDueDate());
    }

    @Test
    void donationReturn_isNotAllowed() {
        ResourceItem item = TestData.item(11, ItemMode.DONATION, ItemStatus.DONATED, owner);
        ResourceRequest request = TestData.request(101, item, student, null);
        RequestWorkflow workflow = new DonationWorkflow();

        assertThrows(ApiException.class, () -> workflow.returnItem(request, ItemCondition.GOOD));
    }

    // Overdue is derived: handed over, has a due date, and the due date is before today.
    @Test
    void overdue_boundaryAroundTheDueDate() {
        ResourceItem item = TestData.item(10, ItemMode.LOAN, ItemStatus.RESERVED, owner);
        ResourceRequest request = TestData.request(100, item, student, 7);
        new LoanWorkflow().handOver(request, TODAY); // due 2026-10-12

        assertFalse(request.isOverdue(LocalDate.of(2026, 10, 11)), "day before due date");
        assertFalse(request.isOverdue(LocalDate.of(2026, 10, 12)), "on the due date");
        assertTrue(request.isOverdue(LocalDate.of(2026, 10, 13)), "day after due date");
    }

    @Test
    void overdue_isFalseAfterReturn() {
        ResourceItem item = TestData.item(10, ItemMode.LOAN, ItemStatus.RESERVED, owner);
        ResourceRequest request = TestData.request(100, item, student, 7);
        LoanWorkflow workflow = new LoanWorkflow();
        workflow.handOver(request, TODAY);
        workflow.returnItem(request, ItemCondition.GOOD);

        assertFalse(request.isOverdue(LocalDate.of(2027, 1, 1)));
    }
}
