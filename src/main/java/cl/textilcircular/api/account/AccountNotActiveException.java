package cl.textilcircular.api.account;

import cl.textilcircular.api.shared.error.DomainException;
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
