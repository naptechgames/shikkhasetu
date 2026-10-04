package bd.bubt.shikkhasetu;

import java.time.LocalDateTime;

import org.springframework.test.util.ReflectionTestUtils;

import bd.bubt.shikkhasetu.model.Enums.Category;
import bd.bubt.shikkhasetu.model.Enums.ItemCondition;
import bd.bubt.shikkhasetu.model.Enums.ItemMode;
import bd.bubt.shikkhasetu.model.Enums.ItemStatus;
import bd.bubt.shikkhasetu.model.Enums.Role;
import bd.bubt.shikkhasetu.model.ResourceItem;
import bd.bubt.shikkhasetu.model.ResourceRequest;
import bd.bubt.shikkhasetu.model.User;

/** Builds in-memory objects for unit tests (no database). */
public final class TestData {

    private TestData() {
    }

    public static User user(long id, Role role) {
        User user = new User("User " + id, "user" + id + "@test.local", "hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static ResourceItem item(long id, ItemMode mode, ItemStatus status, User owner) {
        ResourceItem item = new ResourceItem("Item " + id, "test item", Category.BOOK, mode,
                ItemCondition.GOOD, status, owner);
        ReflectionTestUtils.setField(item, "id", id);
        item.setItemCode(String.format("SS-%05d", id));
        return item;
    }

    public static ResourceRequest request(long id, ResourceItem item, User requester, Integer loanDays) {
        ResourceRequest request = new ResourceRequest(item, requester, loanDays, null,
                LocalDateTime.of(2026, 10, 1, 9, 0).plusMinutes(id));
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }
}
