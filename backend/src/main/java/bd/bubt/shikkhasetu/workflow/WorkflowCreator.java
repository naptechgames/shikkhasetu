package bd.bubt.shikkhasetu.workflow;

import bd.bubt.shikkhasetu.model.Enums.ItemMode;

/**
 * FACTORY METHOD pattern - the "Creator".
 * Subclasses decide which RequestWorkflow object is created. The service
 * only talks to this class and to the RequestWorkflow interface, so it has
 * no "if loan ... else donation" code of its own.
 */
public abstract class WorkflowCreator {

    /** The factory method. */
    protected abstract RequestWorkflow createWorkflow();

    public RequestWorkflow workflow() {
        return createWorkflow();
    }

    /** Picks the creator that belongs to the item's mode. */
    public static WorkflowCreator forMode(ItemMode mode) {
        return mode == ItemMode.LOAN ? new LoanWorkflowCreator() : new DonationWorkflowCreator();
    }

    public static class LoanWorkflowCreator extends WorkflowCreator {
        @Override
        protected RequestWorkflow createWorkflow() {
            return new LoanWorkflow();
        }
    }

    public static class DonationWorkflowCreator extends WorkflowCreator {
        @Override
        protected RequestWorkflow createWorkflow() {
            return new DonationWorkflow();
        }
    }
}
