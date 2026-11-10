package cl.textilcircular.api.feature.account;

import cl.textilcircular.api.feature.account.AccountDtos.LoginRequest;
import cl.textilcircular.api.feature.account.AccountDtos.RefreshRequest;
import cl.textilcircular.api.feature.account.AccountDtos.RegisterCustomerRequest;
import cl.textilcircular.api.feature.account.AccountDtos.RegisterEntrepreneurRequest;
import cl.textilcircular.api.feature.account.AccountDtos.Session;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Operations without a session: sign-up, login and the refresh-token lifecycle. */
@SecurityRequirements
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
class AuthController {

    private final AuthService auth;

    @Operation(tags = "Auth", summary = "Registrar un cliente",
            description = "Crea una cuenta CUSTOMER e inicia su sesión. Contraseña: de 8 a 72 bytes, con letras y números.")
    @ApiResponse(responseCode = "201", description = "Cuenta creada; devuelve la nueva sesión")
    @ApiResponse(responseCode = "409", description = "`EMAIL_TAKEN`: el correo ya tiene una cuenta")
    @PostMapping("/register/customer")
    @ResponseStatus(HttpStatus.CREATED)
    Session registerCustomer(@RequestBody RegisterCustomerRequest request) {
        return auth.registerCustomer(request);
    }

    @Operation(tags = "Auth", summary = "Registrar un emprendedor",
            description = "Crea una cuenta ENTREPRENEUR con su perfil y su primer emprendimiento, e inicia su sesión. "
                    + "Los errores del emprendimiento se informan como `business.<campo>`.")
    @ApiResponse(responseCode = "201", description = "Cuenta creada; devuelve la nueva sesión")
    @ApiResponse(responseCode = "409", description = "`EMAIL_TAKEN` o `RUT_TAKEN`")
    @PostMapping("/register/entrepreneur")
    @ResponseStatus(HttpStatus.CREATED)
    Session registerEntrepreneur(@RequestBody RegisterEntrepreneurRequest request) {
        return auth.registerEntrepreneur(request);
    }

    @Operation(tags = "Auth", summary = "Iniciar sesión", description = "Cambia correo y contraseña por una sesión. El correo no distingue mayúsculas.")
    @ApiResponse(responseCode = "200", description = "Sesión iniciada")
    @ApiResponse(responseCode = "401", description = "`INVALID_CREDENTIALS`: correo o contraseña incorrectos")
    @ApiResponse(responseCode = "403", description = "`ACCOUNT_NOT_ACTIVE`: la cuenta está suspendida; "
            + "`accountStatus` indica el estado")
    @PostMapping("/login")
    Session login(@RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @Operation(tags = "Auth", summary = "Renovar la sesión",
            description = "Rota el token de refresco: el enviado se consume y se devuelve un par nuevo. "
                    + "Enviar de nuevo un token ya consumido revoca toda la sesión.")
    @ApiResponse(responseCode = "200", description = "Nuevos tokens de sesión")
    @ApiResponse(responseCode = "401", description = "`UNAUTHORIZED`: el token de refresco no existe, venció o ya se usó")
    @PostMapping("/refresh")
    Session refresh(@RequestBody RefreshRequest request) {
        return auth.refresh(request.refreshToken());
    }

    @Operation(tags = "Auth", summary = "Cerrar sesión", description = "Revoca la sesión del token de refresco enviado, lo que también termina su token de acceso.")
    @ApiResponse(responseCode = "204", description = "Sesión terminada")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@RequestBody RefreshRequest request) {
        auth.logout(request.refreshToken());
    }
}
