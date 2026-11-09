package cl.textilcircular.api.reference;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgeRangeRepository extends JpaRepository<AgeRange, Long> {

    List<AgeRange> findAllByOrderBySortOrderAsc();
}
