package bd.bubt.shikkhasetu.event;

import bd.bubt.shikkhasetu.model.ResourceItem;
import bd.bubt.shikkhasetu.model.ResourceRequest;
import bd.bubt.shikkhasetu.model.User;

/**
 * OBSERVER pattern - the message that is sent to observers.
 * "request" is null for events that are only about an item listing.
 */
public record DomainEvent(Type type, ResourceItem item, ResourceRequest request, User actor) {

    public enum Type {
        ITEM_SUBMITTED, ITEM_LISTING_APPROVED, ITEM_LISTING_REJECTED,
        REQUEST_SUBMITTED, REQUEST_APPROVED, REQUEST_REJECTED, REQUEST_CANCELLED,
        ITEM_HANDED_OVER, ITEM_RETURNED
    }
}
