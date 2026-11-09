package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.reference.Commune;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommuneRepository extends JpaRepository<Commune, Long> {

    List<Commune> findByRegionIdOrderByNameAsc(Long regionId);
}
