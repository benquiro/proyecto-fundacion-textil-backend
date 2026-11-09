package cl.textilcircular.api.product;

import cl.textilcircular.api.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Gallery image; {@code sortOrder} 1 is the main photo. Managed through {@link Product}. */
@Getter
@Entity
@Table(name = "product_photo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductPhoto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_photo_seq")
    @SequenceGenerator(name = "product_photo_seq", sequenceName = "product_photo_seq", allocationSize = 50)
    @Column(name = "photo_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    ProductPhoto(Product product, String url, int sortOrder, Instant now) {
        this.product = product;
        this.url = url;
        this.sortOrder = sortOrder;
        this.createdAt = now;
    }
}
