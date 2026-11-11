package cl.textilcircular.api.core.model.business;

import cl.textilcircular.api.core.model.account.Entrepreneur;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.model.reference.TextileService;
import cl.textilcircular.api.core.error.ConflictException;
import cl.textilcircular.api.core.error.ForbiddenException;
import cl.textilcircular.api.core.error.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static cl.textilcircular.api.support.Fixtures.NOW;
import static cl.textilcircular.api.support.Fixtures.administrator;
import static cl.textilcircular.api.support.Fixtures.commune;
import static cl.textilcircular.api.support.Fixtures.customer;
import static cl.textilcircular.api.support.Fixtures.entrepreneur;
import static cl.textilcircular.api.support.Fixtures.retiredService;
import static cl.textilcircular.api.support.Fixtures.service;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessTest {

    Entrepreneur owner = entrepreneur();

    Business pending() {
        return Business.register(owner, " Hilos de Barrio ", " Arreglos de ropa ", " ", commune(102L),
                List.of(service(4L), service(2L)));
    }

    @Test
    void registrationStartsPendingInRoundOne() {
        Business business = pending();

        assertThat(business.getName()).isEqualTo("Hilos de Barrio");
        assertThat(business.getPhotoUrl()).isNull();
        assertThat(business.getStatus()).isEqualTo(BusinessStatus.PENDING);
        assertThat(business.getReviewRound()).isEqualTo(1);
        assertThat(business.getServices()).extracting(TextileService::getId).containsExactly(4L, 2L);
        assertThat(business.isOwnedBy(owner.getAccount())).isTrue();
    }

    @Test
    void businessOffersOneOrTwoDistinctActiveServices() {
        for (List<TextileService> services : List.of(List.<TextileService>of(),
                List.of(service(1L), service(2L), service(3L)), List.of(service(1L), service(1L)),
                List.of(retiredService(5L)))) {
            assertThatThrownBy(() -> Business.register(owner, "Taller", "Desc", null, commune(1L), services))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Elige 1 o 2 servicios distintos")
                    .extracting("field").isEqualTo("serviceIds");
        }
    }

    @Test
    void photoMustBeAWebOrMediaLink() {
        assertThatThrownBy(() -> Business.register(owner, "Taller", "Desc", "javascript:alert(1)", commune(1L),
                List.of(service(1L))))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("photoUrl");
    }

    @Test
    void twoApprovalsPublishTheBusiness() {
        Business business = pending();

        business.vote(administrator(), ReviewDecision.APPROVED, null, "Muy bien", NOW);
        assertThat(business.getStatus()).isEqualTo(BusinessStatus.PENDING);

        business.vote(entrepreneur().getAccount(), ReviewDecision.APPROVED, null, null, NOW.plusSeconds(5));

        assertThat(business.getStatus()).isEqualTo(BusinessStatus.APPROVED);
        assertThat(business.getApprovedAt()).isEqualTo(NOW.plusSeconds(5));
        assertThat(business.getReviews()).hasSize(2).allMatch(r -> r.getReviewRound() == 1);
    }

    @Test
    void oneRejectionRejectsAndNeedsAReasonAndComment() {
        Business business = pending();
        UserAccount reviewer = administrator();

        assertThatThrownBy(() -> business.vote(reviewer, ReviewDecision.REJECTED, null, "Falta foto", NOW))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("rejectionReason");
        assertThatThrownBy(() -> business.vote(reviewer, ReviewDecision.REJECTED, RejectionReason.INAPPROPRIATE_IMAGES, " ", NOW))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("comment");

        BusinessReview review = business.vote(reviewer, ReviewDecision.REJECTED, RejectionReason.INAPPROPRIATE_IMAGES, "Falta foto", NOW);

        assertThat(business.getStatus()).isEqualTo(BusinessStatus.REJECTED);
        assertThat(review.getRejectionReason()).isEqualTo(RejectionReason.INAPPROPRIATE_IMAGES);
    }

    @Test
    void approvalIgnoresARejectionReason() {
        BusinessReview review = pending().vote(administrator(), ReviewDecision.APPROVED, RejectionReason.OTHER, null, NOW);

        assertThat(review.getRejectionReason()).isNull();
    }

    @Test
    void ownerAndCustomersCannotVote() {
        Business business = pending();

        assertThatThrownBy(() -> business.vote(owner.getAccount(), ReviewDecision.APPROVED, null, null, NOW))
                .isInstanceOf(ForbiddenException.class).hasMessage("No puedes revisar tu propia ficha");
        assertThatThrownBy(() -> business.vote(customer(), ReviewDecision.APPROVED, null, null, NOW))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void reviewerVotesOncePerRound() {
        Business business = pending();
        UserAccount reviewer = administrator();
        business.vote(reviewer, ReviewDecision.APPROVED, null, null, NOW);

        assertThatThrownBy(() -> business.vote(reviewer, ReviewDecision.APPROVED, null, null, NOW))
                .isInstanceOf(ConflictException.class).hasMessage("Ya votaste esta ficha")
                .extracting("code").isEqualTo("ALREADY_VOTED");
    }

    @Test
    void onlyPendingBusinessesCanBeVoted() {
        Business business = pending();
        business.vote(administrator(), ReviewDecision.REJECTED, RejectionReason.OTHER, "No", NOW);

        assertThatThrownBy(() -> business.vote(administrator(), ReviewDecision.APPROVED, null, null, NOW))
                .isInstanceOf(ConflictException.class).extracting("code").isEqualTo("NOT_PENDING_REVIEW");
    }

    @Test
    void substantiveEditRestartsTheReview() {
        Business business = approved();

        business.edit("Hilos de Barrio", "Nueva descripción", null, commune(102L), List.of(service(4L), service(2L)), NOW);

        assertThat(business.getStatus()).isEqualTo(BusinessStatus.PENDING);
        assertThat(business.getReviewRound()).isEqualTo(2);
        assertThat(business.getApprovedAt()).isNull();
        assertThat(business.currentRoundReviews()).isEmpty();
    }

    @Test
    void editWithoutChangesKeepsTheStatus() {
        Business business = approved();

        business.edit(" Hilos de Barrio ", "Arreglos de ropa", "", commune(102L), List.of(service(4L), service(2L)), NOW);

        assertThat(business.getStatus()).isEqualTo(BusinessStatus.APPROVED);
        assertThat(business.getReviewRound()).isEqualTo(1);
    }

    @Test
    void reorderingServicesIsASubstantiveChange() {
        Business business = approved();

        business.edit("Hilos de Barrio", "Arreglos de ropa", null, commune(102L), List.of(service(2L), service(4L)), NOW);

        assertThat(business.getServices()).extracting(TextileService::getId).containsExactly(2L, 4L);
        assertThat(business.getStatus()).isEqualTo(BusinessStatus.PENDING);
    }

    @Test
    void invalidEditLeavesTheBusinessUntouched() {
        Business business = approved();

        assertThatThrownBy(() -> business.edit("Otro nombre", "Otra", null, commune(1L), List.of(), NOW))
                .isInstanceOf(ValidationException.class);

        assertThat(business.getName()).isEqualTo("Hilos de Barrio");
        assertThat(business.getDescription()).isEqualTo("Arreglos de ropa");
        assertThat(business.getCommune().getId()).isEqualTo(102L);
        assertThat(business.getStatus()).isEqualTo(BusinessStatus.APPROVED);
    }

    @Test
    void hidingDoesNotTriggerReview() {
        Business business = approved();

        business.hide(NOW);
        assertThat(business.getHiddenAt()).isEqualTo(NOW);
        assertThat(business.getStatus()).isEqualTo(BusinessStatus.APPROVED);

        business.show();
        assertThat(business.getHiddenAt()).isNull();
    }

    @Test
    void administratorCanTakeDownAnApprovedBusiness() {
        Business business = approved();
        UserAccount admin = administrator();

        assertThatThrownBy(() -> business.takeDownBy(entrepreneur().getAccount(), RejectionReason.OTHER, "x", NOW))
                .isInstanceOf(ForbiddenException.class);

        business.takeDownBy(admin, RejectionReason.NOT_CIRCULAR, "No es circular", NOW);

        assertThat(business.getStatus()).isEqualTo(BusinessStatus.REJECTED);
        assertThat(business.getApprovedAt()).isNull();
        assertThat(business.getReviewRound()).isEqualTo(2);
        assertThat(business.currentRoundReviews()).singleElement()
                .extracting(BusinessReview::getDecision).isEqualTo(ReviewDecision.REJECTED);
    }

    @Test
    void deletedBusinessCannotBeEditedOrVoted() {
        Business business = pending();
        business.softDelete(NOW);

        assertThat(business.isDeleted()).isTrue();
        assertThatThrownBy(() -> business.vote(administrator(), ReviewDecision.APPROVED, null, null, NOW))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> business.hide(NOW)).isInstanceOf(ConflictException.class);
    }

    private Business approved() {
        Business business = Business.register(owner, "Hilos de Barrio", "Arreglos de ropa", null, commune(102L),
                List.of(service(4L), service(2L)));
        business.vote(administrator(), ReviewDecision.APPROVED, null, null, NOW);
        business.vote(administrator(), ReviewDecision.APPROVED, null, null, NOW);
        return business;
    }

    @Test
    void retiredServiceAlreadyChosenDoesNotBlockOtherEdits() {
        Business business = Business.register(owner, "Taller", "Desc", null, commune(1L), List.of(service(4L)));

        business.edit("Taller", "Nueva descripción", null, commune(1L), List.of(retiredService(4L)), NOW);

        assertThat(business.getDescription()).isEqualTo("Nueva descripción");
        assertThatThrownBy(() -> business.edit("Taller", "Desc", null, commune(1L), List.of(retiredService(5L)), NOW))
                .hasMessage("Elige 1 o 2 servicios distintos");
    }

    @Test
    void businessOfAnInactiveOwnerCannotBeVoted() {
        Business business = pending();
        owner.getAccount().suspendBy(administrator());

        assertThatThrownBy(() -> business.vote(administrator(), ReviewDecision.APPROVED, null, null, NOW))
                .isInstanceOf(ConflictException.class);
        assertThat(business.getStatus()).isEqualTo(BusinessStatus.PENDING);
    }
}
