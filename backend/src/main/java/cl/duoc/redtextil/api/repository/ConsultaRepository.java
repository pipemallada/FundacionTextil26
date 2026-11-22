package cl.duoc.redtextil.api.repository;

import cl.duoc.redtextil.api.model.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    List<Consulta> findByFichaId(Long fichaId);

    Optional<Consulta> findFirstByFichaIdAndNombreClienteAndMensaje(Long fichaId, String nombreCliente, String mensaje);
}
