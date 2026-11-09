package cl.textilcircular.api.core.model.product;

import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.model.business.Business;
import cl.textilcircular.api.core.error.ConflictException;
import cl.textilcircular.api.core.error.ForbiddenException;
import cl.textilcircular.api.core.error.ValidationException;
import cl.textilcircular.api.core.persistence.AuditedEntity;
import cl.textilcircular.api.core.validation.Validators;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Item in a business catalog. Every change requires the business owner as {@code actor}, so
 * ownership is enforced by the model and not by each endpoint.
 */
@Getter
@Entity
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends AuditedEntity {

    private static final int MAX_PHOTOS = 5;
    private static final int MATERIAL_MAX = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @SequenceGenerator(name = "product_seq", sequenceName = "product_seq", allocationSize = 50)
    @Column(name = "product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false, updatable = false)
    private Business business;

    @Column(nullable = false, length = Validators.PRODUCT_TITLE_MAX)
    private String title;

    @Column(nullable = false, length = Validators.PRODUCT_DESCRIPTION_MAX)
    private String description;

    /** Reference price in CLP; {@code null} means "ask". */
    private Long price;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private ProductSize size;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProductCondition condition;

    @Column(length = MATERIAL_MAX)
    private String material;

    @Enumerated(EnumType.STRING)
    @Column(name = "offering_type", length = 20)
    private OfferingType offeringType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ProductStatus status;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    private List<ProductPhoto> photos = new ArrayList<>();

    public static Product create(Business business, UserAccount actor, String title, String description, Long price,
                                 List<String> photoUrls, ProductAttributes attributes, Instant now) {
        requireOwner(business, actor);
        if (business.isDeleted()) {
            throw new ConflictException("BUSINESS_DELETED", "Esta ficha fue eliminada");
        }
        Product product = new Product();
        product.business = business;
        product.status = ProductStatus.ACTIVE;
        product.apply(title, description, price, photoUrls, attributes, now);
        return product;
    }

    public void update(UserAccount actor, String title, String description, Long price, List<String> photoUrls,
                       ProductAttributes attributes, Instant now) {
        requireEditableBy(actor);
        apply(title, description, price, photoUrls, attributes, now);
    }

    public void hide(UserAccount actor) {
        requireEditableBy(actor);
        status = ProductStatus.HIDDEN;
    }

    public void show(UserAccount actor) {
        requireEditableBy(actor);
        status = ProductStatus.ACTIVE;
    }

    public void delete(UserAccount actor, Instant now) {
        requireEditableBy(actor);
        deletedAt = now;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** Shown to visitors: active, not deleted, and its business is in the directory. */
    public boolean isPublic() {
        return status == ProductStatus.ACTIVE && !isDeleted() && business.isPublic();
    }

    public String mainPhotoUrl() {
        return photos.isEmpty() ? null : photos.getFirst().getUrl();
    }

    public List<ProductPhoto> getPhotos() {
        return Collections.unmodifiableList(photos);
    }

    private void apply(String title, String description, Long price, List<String> photoUrls,
                       ProductAttributes attributes, Instant now) {
        // Validate everything first, so a rejected update leaves the product as it was.
        String validTitle = Validators.text("title", title, Validators.PRODUCT_TITLE_MAX, "el título");
        String validDescription = Validators.text("description", description, Validators.PRODUCT_DESCRIPTION_MAX, "la descripción");
        Long validPrice = Validators.price(price);
        List<String> validPhotos = validPhotos(photoUrls);
        ProductAttributes details = attributes == null ? ProductAttributes.NONE : attributes;
        String validMaterial = Validators.optionalText("material", details.material(), MATERIAL_MAX, "el material");

        this.title = validTitle;
        this.description = validDescription;
        this.price = validPrice;
        this.size = details.size();
        this.condition = details.condition();
        this.material = validMaterial;
        this.offeringType = details.offeringType();
        if (!validPhotos.equals(photos.stream().map(ProductPhoto::getUrl).toList())) {
            photos.clear();
            for (int i = 0; i < validPhotos.size(); i++) {
                photos.add(new ProductPhoto(this, validPhotos.get(i), i + 1, now));
            }
        }
    }

    private static List<String> validPhotos(List<String> photoUrls) {
        if (photoUrls == null || photoUrls.isEmpty() || photoUrls.size() > MAX_PHOTOS) {
            throw new ValidationException("photoUrls", "Agrega entre 1 y 5 fotos");
        }
        return photoUrls.stream()
                .map(url -> Validators.url("photoUrls", url, true, "la foto"))
                .toList();
    }

    private void requireEditableBy(UserAccount actor) {
        requireOwner(business, actor);
        if (isDeleted()) {
            throw new ConflictException("PRODUCT_DELETED", "Este producto fue eliminado");
        }
    }

    private static void requireOwner(Business business, UserAccount actor) {
        actor.ensureActive();
        if (!business.isOwnedBy(actor)) {
            throw new ForbiddenException();
        }
    }
}
