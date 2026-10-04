package bd.bubt.shikkhasetu.web;

import java.time.LocalDate;
import java.time.LocalDateTime;

import bd.bubt.shikkhasetu.model.Enums.Category;
import bd.bubt.shikkhasetu.model.Enums.ItemCondition;
import bd.bubt.shikkhasetu.model.Enums.ItemMode;
import bd.bubt.shikkhasetu.model.Notification;
import bd.bubt.shikkhasetu.model.ResourceItem;
import bd.bubt.shikkhasetu.model.ResourceRequest;
import bd.bubt.shikkhasetu.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** JSON shapes: what the app sends (…Body) and what it receives (…Dto). */
public final class Dtos {

    private Dtos() {
    }

    // ---- request bodies (validated on the server) ----

    public record RegisterBody(@NotBlank String name, @NotBlank @Email String email,
            @NotBlank @Size(min = 6, message = "Password must have at least 6 characters") String password) {
    }

    public record LoginBody(@NotBlank String email, @NotBlank String password) {
    }

    public record ItemBody(@NotBlank @Size(max = 120) String title, @Size(max = 1000) String description,
            @NotNull Category category, @NotNull ItemMode mode, @NotNull ItemCondition condition) {
    }

    public record ReviewBody(@NotNull Boolean approve) {
    }

    public record NewRequestBody(@NotNull Long itemId, Integer loanDays, @Size(max = 500) String note) {
    }

    public record HandoverBody(@NotBlank String pickupCode) {
    }

    public record ReturnBody(@NotNull ItemCondition condition) {
    }

    // ---- responses ----

    public record UserDto(Long id, String name, String email, String role) {
        public static UserDto of(User user) {
            return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
        }
    }

    public record LoginDto(String token, UserDto user) {
    }

    public record ItemDto(Long id, String itemCode, String title, String description, String category,
            String mode, String condition, String status, Long ownerId, String ownerName) {
        public static ItemDto of(ResourceItem item) {
            return new ItemDto(item.getId(), item.getItemCode(), item.getTitle(), item.getDescription(),
                    item.getCategory().name(), item.getMode().name(), item.getCondition().name(),
                    item.getStatus().name(), item.getOwner().getId(), item.getOwner().getName());
        }
    }

    public record RequestDto(Long id, ItemDto item, Long requesterId, String requesterName, String status,
            Integer loanDays, String note, LocalDateTime createdAt, String pickupCode,
            LocalDateTime handedOverAt, LocalDate dueDate, LocalDateTime returnedAt, String returnCondition,
            boolean overdue) {

        /** The pickup code is only included when the viewer is the requester. */
        public static RequestDto of(ResourceRequest request, User viewer, LocalDate today) {
            boolean isRequester = request.getRequester().getId().equals(viewer.getId());
            return new RequestDto(request.getId(), ItemDto.of(request.getItem()),
                    request.getRequester().getId(), request.getRequester().getName(),
                    request.getStatus().name(), request.getLoanDays(), request.getNote(),
                    request.getCreatedAt(), isRequester ? request.getPickupCode() : null,
                    request.getHandedOverAt(), request.getDueDate(), request.getReturnedAt(),
                    request.getReturnCondition() == null ? null : request.getReturnCondition().name(),
                    request.isOverdue(today));
        }
    }

    public record NotificationDto(Long id, String message, boolean read, LocalDateTime createdAt) {
        public static NotificationDto of(Notification notification) {
            return new NotificationDto(notification.getId(), notification.getMessage(), notification.isRead(),
                    notification.getCreatedAt());
        }
    }
}
