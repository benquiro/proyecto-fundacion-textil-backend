package cl.textilcircular.api.core.model.account;

import cl.textilcircular.api.core.error.DomainException;
import lombok.Getter;

@Getter
public class AccountNotActiveException extends DomainException {

    private final AccountStatus status;

    public AccountNotActiveException(AccountStatus status) {
        super(switch (status) {
            case SUSPENDED -> "Tu cuenta está suspendida";
            case DEACTIVATED -> "Tu cuenta está desactivada";
            default -> "Tu cuenta no está activa";
        });
        this.status = status;
    }
}
