package cl.textilcircular.api.feature.account;

import cl.textilcircular.api.core.error.UnauthorizedException;
import cl.textilcircular.api.core.error.ValidationException;
import cl.textilcircular.api.core.model.account.AccountErrors;
import cl.textilcircular.api.core.model.account.Entrepreneur;
import cl.textilcircular.api.core.model.account.InvalidCredentialsException;
import cl.textilcircular.api.core.model.account.RefreshToken;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.model.business.Business;
import cl.textilcircular.api.core.model.reference.TextileService;
import cl.textilcircular.api.core.repository.AgeRangeRepository;
import cl.textilcircular.api.core.repository.BusinessRepository;
import cl.textilcircular.api.core.repository.CommuneRepository;
import cl.textilcircular.api.core.repository.EntrepreneurRepository;
import cl.textilcircular.api.core.repository.GenderRepository;
import cl.textilcircular.api.core.repository.RefreshTokenRepository;
import cl.textilcircular.api.core.repository.TextileServiceRepository;
import cl.textilcircular.api.core.repository.UserAccountRepository;
import cl.textilcircular.api.core.security.TokenService;
import cl.textilcircular.api.core.validation.Validators;
import cl.textilcircular.api.feature.account.AccountDtos.BusinessDraft;
import cl.textilcircular.api.feature.account.AccountDtos.LoginRequest;
import cl.textilcircular.api.feature.account.AccountDtos.RegisterCustomerRequest;
import cl.textilcircular.api.feature.account.AccountDtos.RegisterEntrepreneurRequest;
import cl.textilcircular.api.feature.account.AccountDtos.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
class AuthService {

    private final UserAccountRepository accounts;
    private final EntrepreneurRepository entrepreneurs;
    private final BusinessRepository businesses;
    private final RefreshTokenRepository refreshTokens;
    private final AgeRangeRepository ageRanges;
    private final GenderRepository genders;
    private final CommuneRepository communes;
    private final TextileServiceRepository services;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;

    /** Compared against when the email is unknown, so both failures take the same time. */
    private String dummyHash;

    Session registerCustomer(RegisterCustomerRequest request) {
        Instant now = clock.instant();
        UserAccount account = UserAccount.registerCustomer(request.email(), request.password(), request.alias(),
                passwordEncoder, now);
        requireFreeEmail(account);
        accounts.save(account);
        return Session.of(tokenService.issue(account, now), account);
    }

    /** Account, personal data and business (PENDING, round 1) in one transaction. */
    Session registerEntrepreneur(RegisterEntrepreneurRequest request) {
        Instant now = clock.instant();
        UserAccount account = UserAccount.registerEntrepreneur(request.email(), request.password(), request.alias(),
                passwordEncoder, now);
        Entrepreneur entrepreneur = Entrepreneur.create(account, request.rut(), request.firstName(), request.lastName(),
                request.ageRangeId() == null ? null : ageRanges.findById(request.ageRangeId()).orElse(null),
                request.genderId() == null ? null : genders.findById(request.genderId()).orElse(null));
        Business business = newBusiness(entrepreneur, request.business());
        requireFreeEmail(account);
        if (entrepreneurs.existsByRut(entrepreneur.getRut())) {
            throw AccountErrors.rutTaken();
        }
        accounts.save(account);
        entrepreneurs.save(entrepreneur);
        businesses.save(business);
        return Session.of(tokenService.issue(account, now), account);
    }

    Session login(LoginRequest request) {
        Instant now = clock.instant();
        UserAccount account = accounts.findByEmail(Validators.email(request.email())).orElse(null);
        if (account == null) {
            passwordEncoder.matches(request.password() == null ? "" : request.password(), dummyHash());
            throw new InvalidCredentialsException();
        }
        account.authenticate(request.password(), passwordEncoder, now);
        return Session.of(tokenService.issue(account, now), account);
    }

    /** Rotation: the token sent is revoked and a new pair is returned. */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    Session refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken token = findToken(rawRefreshToken)
                .filter(t -> t.isActive(now))
                .orElseThrow(UnauthorizedException::new);
        token.revoke(now);
        UserAccount account = token.getAccount();
        if (!account.isActive()) {
            throw new UnauthorizedException();
        }
        return Session.of(tokenService.issue(account, now), account);
    }

    /** Always succeeds, even for an unknown token. */
    void logout(String rawRefreshToken) {
        Instant now = clock.instant();
        findToken(rawRefreshToken).ifPresent(t -> t.revoke(now));
    }

    private Optional<RefreshToken> findToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return refreshTokens.findByTokenHashForUpdate(RefreshToken.hash(raw));
    }

    private void requireFreeEmail(UserAccount account) {
        if (accounts.existsByEmail(account.getEmail())) {
            throw AccountErrors.emailTaken();
        }
    }

    /** Business errors name their field under {@code business.}, as the contract asks. */
    private Business newBusiness(Entrepreneur owner, BusinessDraft draft) {
        if (draft == null) {
            throw new ValidationException("business", "Ingresa los datos de tu emprendimiento");
        }
        try {
            return Business.register(owner, draft.name(), draft.description(), draft.photoUrl(),
                    draft.communeId() == null ? null : communes.findById(draft.communeId()).orElse(null),
                    servicesInOrder(draft.serviceIds()));
        } catch (ValidationException e) {
            throw new ValidationException("business." + e.getField(), e.getMessage());
        }
    }

    /** Keeps the chosen order; an unknown id stays as {@code null} so the business rejects it. */
    private List<TextileService> servicesInOrder(List<Long> ids) {
        if (ids == null) {
            return null;
        }
        Map<Long, TextileService> found = services.findAllById(ids.stream().filter(Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(TextileService::getId, Function.identity()));
        return ids.stream().map(id -> id == null ? null : found.get(id)).toList();
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode("dummy-password-for-timing");
        }
        return dummyHash;
    }
}
