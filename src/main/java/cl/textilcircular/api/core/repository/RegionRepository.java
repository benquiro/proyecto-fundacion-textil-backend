package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.reference.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findAllByOrderBySortOrderAsc();
}
