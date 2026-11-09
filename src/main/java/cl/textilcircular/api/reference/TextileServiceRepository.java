package cl.textilcircular.api.reference;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TextileServiceRepository extends JpaRepository<TextileService, Long> {

    List<TextileService> findByActiveTrueOrderByNameAsc();

    /** Active services among {@code ids}; used to reject unknown or retired services on save. */
    List<TextileService> findByIdInAndActiveTrue(Collection<Long> ids);
}
