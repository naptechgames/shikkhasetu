package bd.bubt.shikkhasetu.workflow;

import java.time.LocalDate;

import bd.bubt.shikkhasetu.model.Enums.ItemCondition;
import bd.bubt.shikkhasetu.model.Enums.ItemStatus;
import bd.bubt.shikkhasetu.model.Enums.RequestStatus;
import bd.bubt.shikkhasetu.model.ResourceRequest;
import bd.bubt.shikkhasetu.web.ApiException;

/** Loan: AVAILABLE -> RESERVED -> ON_LOAN -> AVAILABLE. */
public class LoanWorkflow implements RequestWorkflow {

    public static final int MIN_LOAN_DAYS = 1;
    public static final int MAX_LOAN_DAYS = 30;

    @Override
    public Integer validateLoanDays(Integer loanDays) {
        if (loanDays == null) {
            throw ApiException.badRequest("Loan period (days) is required for a loan");
        }
        if (loanDays < MIN_LOAN_DAYS || loanDays > MAX_LOAN_DAYS) {
            throw ApiException.badRequest(
                    "Loan period must be between " + MIN_LOAN_DAYS + " and " + MAX_LOAN_DAYS + " days");
        }
        return loanDays;
    }

    @Override
    public void handOver(ResourceRequest request, LocalDate today) {
        request.getItem().setStatus(ItemStatus.ON_LOAN);
        request.setStatus(RequestStatus.HANDED_OVER);
        request.setDueDate(today.plusDays(request.getLoanDays()));
    }

    @Override
    public void returnItem(ResourceRequest request, ItemCondition condition) {
        request.getItem().setStatus(ItemStatus.AVAILABLE);
        request.getItem().setCondition(condition);
        request.setStatus(RequestStatus.RETURNED);
        request.setReturnCondition(condition);
    }
}
