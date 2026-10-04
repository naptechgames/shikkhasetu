package bd.bubt.shikkhasetu.model;

/** All small enumerations of the domain, kept in one file so they are easy to find. */
public final class Enums {

    private Enums() {
    }

    public enum Role { STUDENT, COORDINATOR }

    public enum Category { BOOK, CALCULATOR, OTHER }

    /** DONATION = given permanently, LOAN = borrowed and returned. */
    public enum ItemMode { DONATION, LOAN }

    public enum ItemCondition { NEW, GOOD, FAIR, POOR }

    /**
     * Loan:     AVAILABLE -> RESERVED -> ON_LOAN -> AVAILABLE
     * Donation: AVAILABLE -> RESERVED -> DONATED
     * A listing offered by a student starts in PENDING_REVIEW.
     */
    public enum ItemStatus { PENDING_REVIEW, AVAILABLE, RESERVED, ON_LOAN, DONATED, REJECTED }

    public enum RequestStatus { PENDING, APPROVED, REJECTED, CANCELLED, HANDED_OVER, RETURNED }
}
