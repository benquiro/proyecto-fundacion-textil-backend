package cl.textilcircular.api.analytics;

import cl.textilcircular.api.account.Entrepreneur;
import cl.textilcircular.api.business.Business;
import cl.textilcircular.api.product.Product;
import cl.textilcircular.api.product.ProductAttributes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static cl.textilcircular.api.support.Fixtures.NOW;
import static cl.textilcircular.api.support.Fixtures.customer;
import static cl.textilcircular.api.support.Fixtures.entrepreneur;
import static cl.textilcircular.api.support.Fixtures.publicBusiness;
import static cl.textilcircular.api.support.Fixtures.withId;
import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsEventTest {

    @Test
    void searchKeepsItsFiltersAndAShortTerm() {
        AnalyticsEvent event = AnalyticsEvent.directorySearch(null, "  " + "a".repeat(120) + " ", 13L, null, 4L, NOW);

        assertThat(event.getType()).isEqualTo(AnalyticsEventType.DIRECTORY_SEARCH);
        assertThat(event.getActorUserId()).isNull();
        assertThat(event.getSearchTerm()).hasSize(100);
        assertThat(event.getRegionId()).isEqualTo(13L);
        assertThat(event.getServiceId()).isEqualTo(4L);
        assertThat(AnalyticsEvent.directorySearch(null, " ", null, null, null, NOW).getSearchTerm()).isNull();
    }

    @Test
    void viewsAndContactsPointToTheirBusiness() {
        Entrepreneur owner = entrepreneur();
        Business business = publicBusiness(owner);
        Product product = withId(Product.create(business, owner.getAccount(), "Bolso", "Desc", null,
                List.of("https://img/1.jpg"), ProductAttributes.NONE, NOW), 77L);
        var visitor = customer();

        AnalyticsEvent view = AnalyticsEvent.businessView(visitor, business, NOW);
        AnalyticsEvent productView = AnalyticsEvent.productView(visitor, product, NOW);
        AnalyticsEvent contact = AnalyticsEvent.contactClick(null, business, NOW);

        assertThat(view.getBusinessId()).isEqualTo(business.getId());
        assertThat(view.getActorUserId()).isEqualTo(visitor.getId());
        assertThat(productView.getProductId()).isEqualTo(77L);
        assertThat(productView.getBusinessId()).isEqualTo(business.getId());
        assertThat(contact.getType()).isEqualTo(AnalyticsEventType.CONTACT_CLICK);
        assertThat(contact.getOccurredAt()).isEqualTo(NOW);
    }
}
