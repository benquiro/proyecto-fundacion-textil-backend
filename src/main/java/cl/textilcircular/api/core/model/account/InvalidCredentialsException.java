package cl.textilcircular.api.core.model.account;

import cl.textilcircular.api.core.error.DomainException;

/** Unknown email or wrong password; the message never tells which one. */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Correo o contraseña incorrectos");
    }
}
