package cl.textilcircular.api.core.error;

/** The session is missing, expired or revoked; the app signs the user out. */
public class UnauthorizedException extends DomainException {

    public UnauthorizedException() {
        super("Tu sesión expiró. Inicia sesión de nuevo");
    }
}
