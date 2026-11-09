package cl.textilcircular.api.shared.error;

public class NotFoundException extends DomainException {

    public NotFoundException() {
        super("No encontramos lo que buscas");
    }
}
