package cl.textilcircular.api.core.model.business;

import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.persistence.BaseEntity;
import cl.textilcircular.api.core.validation.Validators;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** One vote of the peer review. Created only through {@link Business#vote} and {@link Business#takeDownBy}. */
@Getter
@Entity
@Table(name = "business_review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "business_review_seq")
    @SequenceGenerator(name = "business_review_seq", sequenceName = "business_review_seq", allocationSize = 50)
    @Column(name = "review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false, updatable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false, updatable = false)
    private UserAccount reviewer;

    @Column(name = "review_round", nullable = false, updatable = false)
    private int reviewRound;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private ReviewDecision decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason", length = 30, updatable = false)
    private RejectionReason rejectionReason;

    @Column(length = Validators.COMMENT_MAX, updatable = false)
    private String comment;

    @Column(name = "reviewed_at", nullable = false, updatable = false)
    private Instant reviewedAt;

    /** A rejection needs a reason and a comment; an approval keeps an optional comment only. */
    static BusinessReview cast(Business business, UserAccount reviewer, int round, ReviewDecision decision,
                               RejectionReason reason, String comment, Instant now) {
        BusinessReview review = new BusinessReview();
        review.business = business;
        review.reviewer = reviewer;
        review.reviewRound = round;
        review.decision = Validators.required("decision", decision, "Elige si apruebas o rechazas la ficha");
        if (decision == ReviewDecision.REJECTED) {
            review.rejectionReason = Validators.required("rejectionReason", reason, "Elige un motivo de rechazo");
            review.comment = Validators.text("comment", comment, Validators.COMMENT_MAX, "un comentario");
        } else {
            review.comment = Validators.optionalText("comment", comment, Validators.COMMENT_MAX, "el comentario");
        }
        review.reviewedAt = now;
        return review;
    }
}
