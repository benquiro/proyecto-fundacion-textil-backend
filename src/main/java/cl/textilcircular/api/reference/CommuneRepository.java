package cl.textilcircular.api.reference;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommuneRepository extends JpaRepository<Commune, Long> {

    List<Commune> findByRegionIdOrderByNameAsc(Long regionId);
}
