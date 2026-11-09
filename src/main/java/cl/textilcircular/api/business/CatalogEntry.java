package cl.textilcircular.api.business;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/** Read-only row of the public directory view {@code v_catalog}. Carries no personal data. */
@Getter
@Entity
@Immutable
@Table(name = "v_catalog")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CatalogEntry {

    @Id
    @Column(name = "business_id")
    private Long id;

    private String name;

    private String description;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "commune_id")
    private Long communeId;

    @Column(name = "commune_name")
    private String communeName;

    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "region_name")
    private String regionName;

    @Column(name = "approved_at")
    private Instant approvedAt;
}
