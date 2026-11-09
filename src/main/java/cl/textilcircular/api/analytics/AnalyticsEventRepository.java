package cl.textilcircular.api.analytics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

/** Aggregations for the admin metrics live in the metrics slice; this covers recording and counting. */
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {

    /** Events of {@code type} in {@code [from, to)}. */
    long countByTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(AnalyticsEventType type, Instant from, Instant to);
}
