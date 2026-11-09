package cl.textilcircular.api.core.model.reference;

import cl.textilcircular.api.core.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Circular textile service a business offers (repair, upcycling...). Table {@code service};
 * the class name avoids clashing with Spring's {@code @Service}.
 */
@Getter
@Entity
@Table(name = "service")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TextileService extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "service_seq")
    @SequenceGenerator(name = "service_seq", sequenceName = "service_seq", allocationSize = 50)
    @Column(name = "service_id")
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
