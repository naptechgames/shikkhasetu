package bd.bubt.shikkhasetu.allocation;

import java.util.List;

import bd.bubt.shikkhasetu.model.ResourceRequest;

/**
 * STRATEGY pattern - the "Strategy" interface.
 * Decides which pending request receives the item.
 */
public interface AllocationPolicy {

    /**
     * @param pending         pending requests for one item, oldest first
     * @param chosenRequestId the request picked by the coordinator, may be null
     * @return the request that gets the item, or null if none qualifies
     */
    ResourceRequest select(List<ResourceRequest> pending, Long chosenRequestId);

    /** Concrete strategy 1: the oldest pending request wins. */
    class FirstComeFirstServed implements AllocationPolicy {
        @Override
        public ResourceRequest select(List<ResourceRequest> pending, Long chosenRequestId) {
            return pending.isEmpty() ? null : pending.get(0);
        }
    }

    /** Concrete strategy 2: the coordinator picks one specific request. */
    class CoordinatorChoice implements AllocationPolicy {
        @Override
        public ResourceRequest select(List<ResourceRequest> pending, Long chosenRequestId) {
            for (ResourceRequest request : pending) {
                if (request.getId().equals(chosenRequestId)) {
                    return request;
                }
            }
            return null;
        }
    }
}
