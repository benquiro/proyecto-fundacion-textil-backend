package cl.textilcircular.api.feature.account;

import cl.textilcircular.api.core.model.account.AccountStatus;
import cl.textilcircular.api.core.model.account.Entrepreneur;
import cl.textilcircular.api.core.model.account.Role;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.security.TokenService.TokenPair;

import java.util.List;

/** Request and response bodies of the account endpoints, as named in the API contract. */
final class AccountDtos {

    private AccountDtos() {
    }

    record LoginRequest(String email, String password) {
    }

    record RefreshRequest(String refreshToken) {
    }

    record RegisterCustomerRequest(String email, String password, String alias) {
    }

    record BusinessDraft(String name, String description, String photoUrl, Long communeId, List<Long> serviceIds) {
    }

    record RegisterEntrepreneurRequest(String email, String password, String alias, String rut, String firstName,
                                       String lastName, Long ageRangeId, Long genderId, BusinessDraft business) {
    }

    record UpdateProfileRequest(String email, String alias, String firstName, String lastName, Long ageRangeId,
                                Long genderId) {
    }

    record ChangePasswordRequest(String currentPassword, String newPassword) {
    }

    record SessionUser(long id, String email, Role role, String alias, AccountStatus status) {

        static SessionUser of(UserAccount account) {
            return new SessionUser(account.getId(), account.getEmail(), account.getRole(), account.getAlias(),
                    account.getStatus());
        }
    }

    record Session(String accessToken, String refreshToken, long expiresIn, SessionUser user) {

        static Session of(TokenPair tokens, UserAccount account) {
            return new Session(tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn(), SessionUser.of(account));
        }
    }

    record EntrepreneurData(String rut, String firstName, String lastName, long ageRangeId, long genderId) {

        static EntrepreneurData of(Entrepreneur entrepreneur) {
            return new EntrepreneurData(entrepreneur.getRut(), entrepreneur.getFirstName(), entrepreneur.getLastName(),
                    entrepreneur.getAgeRange().getId(), entrepreneur.getGender().getId());
        }
    }

    record Profile(long id, String email, Role role, String alias, AccountStatus status, EntrepreneurData entrepreneur) {

        static Profile of(UserAccount account, Entrepreneur entrepreneur) {
            return new Profile(account.getId(), account.getEmail(), account.getRole(), account.getAlias(),
                    account.getStatus(), entrepreneur == null ? null : EntrepreneurData.of(entrepreneur));
        }
    }

    record UserPage(List<SessionUser> content, int page, int size, long totalElements, boolean last) {
    }
}
