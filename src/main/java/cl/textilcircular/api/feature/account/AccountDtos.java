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

    record RefreshRequest(@Schema(description = "Token de refresco de la última respuesta de sesión") String refreshToken) {
    }

    record RegisterCustomerRequest(@Schema(example = "cliente@example.cl") String email,
                                   @Schema(description = "De 8 a 72 bytes, con letras y números", example = "Clave1234") String password,
                                   @Schema(description = "Obligatorio para clientes", example = "Cliente Textil") String alias) {
    }

    record BusinessDraft(String name, String description, String photoUrl, Long communeId,
                         @Schema(description = "1 o 2 ids de servicios textiles distintos") List<Long> serviceIds) {
    }

    record RegisterEntrepreneurRequest(String email, String password, @Schema(description = "Opcional") String alias,
                                       @Schema(description = "RUT chileno, con o sin puntos", example = "12345678-5") String rut,
                                       String firstName, String lastName, Long ageRangeId, Long genderId,
                                       @Schema(description = "Primer emprendimiento de la persona emprendedora") BusinessDraft business) {
    }

    record UpdateProfileRequest(String email, String alias, String firstName, String lastName, Long ageRangeId,
                                Long genderId) {
    }

    record ChangePasswordRequest(String currentPassword,
                                 @Schema(description = "De 8 a 72 bytes, con letras y números, distinta de la actual") String newPassword) {
    }

    record SessionUser(long id, String email, Role role, String alias, AccountStatus status) {

        static SessionUser of(UserAccount account) {
            return new SessionUser(account.getId(), account.getEmail(), account.getRole(), account.getAlias(),
                    account.getStatus());
        }
    }

    record Session(@Schema(description = "JWT para el encabezado Authorization: Bearer") String accessToken,
                   @Schema(description = "Token de un solo uso para renovar la sesión") String refreshToken,
                   @Schema(description = "Duración del token de acceso en segundos", example = "900") long expiresIn,
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

    record UserPage(List<SessionUser> content, @Schema(description = "Base cero") int page, int size,
                    long totalElements, @Schema(description = "Verdadero en la última página") boolean last) {
    }
}
