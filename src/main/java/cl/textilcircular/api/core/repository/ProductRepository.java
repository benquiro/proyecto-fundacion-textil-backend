package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.product.Product;
import cl.textilcircular.api.core.model.product.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Visible condition shared by the public queries: product active and its business in the directory. */
    String PUBLIC = """
            p.status = cl.textilcircular.api.core.model.product.ProductStatus.ACTIVE
            and p.deletedAt is null
            and b.status = cl.textilcircular.api.core.model.business.BusinessStatus.APPROVED
            and b.deletedAt is null
            and b.hiddenAt is null
            and a.status = cl.textilcircular.api.core.model.account.AccountStatus.ACTIVE
            """;

    @Query("select distinct p from Product p join p.business b join b.owner o join o.account a "
            + "left join fetch p.photos where b.id = :businessId and " + PUBLIC + " order by p.createdAt desc")
    List<Product> findPublicByBusinessId(Long businessId);

    @Query("select p from Product p join p.business b join b.owner o join o.account a "
            + "left join fetch p.photos where p.id = :productId and " + PUBLIC)
    Optional<Product> findPublicById(Long productId);

    /** "Mi catálogo": every non-deleted product, optionally only one status. Newest first. */
    @Query("""
            select p from Product p
            where p.business.id = :businessId and p.deletedAt is null
              and (:status is null or p.status = :status)
            order by p.createdAt desc
            """)
    List<Product> findOwnerCatalog(Long businessId, ProductStatus status);

    long countByBusinessIdAndDeletedAtIsNull(Long businessId);

    long countByBusinessIdAndStatusAndDeletedAtIsNull(Long businessId, ProductStatus status);
}
