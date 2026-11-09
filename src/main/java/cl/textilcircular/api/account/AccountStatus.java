package cl.textilcircular.api.account;

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
