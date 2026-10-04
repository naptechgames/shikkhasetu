package bd.bubt.shikkhasetu.workflow;

import java.time.LocalDate;

import bd.bubt.shikkhasetu.model.Enums.ItemCondition;
import bd.bubt.shikkhasetu.model.Enums.ItemStatus;
import bd.bubt.shikkhasetu.model.Enums.RequestStatus;
import bd.bubt.shikkhasetu.model.ResourceRequest;
import bd.bubt.shikkhasetu.web.ApiException;

/** Donation: AVAILABLE -> RESERVED -> DONATED. There is no due date and no return. */
public class DonationWorkflow implements RequestWorkflow {

    @Override
    public Integer validateLoanDays(Integer loanDays) {
        return null; // a donation has no loan period, any value sent is ignored
    }

    @Override
    public void handOver(ResourceRequest request, LocalDate today) {
        request.getItem().setStatus(ItemStatus.DONATED);
        request.setStatus(RequestStatus.HANDED_OVER);
    }

    @Override
    public void returnItem(ResourceRequest request, ItemCondition condition) {
        throw ApiException.badRequest("A donated item is not returned");
    }
}
