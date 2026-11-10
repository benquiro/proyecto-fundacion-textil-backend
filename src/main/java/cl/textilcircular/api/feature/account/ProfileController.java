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

    @Operation(summary = "Get my profile", tags = "Profile", description = "Account data, plus the entrepreneur data when the role is ENTREPRENEUR.")
    @GetMapping("/me")
    Profile get(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return profiles.get(Caller.of(jwt));
    }

    @Operation(summary = "Update my profile", tags = "Profile", description = "`email` is required. `alias` is replaced as sent. `ageRangeId` and `genderId` (and the "
            + "entrepreneur names) only apply to entrepreneurs; omitting the two ids keeps the current values.")
    @ApiResponse(responseCode = "200", description = "Updated profile")
    @ApiResponse(responseCode = "409", description = "`EMAIL_TAKEN`: another account uses that email")
    @PatchMapping("/me")
    Profile update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @RequestBody UpdateProfileRequest request) {
        return profiles.update(Caller.of(jwt), request);
    }

    @Operation(summary = "Delete my account", tags = "Profile", description = "Frees the email. Only ACTIVE accounts can be deleted.")
    @ApiResponse(responseCode = "204", description = "Account deleted")
    @ApiResponse(responseCode = "409", description = "`INVALID_ACCOUNT_STATUS`: the account is not active")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        profiles.delete(Caller.of(jwt));
    }

    @Operation(summary = "Change my password", tags = "Profile",
            description = "Keeps the current session and ends every other one. The new password must differ from the current one.")
    @ApiResponse(responseCode = "204", description = "Password changed")
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @RequestBody ChangePasswordRequest request) {
        profiles.changePassword(Caller.of(jwt), request);
    }

    @Operation(summary = "Deactivate my account", tags = "Profile", description = "Logging in again reactivates the account.")
    @ApiResponse(responseCode = "204", description = "Account deactivated")
    @ApiResponse(responseCode = "409", description = "`INVALID_ACCOUNT_STATUS`: the account is not active")
    @PostMapping("/me/deactivation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        profiles.deactivate(Caller.of(jwt));
    }

    @Operation(summary = "List users (administrator)", tags = "Admin",
            description = "Paged list of every account. Requires the ADMINISTRATOR role.")
    @ApiResponse(responseCode = "200", description = "One page of users")
    @ApiResponse(responseCode = "403", description = "`FORBIDDEN`: the caller is not an administrator")
    @GetMapping("/admin/users")
    UserPage listUsers(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                       @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") int page,
                       @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return profiles.listUsers(Caller.of(jwt), page, size);
    }

    @Operation(summary = "Suspend a user (administrator)", tags = "Admin",
            description = "The user can no longer log in. Works on ACTIVE and DEACTIVATED accounts. "
                    + "Administrators cannot change their own status.")
    @ApiResponse(responseCode = "204", description = "User suspended")
    @ApiResponse(responseCode = "403", description = "`FORBIDDEN`: not an administrator, or the target is the caller")
    @ApiResponse(responseCode = "404", description = "`NOT_FOUND`: no such user")
    @ApiResponse(responseCode = "409", description = "`INVALID_ACCOUNT_STATUS`: the user is neither active nor deactivated")
    @PutMapping("/admin/users/{userId}/suspension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void suspend(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @Parameter(description = "Account id") @PathVariable long userId) {
        profiles.suspend(Caller.of(jwt), userId);
    }

    @Operation(summary = "Reactivate a suspended user (administrator)", tags = "Admin",
            description = "Works on SUSPENDED and DEACTIVATED accounts, so the user can log in again.")
    @ApiResponse(responseCode = "204", description = "User reactivated")
    @ApiResponse(responseCode = "403", description = "`FORBIDDEN`: not an administrator, or the target is the caller")
    @ApiResponse(responseCode = "404", description = "`NOT_FOUND`: no such user")
    @ApiResponse(responseCode = "409", description = "`INVALID_ACCOUNT_STATUS`: the user is neither suspended nor deactivated")
    @DeleteMapping("/admin/users/{userId}/suspension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reactivate(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @Parameter(description = "Account id") @PathVariable long userId) {
        profiles.reactivate(Caller.of(jwt), userId);
    }
}
