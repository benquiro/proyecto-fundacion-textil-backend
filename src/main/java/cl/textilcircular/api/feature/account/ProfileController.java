package cl.textilcircular.api.feature.account;

import cl.textilcircular.api.core.security.Caller;
import cl.textilcircular.api.feature.account.AccountDtos.ChangePasswordRequest;
import cl.textilcircular.api.feature.account.AccountDtos.Profile;
import cl.textilcircular.api.feature.account.AccountDtos.UpdateProfileRequest;
import cl.textilcircular.api.feature.account.AccountDtos.UserPage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    @GetMapping("/me")
    Profile get(@AuthenticationPrincipal Jwt jwt) {
        return profiles.get(Caller.of(jwt));
    }

    @PatchMapping("/me")
    Profile update(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateProfileRequest request) {
        return profiles.update(Caller.of(jwt), request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal Jwt jwt) {
        profiles.delete(Caller.of(jwt));
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@AuthenticationPrincipal Jwt jwt, @RequestBody ChangePasswordRequest request) {
        profiles.changePassword(Caller.of(jwt), request);
    }

    @PostMapping("/me/deactivation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@AuthenticationPrincipal Jwt jwt) {
        profiles.deactivate(Caller.of(jwt));
    }

    @GetMapping("/admin/users")
    UserPage listUsers(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size) {
        return profiles.listUsers(Caller.of(jwt), page, size);
    }

    @PutMapping("/admin/users/{userId}/suspension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void suspend(@AuthenticationPrincipal Jwt jwt, @PathVariable long userId) {
        profiles.suspend(Caller.of(jwt), userId);
    }

    @DeleteMapping("/admin/users/{userId}/suspension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable long userId) {
        profiles.reactivate(Caller.of(jwt), userId);
    }
}
