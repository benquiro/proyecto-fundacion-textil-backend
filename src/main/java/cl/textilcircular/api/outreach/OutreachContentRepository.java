package cl.textilcircular.api.outreach;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OutreachContentRepository extends JpaRepository<OutreachContent, Long> {

    /** Home feed. */
    Page<OutreachContent> findByDeletedAtIsNullOrderByCreatedAtDescIdDesc(Pageable pageable);

    Optional<OutreachContent> findByIdAndDeletedAtIsNull(Long id);
}
