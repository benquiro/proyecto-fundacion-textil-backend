package cl.textilcircular.api.business;

import cl.textilcircular.api.shared.error.ConflictException;

final class BusinessErrors {

    private BusinessErrors() {
    }

    static ConflictException alreadyVoted() {
        return new ConflictException("ALREADY_VOTED", "Ya votaste esta ficha");
    }

    static ConflictException notPendingReview() {
        return new ConflictException("NOT_PENDING_REVIEW", "Esta ficha ya no está pendiente de revisión");
    }

    static ConflictException notPublished() {
        return new ConflictException("NOT_PUBLISHED", "Solo se puede dar de baja una ficha publicada");
    }

    static ConflictException deleted() {
        return new ConflictException("BUSINESS_DELETED", "Esta ficha fue eliminada");
    }
}
