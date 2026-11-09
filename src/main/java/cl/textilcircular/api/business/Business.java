package cl.textilcircular.api.business;

import cl.textilcircular.api.account.Entrepreneur;
import cl.textilcircular.api.account.UserAccount;
import cl.textilcircular.api.reference.Commune;
import cl.textilcircular.api.reference.TextileService;
import cl.textilcircular.api.shared.error.ForbiddenException;
import cl.textilcircular.api.shared.error.ValidationException;
import cl.textilcircular.api.shared.persistence.AuditedEntity;
import cl.textilcircular.api.shared.validation.Validators;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ListIndexBase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * An entrepreneur's workshop and its community validation. Peer review rules live here:
 * two approvals in a round publish it, one rejection rejects it, and any substantive edit
 * starts a new round. Callers that vote must load it with {@code findByIdForUpdate}.
 */
@Getter
@Entity
@Table(name = "business")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Business extends AuditedEntity {

    static final int APPROVALS_TO_PUBLISH = 2;
    private static final int MAX_SERVICES = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "business_seq")
    @SequenceGenerator(name = "business_seq", sequenceName = "business_seq", allocationSize = 50)
    @Column(name = "business_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private Entrepreneur owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commune_id", nullable = false)
    private Commune commune;

    @Column(nullable = false, length = Validators.BUSINESS_NAME_MAX)
    private String name;

    @Column(nullable = false, length = Validators.BUSINESS_DESCRIPTION_MAX)
    private String description;

    @Column(name = "photo_url", length = Validators.URL_MAX)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BusinessStatus status;

    @Column(name = "review_round", nullable = false)
    private int reviewRound;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "hidden_at")
    private Instant hiddenAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private long version;

    /** Services in the order the owner chose them (sort_order 1 and 2). */
    @ManyToMany
    @JoinTable(name = "business_service",
            joinColumns = @JoinColumn(name = "business_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id"))
    @OrderColumn(name = "sort_order")
    @ListIndexBase(1)
    private List<TextileService> services = new ArrayList<>();

    @OneToMany(mappedBy = "business", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<BusinessReview> reviews = new ArrayList<>();

    public static Business register(Entrepreneur owner, String name, String description, String photoUrl,
                                    Commune commune, List<TextileService> services, Instant now) {
        Business business = new Business();
        business.owner = Objects.requireNonNull(owner);
        business.status = BusinessStatus.PENDING;
        business.reviewRound = 1;
        business.apply(name, description, photoUrl, commune, services);
        return business;
    }

    /**
     * Owner edit. A change to what reviewers judged (texts, photo, commune, services) sends the
     * business back to PENDING in a new round, whatever its status, so old votes never approve
     * new content. Saving the same values changes nothing.
     */
    public void edit(String name, String description, String photoUrl, Commune commune,
                     List<TextileService> services, Instant now) {
        requireNotDeleted();
        String oldName = this.name;
        String oldDescription = this.description;
        String oldPhoto = this.photoUrl;
        Long oldCommune = this.commune.getId();
        List<Long> oldServices = serviceIds();

        apply(name, description, photoUrl, commune, services);

        boolean changed = !Objects.equals(oldName, this.name)
                || !Objects.equals(oldDescription, this.description)
                || !Objects.equals(oldPhoto, this.photoUrl)
                || !Objects.equals(oldCommune, this.commune.getId())
                || !oldServices.equals(serviceIds());
        if (changed) {
            startNewRound();
        }
    }

    /**
     * Records a peer review vote in the current round and applies its outcome.
     * Entrepreneur reviewers must also own an approved business; the caller checks that
     * with {@code BusinessRepository.existsByOwnerIdAndStatusAndDeletedAtIsNull}.
     */
    public BusinessReview vote(UserAccount reviewer, ReviewDecision decision, RejectionReason reason,
                               String comment, Instant now) {
        if (isDeleted() || status != BusinessStatus.PENDING) {
            throw BusinessErrors.notPendingReview();
        }
        reviewer.ensureActive();
        if (!reviewer.isAdministrator() && !reviewer.isEntrepreneur()) {
            throw new ForbiddenException();
        }
        if (isOwnedBy(reviewer)) {
            throw new ForbiddenException("No puedes revisar tu propia ficha");
        }
        if (currentRoundReviews().stream().anyMatch(r -> sameEntity(r.getReviewer(), reviewer))) {
            throw BusinessErrors.alreadyVoted();
        }

        BusinessReview review = BusinessReview.cast(this, reviewer, reviewRound, decision, reason, comment, now);
        reviews.add(review);

        if (decision == ReviewDecision.REJECTED) {
            status = BusinessStatus.REJECTED;
        } else if (approvalsInCurrentRound() >= APPROVALS_TO_PUBLISH) {
            status = BusinessStatus.APPROVED;
            approvedAt = now;
        }
        return review;
    }

    /** Administrator moderation of a published business; recorded as a rejection in its own round. */
    public BusinessReview takeDownBy(UserAccount administrator, RejectionReason reason, String comment, Instant now) {
        administrator.ensureActive();
        if (!administrator.isAdministrator()) {
            throw new ForbiddenException();
        }
        requireNotDeleted();
        if (status != BusinessStatus.APPROVED) {
            throw BusinessErrors.notPublished();
        }
        // Validate the reason and comment before touching the round.
        BusinessReview review = BusinessReview.cast(this, administrator, reviewRound + 1, ReviewDecision.REJECTED,
                reason, comment, now);
        reviewRound++;
        reviews.add(review);
        status = BusinessStatus.REJECTED;
        approvedAt = null;
        return review;
    }

    /** Owner pauses the listing; no review needed. */
    public void hide(Instant now) {
        requireNotDeleted();
        if (hiddenAt == null) {
            hiddenAt = now;
        }
    }

    public void show() {
        requireNotDeleted();
        hiddenAt = null;
    }

    /** Used when the owner's account is deleted or by moderation; the row stays for history. */
    public void softDelete(Instant now) {
        if (deletedAt == null) {
            deletedAt = now;
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isOwnedBy(UserAccount account) {
        return sameEntity(owner.getAccount(), account);
    }

    /** Visible in the directory: same rule as {@code v_catalog}. */
    public boolean isPublic() {
        return status == BusinessStatus.APPROVED && deletedAt == null && hiddenAt == null
                && owner.getAccount().isActive();
    }

    public List<BusinessReview> currentRoundReviews() {
        return reviews.stream().filter(r -> r.getReviewRound() == reviewRound).toList();
    }

    public List<TextileService> getServices() {
        return Collections.unmodifiableList(services);
    }

    public List<BusinessReview> getReviews() {
        return Collections.unmodifiableList(reviews);
    }

    private long approvalsInCurrentRound() {
        return currentRoundReviews().stream().filter(r -> r.getDecision() == ReviewDecision.APPROVED).count();
    }

    private void startNewRound() {
        status = BusinessStatus.PENDING;
        reviewRound++;
        approvedAt = null;
    }

    private List<Long> serviceIds() {
        return services.stream().map(TextileService::getId).toList();
    }

    private void apply(String name, String description, String photoUrl, Commune commune,
                       List<TextileService> services) {
        // Validate everything first, so a rejected edit leaves the entity as it was.
        String validName = Validators.text("name", name, Validators.BUSINESS_NAME_MAX, "el nombre del emprendimiento");
        String validDescription = Validators.text("description", description, Validators.BUSINESS_DESCRIPTION_MAX, "la descripción");
        String validPhoto = Validators.url("photoUrl", photoUrl, false, "la foto");
        Commune validCommune = Validators.required("communeId", commune, "Elige la comuna");
        List<TextileService> chosen = validServices(services);

        this.name = validName;
        this.description = validDescription;
        this.photoUrl = validPhoto;
        this.commune = validCommune;
        this.services.clear();
        this.services.addAll(chosen);
    }

    private static List<TextileService> validServices(List<TextileService> services) {
        boolean valid = services != null && !services.isEmpty() && services.size() <= MAX_SERVICES
                && services.stream().allMatch(s -> s != null && s.isActive())
                && new HashSet<>(services.stream().map(TextileService::getId).toList()).size() == services.size();
        if (!valid) {
            throw new ValidationException("serviceIds", "Elige 1 o 2 servicios distintos");
        }
        return List.copyOf(services);
    }

    private void requireNotDeleted() {
        if (isDeleted()) {
            throw BusinessErrors.deleted();
        }
    }
}
