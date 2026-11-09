package cl.textilcircular.api.account;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntrepreneurRepository extends JpaRepository<Entrepreneur, Long> {

    /** {@code rut} must already be normalized with {@code Validators.rut}. */
    boolean existsByRut(String rut);
}
