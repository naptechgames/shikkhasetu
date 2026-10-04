package bd.bubt.shikkhasetu.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import bd.bubt.shikkhasetu.model.Enums.ItemCondition;
import bd.bubt.shikkhasetu.model.Enums.RequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A student's request for one item. To keep the model small, the allocation
 * (approval + pickup code) and the loan (due date + return) are stored on the
 * request itself instead of in separate tables.
 */
@Entity
@Table(name = "resource_requests")
public class ResourceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private ResourceItem item;

    @ManyToOne(optional = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    /** Only for loans: how many days the student wants the item. */
    private Integer loanDays;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** Single-use code shown to the requester after approval. */
    private String pickupCode;

    private LocalDateTime handedOverAt;
    private LocalDate dueDate;
    private LocalDateTime returnedAt;

    @Enumerated(EnumType.STRING)
    private ItemCondition returnCondition;

    protected ResourceRequest() {
    }

    public ResourceRequest(ResourceItem item, User requester, Integer loanDays, String note,
            LocalDateTime createdAt) {
        this.item = item;
        this.requester = requester;
        this.loanDays = loanDays;
        this.note = note;
        this.createdAt = createdAt;
        this.status = RequestStatus.PENDING;
    }

    public Long getId() { return id; }
    public ResourceItem getItem() { return item; }
    public User getRequester() { return requester; }
    public RequestStatus getStatus() { return status; }
    public Integer getLoanDays() { return loanDays; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getPickupCode() { return pickupCode; }
    public LocalDateTime getHandedOverAt() { return handedOverAt; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public ItemCondition getReturnCondition() { return returnCondition; }

    public void setStatus(RequestStatus status) { this.status = status; }
    public void setPickupCode(String pickupCode) { this.pickupCode = pickupCode; }
    public void setHandedOverAt(LocalDateTime handedOverAt) { this.handedOverAt = handedOverAt; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }
    public void setReturnCondition(ItemCondition returnCondition) { this.returnCondition = returnCondition; }

    /** Overdue is not stored. It is derived from the due date of an unreturned loan. */
    public boolean isOverdue(LocalDate today) {
        return status == RequestStatus.HANDED_OVER && dueDate != null && dueDate.isBefore(today);
    }
}
