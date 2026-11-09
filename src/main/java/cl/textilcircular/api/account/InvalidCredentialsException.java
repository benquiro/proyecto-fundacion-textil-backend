package cl.textilcircular.api.account;

import cl.textilcircular.api.shared.error.DomainException;

/** Unknown email or wrong password; the message never tells which one. */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Correo o contraseña incorrectos");
    }
}
