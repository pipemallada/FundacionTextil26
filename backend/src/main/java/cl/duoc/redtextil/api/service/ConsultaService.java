package cl.duoc.redtextil.api.service;

import cl.duoc.redtextil.api.exception.RecursoNoEncontradoException;
import cl.duoc.redtextil.api.model.Consulta;
import cl.duoc.redtextil.api.repository.ConsultaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultaService {

    private final ConsultaRepository consultas;

    public List<Consulta> listar(Long fichaId) {
        return fichaId == null ? consultas.findAll() : consultas.findByFichaId(fichaId);
    }

    public Consulta obtener(Long id) {
        return consultas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Consulta", id));
    }

    // Si la app reintenta el envio de la misma consulta (senal mala) no se duplica
    public Consulta crear(Consulta consulta) {
        consulta.setId(consulta.getFichaId() == null
                ? null
                : consultas.findFirstByFichaIdAndNombreClienteAndMensaje(
                        consulta.getFichaId(), consulta.getNombreCliente(), consulta.getMensaje())
                .map(Consulta::getId).orElse(null));
        if (consulta.getEstado() == null) consulta.setEstado("PENDIENTE");
        if (consulta.getFechaEnvio() == null) consulta.setFechaEnvio(System.currentTimeMillis());
        return consultas.save(consulta);
    }

    public Consulta actualizar(Long id, Consulta datos) {
        obtener(id);
        datos.setId(id);
        return consultas.save(datos);
    }

    public void eliminar(Long id) {
        obtener(id);
        consultas.deleteById(id);
    }
}
