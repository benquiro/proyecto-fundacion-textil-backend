package cl.textilcircular.api.shared.error;

/**
 * Base of every business rule violation. The message is user-facing Spanish text
 * (the same wording the mobile app shows) and the API layer maps each subtype to an HTTP status.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
