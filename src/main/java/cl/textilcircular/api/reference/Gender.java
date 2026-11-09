package cl.textilcircular.api.reference;

import cl.textilcircular.api.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "gender")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Gender extends BaseEntity {

    @Id
    @Column(name = "gender_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String description;
}
