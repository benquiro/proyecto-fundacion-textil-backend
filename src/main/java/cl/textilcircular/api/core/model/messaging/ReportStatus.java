package cl.textilcircular.api.core.model.messaging;

public enum ReportStatus {
    OPEN,
    /** An administrator acted on it. */
    RESOLVED,
    /** An administrator found nothing to act on. */
    DISMISSED
}
