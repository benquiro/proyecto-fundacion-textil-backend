package cl.textilcircular.api.core.model.analytics;

import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.model.business.Business;
import cl.textilcircular.api.core.model.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Append-only usage event recorded by the server when the directory and detail endpoints are
 * used. Replaces the app's invented metrics with real counts. References are plain ids: events
 * are never navigated, only aggregated. Visitors without a session have no actor.
 */
@Getter
@Entity
@Table(name = "analytics_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalyticsEvent {

    private static final int SEARCH_TERM_MAX = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "analytics_event_seq")
    @SequenceGenerator(name = "analytics_event_seq", sequenceName = "analytics_event_seq", allocationSize = 50)
    @Column(name = "event_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private AnalyticsEventType type;

    @Column(name = "actor_user_id", updatable = false)
    private Long actorUserId;

    @Column(name = "business_id", updatable = false)
    private Long businessId;

    @Column(name = "product_id", updatable = false)
    private Long productId;

    @Column(name = "region_id", updatable = false)
    private Long regionId;

    @Column(name = "commune_id", updatable = false)
    private Long communeId;

    @Column(name = "service_id", updatable = false)
    private Long serviceId;

    @Column(name = "search_term", length = SEARCH_TERM_MAX, updatable = false)
    private String searchTerm;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    /**
     * Search terms are free text and may contain personal data, so search events are never
     * linked to the user who searched.
     */
    public static AnalyticsEvent directorySearch(String term, Long regionId, Long communeId,
                                                 Long serviceId, Instant now) {
        AnalyticsEvent event = of(AnalyticsEventType.DIRECTORY_SEARCH, null, now);
        event.searchTerm = shorten(term);
        event.regionId = regionId;
        event.communeId = communeId;
        event.serviceId = serviceId;
        return event;
    }

    public static AnalyticsEvent businessView(UserAccount actor, Business business, Instant now) {
        AnalyticsEvent event = of(AnalyticsEventType.BUSINESS_VIEW, actor, now);
        event.businessId = business.getId();
        return event;
    }

    public static AnalyticsEvent productView(UserAccount actor, Product product, Instant now) {
        AnalyticsEvent event = of(AnalyticsEventType.PRODUCT_VIEW, actor, now);
        event.productId = product.getId();
        event.businessId = product.getBusiness().getId();
        return event;
    }

    public static AnalyticsEvent contactClick(UserAccount actor, Business business, Instant now) {
        AnalyticsEvent event = of(AnalyticsEventType.CONTACT_CLICK, actor, now);
        event.businessId = business.getId();
        return event;
    }

    private static AnalyticsEvent of(AnalyticsEventType type, UserAccount actor, Instant now) {
        AnalyticsEvent event = new AnalyticsEvent();
        event.type = type;
        event.actorUserId = actor == null ? null : actor.getId();
        event.occurredAt = now;
        return event;
    }

    private static String shorten(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }
        String trimmed = term.trim();
        return trimmed.length() > SEARCH_TERM_MAX ? trimmed.substring(0, SEARCH_TERM_MAX) : trimmed;
    }
}
