package cl.textilcircular.api.core.error;

public class NotFoundException extends DomainException {

    public NotFoundException() {
        super("No encontramos lo que buscas");
    }
}
