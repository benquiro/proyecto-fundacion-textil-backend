package cl.textilcircular.api.core.model.business;

public enum BusinessStatus {
    /** Waiting for peer review in the current round. */
    PENDING,
    /** Published in the directory (unless hidden or the owner is not active). */
    APPROVED,
    /** Rejected by a reviewer or taken down by an administrator; editing it starts a new round. */
    REJECTED
}
