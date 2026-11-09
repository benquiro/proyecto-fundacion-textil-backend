package cl.textilcircular.api.core.error;

import lombok.Getter;

/**
 * The request clashes with the current state: a duplicate key, a repeated vote or an
 * operation that the entity's status does not allow. {@code code} is a stable identifier.
 */
@Getter
public class ConflictException extends DomainException {

    private final String code;

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }
}
