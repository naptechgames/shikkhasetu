package bd.bubt.shikkhasetu.workflow;

import java.time.LocalDate;

import bd.bubt.shikkhasetu.model.Enums.ItemCondition;
import bd.bubt.shikkhasetu.model.ResourceRequest;

/**
 * FACTORY METHOD pattern - the "Product".
 * The rules that are different for a loan and for a donation.
 */
public interface RequestWorkflow {

    /** Checks the loan period of a new request and returns the value to store. */
    Integer validateLoanDays(Integer loanDays);

    /** The item is given to the student. */
    void handOver(ResourceRequest request, LocalDate today);

    /** The item comes back to the club. */
    void returnItem(ResourceRequest request, ItemCondition condition);
}
