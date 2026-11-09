package cl.textilcircular.api.product;

import cl.textilcircular.api.business.Business;
import cl.textilcircular.api.support.PersistedData;
import cl.textilcircular.api.support.RepositoryTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static cl.textilcircular.api.support.Fixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RepositoryTest
class ProductRepositoriesTest {

    @Autowired ProductRepository products;
    @Autowired EntityManager em;
    PersistedData data;

    @BeforeEach
    void setUp() {
        data = new PersistedData(em);
    }

    Product save(Business business, String title) {
        return products.save(Product.create(business, business.getOwner().getAccount(), title, "Desc", 1000L,
                List.of("https://img/a.jpg", "https://img/b.jpg"),
                new ProductAttributes(ProductSize.ONE_SIZE, ProductCondition.NEW, "Lino", OfferingType.MADE_TO_ORDER), NOW));
    }

    @Test
    void photosAndAttributesArePersistedInOrder() {
        Product product = save(data.approvedBusiness("Taller", "Desc", 102L, 1L), "Bolso");
        data.flushAndClear();

        Product loaded = products.findById(product.getId()).orElseThrow();
        assertThat(loaded.getPhotos()).extracting(ProductPhoto::getUrl).containsExactly("https://img/a.jpg", "https://img/b.jpg");
        assertThat(loaded.getCondition()).isEqualTo(ProductCondition.NEW);
        assertThat(loaded.getOfferingType()).isEqualTo(OfferingType.MADE_TO_ORDER);
    }

    @Test
    void photosCanBeReplacedAndReordered() {
        Business business = data.approvedBusiness("Taller", "Desc", 102L, 1L);
        Product product = save(business, "Bolso");
        data.flushAndClear();

        Product loaded = products.findById(product.getId()).orElseThrow();
        loaded.update(business.getOwner().getAccount(), "Bolso", "Desc", 1000L,
                List.of("https://img/b.jpg", "https://img/c.jpg", "https://img/a.jpg"), ProductAttributes.NONE, NOW);
        data.flushAndClear();

        assertThat(products.findById(product.getId()).orElseThrow().getPhotos()).extracting(ProductPhoto::getUrl)
                .containsExactly("https://img/b.jpg", "https://img/c.jpg", "https://img/a.jpg");
    }

    @Test
    void publicListingShowsActiveProductsOfPublicBusinessesOnly() {
        Business published = data.approvedBusiness("Publicado", "Desc", 102L, 1L);
        Product visible = save(published, "Visible");
        Product hidden = save(published, "Oculto");
        hidden.hide(published.getOwner().getAccount());
        Product deleted = save(published, "Borrado");
        deleted.delete(published.getOwner().getAccount(), NOW);
        Product ofPending = save(data.pendingBusiness("Pendiente", "Desc", 102L, 1L), "De pendiente");
        data.flushAndClear();

        assertThat(products.findPublicByBusinessId(published.getId())).extracting(Product::getTitle).containsExactly("Visible");
        assertThat(products.findPublicById(visible.getId())).isPresent();
        assertThat(products.findPublicById(hidden.getId())).isEmpty();
        assertThat(products.findPublicById(ofPending.getId())).isEmpty();
        assertThat(products.findPublicById(deleted.getId())).isEmpty();
    }

    @Test
    void ownerCatalogFiltersByStatusAndCounts() {
        Business business = data.approvedBusiness("Taller", "Desc", 102L, 1L);
        save(business, "Uno");
        save(business, "Dos").hide(business.getOwner().getAccount());
        save(business, "Tres").delete(business.getOwner().getAccount(), NOW);
        data.flushAndClear();

        assertThat(products.findOwnerCatalog(business.getId(), null)).hasSize(2);
        assertThat(products.findOwnerCatalog(business.getId(), ProductStatus.HIDDEN)).extracting(Product::getTitle).containsExactly("Dos");
        assertThat(products.countByBusinessIdAndDeletedAtIsNull(business.getId())).isEqualTo(2);
        assertThat(products.countByBusinessIdAndStatusAndDeletedAtIsNull(business.getId(), ProductStatus.ACTIVE)).isEqualTo(1);
    }

    @Test
    void databaseRejectsANegativePrice() {
        Product product = save(data.approvedBusiness("Taller", "Desc", 102L, 1L), "Bolso");
        em.flush();

        assertThatThrownBy(() -> em.createNativeQuery("UPDATE product SET price = -5 WHERE product_id = :id")
                .setParameter("id", product.getId()).executeUpdate())
                .rootCause().hasMessageContaining("ck_product_price");
    }
}
