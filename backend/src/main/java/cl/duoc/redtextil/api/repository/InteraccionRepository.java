package cl.duoc.redtextil.api.repository;

import cl.duoc.redtextil.api.model.Interaccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InteraccionRepository extends JpaRepository<Interaccion, Long> {

    List<Interaccion> findByFichaId(Long fichaId);
}
