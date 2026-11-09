package cl.textilcircular.api.messaging;

public enum ReportStatus {
    OPEN,
    /** An administrator acted on it. */
    RESOLVED,
    /** An administrator found nothing to act on. */
    DISMISSED
}
