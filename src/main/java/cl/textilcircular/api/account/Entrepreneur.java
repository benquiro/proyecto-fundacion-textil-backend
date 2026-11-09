package cl.textilcircular.api.account;

import cl.textilcircular.api.reference.AgeRange;
import cl.textilcircular.api.reference.Gender;
import cl.textilcircular.api.shared.persistence.BaseEntity;
import cl.textilcircular.api.shared.validation.Validators;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Personal and demographic data of an entrepreneur; same id as its {@link UserAccount}. */
@Getter
@Entity
@Table(name = "entrepreneur")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Entrepreneur extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "age_range_id", nullable = false)
    private AgeRange ageRange;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gender_id", nullable = false)
    private Gender gender;

    /** Immutable once registered; {@code null} only after anonymization. */
    @Column(length = 10)
    private String rut;

    @Column(name = "first_name", length = Validators.PERSON_NAME_MAX)
    private String firstName;

    @Column(name = "last_name", length = Validators.PERSON_NAME_MAX)
    private String lastName;

    @Column(name = "anonymized_at")
    private Instant anonymizedAt;

    public static Entrepreneur create(UserAccount account, String rut, String firstName, String lastName,
                                      AgeRange ageRange, Gender gender) {
        if (!account.isEntrepreneur()) {
            throw new IllegalArgumentException("Only ENTREPRENEUR accounts have an entrepreneur profile");
        }
        Entrepreneur entrepreneur = new Entrepreneur();
        entrepreneur.account = account;
        entrepreneur.rut = Validators.rut(rut);
        entrepreneur.applyProfile(firstName, lastName, ageRange, gender);
        return entrepreneur;
    }

    /** Everything but the RUT can change. */
    public void updateProfile(String firstName, String lastName, AgeRange ageRange, Gender gender) {
        applyProfile(firstName, lastName, ageRange, gender);
    }

    /** Removes the data that identifies the person; demographics stay for aggregated metrics. */
    public void anonymize(Instant now) {
        this.rut = null;
        this.firstName = null;
        this.lastName = null;
        this.anonymizedAt = now;
    }

    public String fullName() {
        return firstName == null ? null : firstName + " " + lastName;
    }

    private void applyProfile(String firstName, String lastName, AgeRange ageRange, Gender gender) {
        this.firstName = Validators.text("firstName", firstName, Validators.PERSON_NAME_MAX, "tu nombre");
        this.lastName = Validators.text("lastName", lastName, Validators.PERSON_NAME_MAX, "tus apellidos");
        this.ageRange = Validators.required("ageRangeId", ageRange, "Elige tu rango de edad");
        this.gender = Validators.required("genderId", gender, "Elige tu género");
    }
}
