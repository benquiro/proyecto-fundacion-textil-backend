package cl.textilcircular.api.core.model.reference;

import cl.textilcircular.api.core.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "commune")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Commune extends BaseEntity {

    @Id
    @Column(name = "commune_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(nullable = false, length = 100)
    private String name;
}
