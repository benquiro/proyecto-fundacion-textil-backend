package cl.textilcircular.api.core.model.product;

/** Optional structured details of a product; any of them may be {@code null}. */
public record ProductAttributes(ProductSize size, ProductCondition condition, String material,
                                OfferingType offeringType) {

    public static final ProductAttributes NONE = new ProductAttributes(null, null, null, null);
}
