package cl.textilcircular.api.feature.account;

import cl.textilcircular.api.core.security.Caller;
import cl.textilcircular.api.feature.account.AccountDtos.ChangePasswordRequest;
import cl.textilcircular.api.feature.account.AccountDtos.Profile;
import cl.textilcircular.api.feature.account.AccountDtos.UpdateProfileRequest;
import cl.textilcircular.api.feature.account.AccountDtos.UserPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The caller's profile ({@code /me}) and the administrator's user moderation ({@code /admin/users}). */
@RestController
@RequiredArgsConstructor
class ProfileController {

    private final ProfileService profiles;

    @Operation(summary = "Ver mi perfil", tags = "Profile", description = "Datos de la cuenta, más los datos de emprendedor cuando el rol es ENTREPRENEUR.")
    @GetMapping("/me")
    Profile get(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return profiles.get(Caller.of(jwt));
    }

    @Operation(summary = "Reemplazar mi perfil", tags = "Profile",
            description = "Reemplazo completo: todas las propiedades presentes. Las reglas por rol están en docs/api/openapi.yaml.")
    @ApiResponse(responseCode = "200", description = "Perfil actualizado")
    @ApiResponse(responseCode = "409", description = "`EMAIL_TAKEN`: otra cuenta usa ese correo")
    @PutMapping("/me")
    Profile update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @RequestBody UpdateProfileRequest request) {
        return profiles.update(Caller.of(jwt), request);
    }

    @Operation(summary = "Eliminar mi cuenta", tags = "Profile", description = "Libera el correo. Solo se pueden eliminar cuentas ACTIVE.")
    @ApiResponse(responseCode = "204", description = "Cuenta eliminada")
    @ApiResponse(responseCode = "403", description = "`ACCOUNT_NOT_ACTIVE`: la cuenta ya no está activa")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        profiles.delete(Caller.of(jwt));
    }

    @Operation(summary = "Cambiar mi contraseña", tags = "Profile",
            description = "Conserva la sesión actual y termina todas las demás. La nueva contraseña debe ser distinta de la actual.")
    @ApiResponse(responseCode = "204", description = "Contraseña cambiada")
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @RequestBody ChangePasswordRequest request) {
        profiles.changePassword(Caller.of(jwt), request);
    }

    @Operation(summary = "Desactivar mi cuenta", tags = "Profile", description = "Volver a iniciar sesión reactiva la cuenta.")
    @ApiResponse(responseCode = "204", description = "Cuenta desactivada")
    @ApiResponse(responseCode = "403", description = "`ACCOUNT_NOT_ACTIVE`: la cuenta ya no está activa")
    @PostMapping("/me/deactivation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        profiles.deactivate(Caller.of(jwt));
    }

    @Operation(summary = "Listar usuarios (administrador)", tags = "Admin",
            description = "Lista paginada de todas las cuentas. Requiere el rol ADMINISTRATOR.")
    @ApiResponse(responseCode = "200", description = "Una página de usuarios")
    @ApiResponse(responseCode = "403", description = "`FORBIDDEN`: quien llama no es administrador")
    @GetMapping("/admin/users")
    UserPage listUsers(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                       @Parameter(description = "Número de página, desde 0") @RequestParam(defaultValue = "0") int page,
                       @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size) {
        return profiles.listUsers(Caller.of(jwt), page, size);
    }

    @Operation(summary = "Suspender un usuario (administrador)", tags = "Admin",
            description = "El usuario ya no puede iniciar sesión. Funciona con cuentas ACTIVE y DEACTIVATED. "
                    + "Un administrador no puede cambiar su propio estado.")
    @ApiResponse(responseCode = "204", description = "Usuario suspendido")
    @ApiResponse(responseCode = "403", description = "`FORBIDDEN`: no es administrador, o el usuario objetivo es quien llama")
    @ApiResponse(responseCode = "404", description = "`NOT_FOUND`: el usuario no existe")
    @ApiResponse(responseCode = "409", description = "`INVALID_ACCOUNT_STATUS`: el usuario no está activo ni desactivado")
    @PutMapping("/admin/users/{userId}/suspension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void suspend(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @Parameter(description = "Id de la cuenta") @PathVariable long userId) {
        profiles.suspend(Caller.of(jwt), userId);
    }

    @Operation(summary = "Reactivar un usuario suspendido (administrador)", tags = "Admin",
            description = "Funciona con cuentas SUSPENDED y DEACTIVATED, para que el usuario pueda iniciar sesión de nuevo.")
    @ApiResponse(responseCode = "204", description = "Usuario reactivado")
    @ApiResponse(responseCode = "403", description = "`FORBIDDEN`: no es administrador, o el usuario objetivo es quien llama")
    @ApiResponse(responseCode = "404", description = "`NOT_FOUND`: el usuario no existe")
    @ApiResponse(responseCode = "409", description = "`INVALID_ACCOUNT_STATUS`: el usuario no está suspendido ni desactivado")
    @DeleteMapping("/admin/users/{userId}/suspension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reactivate(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @Parameter(description = "Id de la cuenta") @PathVariable long userId) {
        profiles.reactivate(Caller.of(jwt), userId);
    }
}
