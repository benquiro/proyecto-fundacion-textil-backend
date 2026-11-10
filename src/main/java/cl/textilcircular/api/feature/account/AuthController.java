package cl.textilcircular.api.feature.account;

import cl.textilcircular.api.feature.account.AccountDtos.LoginRequest;
import cl.textilcircular.api.feature.account.AccountDtos.RefreshRequest;
import cl.textilcircular.api.feature.account.AccountDtos.RegisterCustomerRequest;
import cl.textilcircular.api.feature.account.AccountDtos.RegisterEntrepreneurRequest;
import cl.textilcircular.api.feature.account.AccountDtos.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Operations without a session: sign-up, login and the refresh-token lifecycle. */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
class AuthController {

    private final AuthService auth;

    @PostMapping("/register/customer")
    @ResponseStatus(HttpStatus.CREATED)
    Session registerCustomer(@RequestBody RegisterCustomerRequest request) {
        return auth.registerCustomer(request);
    }

    @PostMapping("/register/entrepreneur")
    @ResponseStatus(HttpStatus.CREATED)
    Session registerEntrepreneur(@RequestBody RegisterEntrepreneurRequest request) {
        return auth.registerEntrepreneur(request);
    }

    @PostMapping("/login")
    Session login(@RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @PostMapping("/refresh")
    Session refresh(@RequestBody RefreshRequest request) {
        return auth.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@RequestBody RefreshRequest request) {
        auth.logout(request.refreshToken());
    }
}
