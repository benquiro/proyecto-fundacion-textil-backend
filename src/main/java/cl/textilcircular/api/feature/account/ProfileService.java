package cl.textilcircular.api.feature.account;

import cl.textilcircular.api.core.error.NotFoundException;
import cl.textilcircular.api.core.error.UnauthorizedException;
import cl.textilcircular.api.core.model.account.AccountErrors;
import cl.textilcircular.api.core.model.account.AccountStatus;
import cl.textilcircular.api.core.model.account.Entrepreneur;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.model.reference.AgeRange;
import cl.textilcircular.api.core.model.reference.Gender;
import cl.textilcircular.api.core.repository.AgeRangeRepository;
import cl.textilcircular.api.core.repository.BusinessRepository;
import cl.textilcircular.api.core.repository.EntrepreneurRepository;
import cl.textilcircular.api.core.repository.GenderRepository;
import cl.textilcircular.api.core.repository.RefreshTokenRepository;
import cl.textilcircular.api.core.repository.UserAccountRepository;
import cl.textilcircular.api.core.security.Caller;
import cl.textilcircular.api.core.validation.Validators;
import cl.textilcircular.api.feature.account.AccountDtos.ChangePasswordRequest;
import cl.textilcircular.api.feature.account.AccountDtos.Profile;
import cl.textilcircular.api.feature.account.AccountDtos.SessionUser;
import cl.textilcircular.api.feature.account.AccountDtos.UpdateProfileRequest;
import cl.textilcircular.api.feature.account.AccountDtos.UserPage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** The caller's own account, plus the administrator's account moderation. */
@Service
@RequiredArgsConstructor
@Transactional
class ProfileService {

    private static final int MAX_PAGE_SIZE = 50;

    private final UserAccountRepository accounts;
    private final EntrepreneurRepository entrepreneurs;
    private final BusinessRepository businesses;
    private final RefreshTokenRepository refreshTokens;
    private final AgeRangeRepository ageRanges;
    private final GenderRepository genders;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional(readOnly = true)
    Profile get(Caller caller) {
        UserAccount account = activeAccount(caller);
        return Profile.of(account, entrepreneurOf(account));
    }

    /** RUT never changes; entrepreneur fields are ignored for other roles. */
    Profile update(Caller caller, UpdateProfileRequest request) {
        UserAccount account = activeAccount(caller);
        String email = Validators.email(request.email());
        if (!email.equals(account.getEmail()) && accounts.existsByEmail(email)) {
            throw AccountErrors.emailTaken();
        }
        Entrepreneur entrepreneur = entrepreneurOf(account);
        account.updateProfile(email, request.alias());
        if (entrepreneur != null) {
            AgeRange ageRange = request.ageRangeId() == null ? entrepreneur.getAgeRange()
                    : ageRanges.findById(request.ageRangeId()).orElse(null);
            Gender gender = request.genderId() == null ? entrepreneur.getGender()
                    : genders.findById(request.genderId()).orElse(null);
            entrepreneur.updateProfile(request.firstName(), request.lastName(), ageRange, gender);
        }
        return Profile.of(account, entrepreneur);
    }

    /** Keeps the caller's own session and ends every other one. */
    void changePassword(Caller caller, ChangePasswordRequest request) {
        UserAccount account = activeAccount(caller);
        account.changePassword(request.currentPassword(), request.newPassword(), passwordEncoder);
        refreshTokens.revokeAllActiveExcept(account.getId(), caller.sessionId(), clock.instant());
    }

    void deactivate(Caller caller) {
        UserAccount account = activeAccount(caller);
        Instant now = clock.instant();
        account.deactivate(now);
        refreshTokens.revokeAllActive(account.getId(), now);
    }

    /**
     * Soft delete plus anonymization; the business leaves the directory with the account. Only an active
     * account may delete itself, so a suspension cannot be escaped by deleting and signing up again.
     */
    void delete(Caller caller) {
        UserAccount account = activeAccount(caller);
        Instant now = clock.instant();
        account.delete(now);
        Entrepreneur entrepreneur = entrepreneurOf(account);
        if (entrepreneur != null) {
            entrepreneur.anonymize(now);
            businesses.findByOwnerId(account.getId()).ifPresent(b -> b.softDelete(now));
        }
        refreshTokens.revokeAllActive(account.getId(), now);
    }

    @Transactional(readOnly = true)
    UserPage listUsers(Caller caller, int page, int size) {
        activeAccount(caller);
        Page<UserAccount> found = accounts.findAll(PageRequest.of(Math.max(page, 0),
                Math.clamp(size, 1, MAX_PAGE_SIZE), Sort.by("id")));
        return new UserPage(found.map(SessionUser::of).getContent(), found.getNumber(), found.getSize(),
                found.getTotalElements(), found.isLast());
    }

    void suspend(Caller caller, long userId) {
        UserAccount administrator = activeAccount(caller);
        UserAccount target = accounts.findById(userId).orElseThrow(NotFoundException::new);
        target.suspendBy(administrator);
        refreshTokens.revokeAllActive(target.getId(), clock.instant());
    }

    void reactivate(Caller caller, long userId) {
        UserAccount administrator = activeAccount(caller);
        UserAccount target = accounts.findById(userId).orElseThrow(NotFoundException::new);
        target.reactivateBy(administrator);
    }

    /** A token can outlive its account's status: suspended or deactivated accounts are refused here. */
    private UserAccount activeAccount(Caller caller) {
        UserAccount account = existingAccount(caller.userId());
        account.ensureActive();
        return account;
    }

    private UserAccount existingAccount(long userId) {
        return accounts.findById(userId)
                .filter(a -> a.getStatus() != AccountStatus.DELETED)
                .orElseThrow(UnauthorizedException::new);
    }

    private Entrepreneur entrepreneurOf(UserAccount account) {
        return account.isEntrepreneur() ? entrepreneurs.findById(account.getId()).orElse(null) : null;
    }
}
