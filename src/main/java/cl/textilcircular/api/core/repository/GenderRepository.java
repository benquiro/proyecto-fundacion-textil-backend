package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.reference.Gender;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GenderRepository extends JpaRepository<Gender, Long> {

    List<Gender> findAllByOrderByIdAsc();
}
