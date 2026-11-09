package cl.textilcircular.api.core.model.account;

import cl.textilcircular.api.core.error.ConflictException;

/** Conflicts raised by the account feature, with the app's wording. */
public final class AccountErrors {

    private AccountErrors() {
    }

    public static ConflictException emailTaken() {
        return new ConflictException("EMAIL_TAKEN", "Ese correo ya tiene una cuenta");
    }

    public static ConflictException rutTaken() {
        return new ConflictException("RUT_TAKEN", "Ese RUT ya está registrado");
    }

    static ConflictException invalidStatus(String message) {
        return new ConflictException("INVALID_ACCOUNT_STATUS", message);
    }
}
