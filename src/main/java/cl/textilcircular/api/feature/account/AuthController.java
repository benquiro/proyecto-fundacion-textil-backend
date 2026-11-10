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

    @Operation(tags = "Auth", summary = "Register a customer",
            description = "Creates a CUSTOMER account and starts its session. Password: 8 to 72 bytes, with letters and digits.")
    @ApiResponse(responseCode = "201", description = "Account created; returns the new session")
    @ApiResponse(responseCode = "409", description = "`EMAIL_TAKEN`: the email already has an account")
    @PostMapping("/register/customer")
    @ResponseStatus(HttpStatus.CREATED)
    Session registerCustomer(@RequestBody RegisterCustomerRequest request) {
        return auth.registerCustomer(request);
    }

    @Operation(tags = "Auth", summary = "Register an entrepreneur",
            description = "Creates an ENTREPRENEUR account with its profile and first business, and starts its session. "
                    + "Business field errors are reported as `business.<field>`.")
    @ApiResponse(responseCode = "201", description = "Account created; returns the new session")
    @ApiResponse(responseCode = "409", description = "`EMAIL_TAKEN` or `RUT_TAKEN`")
    @PostMapping("/register/entrepreneur")
    @ResponseStatus(HttpStatus.CREATED)
    Session registerEntrepreneur(@RequestBody RegisterEntrepreneurRequest request) {
        return auth.registerEntrepreneur(request);
    }

    @Operation(tags = "Auth", summary = "Log in", description = "Exchanges email and password for a session. The email is case-insensitive.")
    @ApiResponse(responseCode = "200", description = "Session started")
    @ApiResponse(responseCode = "401", description = "`INVALID_CREDENTIALS`: wrong email or password")
    @ApiResponse(responseCode = "403", description = "`ACCOUNT_NOT_ACTIVE`: the account is deactivated or suspended; "
            + "`accountStatus` tells which")
    @PostMapping("/login")
    Session login(@RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @Operation(tags = "Auth", summary = "Renew the session",
            description = "Rotates the refresh token: the one sent is spent and a new pair is returned. "
                    + "Sending a spent token again revokes the whole session.")
    @ApiResponse(responseCode = "200", description = "New session tokens")
    @ApiResponse(responseCode = "401", description = "`UNAUTHORIZED`: the refresh token is unknown, expired or already used")
    @PostMapping("/refresh")
    Session refresh(@RequestBody RefreshRequest request) {
        return auth.refresh(request.refreshToken());
    }

    @Operation(tags = "Auth", summary = "Log out", description = "Revokes the session of the given refresh token, which also ends its access token.")
    @ApiResponse(responseCode = "204", description = "Session ended")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@RequestBody RefreshRequest request) {
        auth.logout(request.refreshToken());
    }
}
