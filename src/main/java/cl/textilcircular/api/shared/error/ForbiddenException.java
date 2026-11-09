package cl.textilcircular.api.shared.error;

public class ForbiddenException extends DomainException {

    public ForbiddenException() {
        this("No tienes permiso para hacer esto");
    }

    public ForbiddenException(String message) {
        super(message);
    }
}
