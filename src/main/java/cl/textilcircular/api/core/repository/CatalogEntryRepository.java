package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.business.CatalogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/** Public directory. Reads only from {@code v_catalog}, so non-public businesses never leak. */
public interface CatalogEntryRepository extends Repository<CatalogEntry, Long> {

    Optional<CatalogEntry> findById(Long businessId);

    /**
     * Directory search. {@code term} matches part of the name or description ignoring case and
     * accents; every filter is optional. Results are shuffled with {@code seed}, so a client that
     * keeps its seed gets stable pages and different clients see different businesses first.
     */
    default Page<CatalogEntry> search(String term, Long regionId, Long communeId, Long serviceId,
                                      String seed, Pageable pageable) {
        return searchByPattern(likePattern(term), regionId, communeId, serviceId, seed == null ? "" : seed, pageable);
    }

    @Query(value = """
            SELECT c.* FROM v_catalog c
            WHERE (CAST(:pattern AS text) IS NULL
                   OR c.search_text LIKE immutable_unaccent(lower(CAST(:pattern AS text))) ESCAPE '\\')
              AND (CAST(:regionId AS bigint) IS NULL OR c.region_id = CAST(:regionId AS bigint))
              AND (CAST(:communeId AS bigint) IS NULL OR c.commune_id = CAST(:communeId AS bigint))
              AND (CAST(:serviceId AS bigint) IS NULL OR EXISTS (
                     SELECT 1 FROM business_service bs
                     WHERE bs.business_id = c.business_id AND bs.service_id = CAST(:serviceId AS bigint)))
            ORDER BY md5(c.business_id::text || CAST(:seed AS text)), c.business_id
            """,
            countQuery = """
            SELECT count(*) FROM v_catalog c
            WHERE (CAST(:pattern AS text) IS NULL
                   OR c.search_text LIKE immutable_unaccent(lower(CAST(:pattern AS text))) ESCAPE '\\')
              AND (CAST(:regionId AS bigint) IS NULL OR c.region_id = CAST(:regionId AS bigint))
              AND (CAST(:communeId AS bigint) IS NULL OR c.commune_id = CAST(:communeId AS bigint))
              AND (CAST(:serviceId AS bigint) IS NULL OR EXISTS (
                     SELECT 1 FROM business_service bs
                     WHERE bs.business_id = c.business_id AND bs.service_id = CAST(:serviceId AS bigint)))
            """,
            nativeQuery = true)
    Page<CatalogEntry> searchByPattern(String pattern, Long regionId, Long communeId, Long serviceId,
                                       String seed, Pageable pageable);

    /** {@code %term%} with LIKE wildcards escaped, or {@code null} for a blank term. */
    static String likePattern(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }
        String escaped = term.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
