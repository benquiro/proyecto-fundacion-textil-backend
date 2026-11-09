package cl.textilcircular.api.core.error;

import lombok.Getter;

/** A value breaks a rule. {@code field} names the input so the client can mark it. */
@Getter
public class ValidationException extends DomainException {

    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }
}
