package cl.textilcircular.api.core.model.product;

import cl.textilcircular.api.core.model.account.Entrepreneur;
import cl.textilcircular.api.core.model.business.Business;
import cl.textilcircular.api.core.model.business.ReviewDecision;
import cl.textilcircular.api.core.error.ConflictException;
import cl.textilcircular.api.core.error.ForbiddenException;
import cl.textilcircular.api.core.error.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static cl.textilcircular.api.support.Fixtures.NOW;
import static cl.textilcircular.api.support.Fixtures.administrator;
import static cl.textilcircular.api.support.Fixtures.commune;
import static cl.textilcircular.api.support.Fixtures.entrepreneur;
import static cl.textilcircular.api.support.Fixtures.service;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    Entrepreneur owner = entrepreneur();
    Business business = Business.register(owner, "Taller", "Desc", null, commune(1L), List.of(service(1L)), NOW);
    ProductAttributes attributes = new ProductAttributes(ProductSize.M, ProductCondition.GOOD, " Algodón ", OfferingType.REPAIR);

    Product product() {
        return Product.create(business, owner.getAccount(), " Chaqueta ", " Reparada ", 15000L,
                List.of("https://img/1.jpg", " https://img/2.jpg "), attributes, NOW);
    }

    @Test
    void ownerCreatesAnActiveProductWithOrderedPhotos() {
        Product product = product();

        assertThat(product.getTitle()).isEqualTo("Chaqueta");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(product.getPrice()).isEqualTo(15000L);
        assertThat(product.getMaterial()).isEqualTo("Algodón");
        assertThat(product.getSize()).isEqualTo(ProductSize.M);
        assertThat(product.getPhotos()).extracting(ProductPhoto::getSortOrder).containsExactly(1, 2);
        assertThat(product.mainPhotoUrl()).isEqualTo("https://img/1.jpg");
    }

    @Test
    void onlyTheBusinessOwnerManagesProducts() {
        Entrepreneur other = entrepreneur();

        assertThatThrownBy(() -> Product.create(business, other.getAccount(), "T", "D", null,
                List.of("https://img/1.jpg"), ProductAttributes.NONE, NOW))
                .isInstanceOf(ForbiddenException.class);

        Product product = product();
        assertThatThrownBy(() -> product.hide(other.getAccount())).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> product.delete(other.getAccount(), NOW)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void productNeedsOneToFivePhotos() {
        for (List<String> photos : List.of(List.<String>of(), Collections.nCopies(6, "https://img/x.jpg"))) {
            assertThatThrownBy(() -> Product.create(business, owner.getAccount(), "T", "D", null, photos,
                    ProductAttributes.NONE, NOW))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Agrega entre 1 y 5 fotos")
                    .extracting("field").isEqualTo("photoUrls");
        }
    }

    @Test
    void photosMustBeWebOrMediaLinks() {
        assertThatThrownBy(() -> Product.create(business, owner.getAccount(), "T", "D", null,
                List.of("https://img/1.jpg", "data:image/png;base64,AA"), ProductAttributes.NONE, NOW))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("photoUrls");
    }

    @Test
    void priceIsOptionalButNotNegative() {
        assertThatThrownBy(() -> Product.create(business, owner.getAccount(), "T", "D", -1L,
                List.of("https://img/1.jpg"), ProductAttributes.NONE, NOW))
                .hasMessage("El precio no puede ser negativo");

        Product free = Product.create(business, owner.getAccount(), "T", "D", null,
                List.of("https://img/1.jpg"), ProductAttributes.NONE, NOW);
        assertThat(free.getPrice()).isNull();
        assertThat(free.getSize()).isNull();
    }

    @Test
    void updateReplacesTextsAndPhotos() {
        Product product = product();

        product.update(owner.getAccount(), "Chaqueta nueva", "Otra", 9000L, List.of("https://img/3.jpg"),
                ProductAttributes.NONE, NOW);

        assertThat(product.getTitle()).isEqualTo("Chaqueta nueva");
        assertThat(product.getPhotos()).extracting(ProductPhoto::getUrl).containsExactly("https://img/3.jpg");
        assertThat(product.getMaterial()).isNull();
    }

    @Test
    void hideShowAndDelete() {
        Product product = product();

        product.hide(owner.getAccount());
        assertThat(product.getStatus()).isEqualTo(ProductStatus.HIDDEN);
        product.show(owner.getAccount());
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        product.delete(owner.getAccount(), NOW);
        assertThat(product.isDeleted()).isTrue();
        assertThatThrownBy(() -> product.show(owner.getAccount())).isInstanceOf(ConflictException.class);
    }

    @Test
    void productIsPublicOnlyWhileActiveAndItsBusinessIsPublic() {
        Product product = product();
        assertThat(product.isPublic()).as("business still pending").isFalse();

        business.vote(administrator(), ReviewDecision.APPROVED, null, null, NOW);
        business.vote(administrator(), ReviewDecision.APPROVED, null, null, NOW);
        assertThat(product.isPublic()).isTrue();

        product.hide(owner.getAccount());
        assertThat(product.isPublic()).as("hidden").isFalse();
        product.show(owner.getAccount());
        product.delete(owner.getAccount(), NOW);
        assertThat(product.isPublic()).as("deleted").isFalse();
    }

    @Test
    void rejectedUpdateLeavesTheProductUntouched() {
        Product product = product();

        assertThatThrownBy(() -> product.update(owner.getAccount(), "Nuevo", "Otra", 1L, List.of("https://img/9.jpg"),
                new ProductAttributes(null, null, "x".repeat(101), null), NOW))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("material");

        assertThat(product.getTitle()).isEqualTo("Chaqueta");
        assertThat(product.getPhotos()).extracting(ProductPhoto::getUrl).containsExactly("https://img/1.jpg", "https://img/2.jpg");
    }

    @Test
    void failurePathsOfOwnershipAndDeletion() {
        Product product = product();
        Entrepreneur other = entrepreneur();

        assertThatThrownBy(() -> product.update(other.getAccount(), "T", "D", null, List.of("https://img/1.jpg"),
                ProductAttributes.NONE, NOW)).isInstanceOf(ForbiddenException.class);

        product.delete(owner.getAccount(), NOW);
        assertThatThrownBy(() -> product.hide(owner.getAccount())).isInstanceOf(ConflictException.class);

        business.softDelete(NOW);
        assertThatThrownBy(() -> Product.create(business, owner.getAccount(), "T", "D", null,
                List.of("https://img/1.jpg"), ProductAttributes.NONE, NOW))
                .isInstanceOf(ConflictException.class);
    }
}
