package cl.textilcircular.api.reference;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GenderRepository extends JpaRepository<Gender, Long> {

    List<Gender> findAllByOrderByIdAsc();
}
