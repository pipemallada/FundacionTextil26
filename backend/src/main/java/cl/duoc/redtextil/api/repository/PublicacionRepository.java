package cl.duoc.redtextil.api.repository;

import cl.duoc.redtextil.api.model.Publicacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {

    List<Publicacion> findByFichaId(Long fichaId);
}
