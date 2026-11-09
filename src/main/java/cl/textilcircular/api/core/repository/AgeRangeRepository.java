package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.reference.AgeRange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgeRangeRepository extends JpaRepository<AgeRange, Long> {

    List<AgeRange> findAllByOrderBySortOrderAsc();
}
