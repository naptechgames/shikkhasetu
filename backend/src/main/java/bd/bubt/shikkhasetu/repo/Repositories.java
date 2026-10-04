package bd.bubt.shikkhasetu.repo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import bd.bubt.shikkhasetu.model.AuditLog;
import bd.bubt.shikkhasetu.model.AuthToken;
import bd.bubt.shikkhasetu.model.Enums.ItemStatus;
import bd.bubt.shikkhasetu.model.Enums.RequestStatus;
import bd.bubt.shikkhasetu.model.Enums.Role;
import bd.bubt.shikkhasetu.model.Notification;
import bd.bubt.shikkhasetu.model.ResourceItem;
import bd.bubt.shikkhasetu.model.ResourceRequest;
import bd.bubt.shikkhasetu.model.User;
import jakarta.persistence.LockModeType;

/** All Spring Data repositories. Spring generates the implementations. */
public final class Repositories {

    private Repositories() {
    }

    public interface UserRepository extends JpaRepository<User, Long> {
        Optional<User> findByEmailIgnoreCase(String email);

        List<User> findByRole(Role role);
    }

    public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {
    }

    public interface ItemRepository extends JpaRepository<ResourceItem, Long> {

        /**
         * Reads the item with a database row lock (SELECT ... FOR UPDATE).
         * A second transaction that wants the same item must wait until the
         * first one commits. This is the main defence against double allocation.
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select i from ResourceItem i where i.id = :id")
        Optional<ResourceItem> findByIdForUpdate(@Param("id") Long id);

        List<ResourceItem> findAllByOrderByIdDesc();

        List<ResourceItem> findByOwnerOrderByIdDesc(User owner);

        long countByStatus(ItemStatus status);
    }

    public interface RequestRepository extends JpaRepository<ResourceRequest, Long> {
        /** Oldest first, used by the first-come-first-served policy. */
        List<ResourceRequest> findByItemAndStatusOrderByCreatedAtAscIdAsc(ResourceItem item, RequestStatus status);

        /** Reads only the item id, without loading the item into memory. */
        @Query("select r.item.id from ResourceRequest r where r.id = :requestId")
        Optional<Long> findItemIdByRequestId(@Param("requestId") Long requestId);

        List<ResourceRequest> findByRequesterOrderByIdDesc(User requester);

        List<ResourceRequest> findAllByOrderByIdDesc();

        List<ResourceRequest> findByStatusOrderByIdDesc(RequestStatus status);

        boolean existsByItemAndRequesterAndStatusIn(ResourceItem item, User requester,
                Collection<RequestStatus> statuses);
    }

    public interface NotificationRepository extends JpaRepository<Notification, Long> {
        List<Notification> findByUserOrderByIdDesc(User user);
    }

    public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
        List<AuditLog> findTop100ByOrderByIdDesc();
    }
}
