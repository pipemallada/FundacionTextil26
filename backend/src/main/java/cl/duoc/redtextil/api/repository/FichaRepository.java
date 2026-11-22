package cl.duoc.redtextil.api.repository;

import cl.duoc.redtextil.api.model.Ficha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FichaRepository extends JpaRepository<Ficha, Long> {

    List<Ficha> findByRegionIgnoreCase(String region);

    Optional<Ficha> findFirstByRut(String rut);
}
