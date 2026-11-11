package cl.textilcircular.api.core.model.account;

public enum AccountStatus {
    /** Can use the platform. */
    ACTIVE,
    /** Paused by its owner; logging in again reactivates it. */
    DEACTIVATED,
    /** Blocked by an administrator. */
    SUSPENDED,
    /** Closed by its owner; personal data was anonymized. */
    DELETED
}
