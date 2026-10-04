package bd.bubt.shikkhasetu.model;

import bd.bubt.shikkhasetu.model.Enums.Category;
import bd.bubt.shikkhasetu.model.Enums.ItemCondition;
import bd.bubt.shikkhasetu.model.Enums.ItemMode;
import bd.bubt.shikkhasetu.model.Enums.ItemStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** One physical book, calculator or other resource. */
@Entity
@Table(name = "resource_items")
public class ResourceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human readable unique identifier written on the item, e.g. SS-00012. */
    @Column(unique = true)
    private String itemCode;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false)
    private ItemCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatus status;

    @ManyToOne(optional = false)
    private User owner;

    /**
     * Optimistic lock. If two transactions change the same item, the second
     * one fails instead of silently overwriting the first.
     */
    @Version
    private long version;

    protected ResourceItem() {
    }

    public ResourceItem(String title, String description, Category category, ItemMode mode,
            ItemCondition condition, ItemStatus status, User owner) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.mode = mode;
        this.condition = condition;
        this.status = status;
        this.owner = owner;
    }

    public Long getId() { return id; }
    public String getItemCode() { return itemCode; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public ItemMode getMode() { return mode; }
    public ItemCondition getCondition() { return condition; }
    public ItemStatus getStatus() { return status; }
    public User getOwner() { return owner; }

    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public void setStatus(ItemStatus status) { this.status = status; }
    public void setCondition(ItemCondition condition) { this.condition = condition; }
}
