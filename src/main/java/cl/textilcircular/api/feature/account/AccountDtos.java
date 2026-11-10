package cl.textilcircular.api.feature.account;

import cl.textilcircular.api.core.model.account.AccountStatus;
import cl.textilcircular.api.core.model.account.Entrepreneur;
import cl.textilcircular.api.core.model.account.Role;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.security.TokenService.TokenPair;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Request and response bodies of the account endpoints, as named in the API contract. */
final class AccountDtos {

    private AccountDtos() {
    }

    record LoginRequest(@Schema(example = "valentina.mora@example.cl") String email,
                        @Schema(example = "Demo1234") String password) {
    }

    record RefreshRequest(@Schema(description = "Refresh token from the last session response") String refreshToken) {
    }

    record RegisterCustomerRequest(@Schema(example = "cliente@example.cl") String email,
                                   @Schema(description = "8 to 72 bytes, with letters and digits", example = "Clave1234") String password,
                                   @Schema(description = "Required for customers", example = "Cliente Textil") String alias) {
    }

    record BusinessDraft(String name, String description, String photoUrl, Long communeId,
                         @Schema(description = "1 or 2 distinct textile service ids") List<Long> serviceIds) {
    }

    record RegisterEntrepreneurRequest(String email, String password, @Schema(description = "Optional") String alias,
                                       @Schema(description = "Chilean RUT, with or without dots", example = "12345678-5") String rut,
                                       String firstName, String lastName, Long ageRangeId, Long genderId,
                                       @Schema(description = "First business of the entrepreneur") BusinessDraft business) {
    }

    record UpdateProfileRequest(String email, String alias, String firstName, String lastName, Long ageRangeId,
                                Long genderId) {
    }

    record ChangePasswordRequest(String currentPassword,
                                 @Schema(description = "8 to 72 bytes, with letters and digits, different from the current one") String newPassword) {
    }

    record SessionUser(long id, String email, Role role, String alias, AccountStatus status) {

        static SessionUser of(UserAccount account) {
            return new SessionUser(account.getId(), account.getEmail(), account.getRole(), account.getAlias(),
                    account.getStatus());
        }
    }

    record Session(@Schema(description = "JWT for the Authorization: Bearer header") String accessToken,
                   @Schema(description = "Single-use token to renew the session") String refreshToken,
                   @Schema(description = "Access token lifetime in seconds", example = "900") long expiresIn,
                   SessionUser user) {

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

    record UserPage(List<SessionUser> content, @Schema(description = "Zero-based") int page, int size,
                    long totalElements, @Schema(description = "True on the final page") boolean last) {
    }
}
