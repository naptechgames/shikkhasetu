package bd.bubt.shikkhasetu.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bd.bubt.shikkhasetu.event.DomainEvent;
import bd.bubt.shikkhasetu.event.EventBus;
import bd.bubt.shikkhasetu.model.Enums.Category;
import bd.bubt.shikkhasetu.model.Enums.ItemCondition;
import bd.bubt.shikkhasetu.model.Enums.ItemMode;
import bd.bubt.shikkhasetu.model.Enums.ItemStatus;
import bd.bubt.shikkhasetu.model.ResourceItem;
import bd.bubt.shikkhasetu.model.User;
import bd.bubt.shikkhasetu.repo.Repositories.ItemRepository;
import bd.bubt.shikkhasetu.web.ApiException;

@Service
public class ItemService {

    /** Students never see listings that are waiting for review or were rejected. */
    private static final Set<ItemStatus> PUBLIC_STATUSES = Set.of(
            ItemStatus.AVAILABLE, ItemStatus.RESERVED, ItemStatus.ON_LOAN, ItemStatus.DONATED);

    private final ItemRepository items;
    private final EventBus eventBus;

    public ItemService(ItemRepository items, EventBus eventBus) {
        this.items = items;
        this.eventBus = eventBus;
    }

    /**
     * A student offers an item (donation or loan). It waits for coordinator
     * review. An item added by a coordinator is available immediately.
     */
    @Transactional
    public ResourceItem create(User owner, String title, String description, Category category,
            ItemMode mode, ItemCondition condition) {
        ItemStatus status = owner.isCoordinator() ? ItemStatus.AVAILABLE : ItemStatus.PENDING_REVIEW;
        ResourceItem item = items.save(
                new ResourceItem(title.trim(), description, category, mode, condition, status, owner));
        item.setItemCode(String.format("SS-%05d", item.getId()));
        if (status == ItemStatus.PENDING_REVIEW) {
            eventBus.publish(new DomainEvent(DomainEvent.Type.ITEM_SUBMITTED, item, null, owner));
        }
        return item;
    }

    /** Coordinator accepts or rejects a listing offered by a student. */
    @Transactional
    public ResourceItem review(User actor, Long itemId, boolean approve) {
        if (!actor.isCoordinator()) {
            throw ApiException.forbidden("Only a coordinator can review listings");
        }
        ResourceItem item = items.findByIdForUpdate(itemId)
                .orElseThrow(() -> ApiException.notFound("Item not found"));
        if (item.getStatus() != ItemStatus.PENDING_REVIEW) {
            throw ApiException.conflict("This listing was already reviewed");
        }
        item.setStatus(approve ? ItemStatus.AVAILABLE : ItemStatus.REJECTED);
        eventBus.publish(new DomainEvent(approve ? DomainEvent.Type.ITEM_LISTING_APPROVED
                : DomainEvent.Type.ITEM_LISTING_REJECTED, item, null, actor));
        return item;
    }

    public List<ResourceItem> search(User viewer, String query, Category category, ItemMode mode,
            ItemStatus status) {
        // The catalogue of one club is small, so the filters are applied here in
        // plain Java. Every filter is optional: null means "do not filter".
        String text = (query == null || query.isBlank()) ? null : query.trim().toLowerCase();
        return items.findAllByOrderByIdDesc().stream()
                .filter(item -> viewer.isCoordinator() || PUBLIC_STATUSES.contains(item.getStatus()))
                .filter(item -> category == null || item.getCategory() == category)
                .filter(item -> mode == null || item.getMode() == mode)
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> text == null || contains(item.getTitle(), text)
                        || contains(item.getDescription(), text) || contains(item.getItemCode(), text))
                .toList();
    }

    private static boolean contains(String value, String lowerCaseText) {
        return value != null && value.toLowerCase().contains(lowerCaseText);
    }

    public List<ResourceItem> listOwnedBy(User owner) {
        return items.findByOwnerOrderByIdDesc(owner);
    }
}
