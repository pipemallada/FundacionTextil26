package cl.duoc.redtextil.api.service;

import cl.duoc.redtextil.api.exception.RecursoNoEncontradoException;
import cl.duoc.redtextil.api.model.Ficha;
import cl.duoc.redtextil.api.repository.ConsultaRepository;
import cl.duoc.redtextil.api.repository.FichaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FichaService {

    private final FichaRepository fichas;
    private final ConsultaRepository consultas;
    private final CatalogoService catalogo;

    public List<Ficha> listar(String region) {
        return (region == null || region.isBlank()) ? fichas.findAll() : fichas.findByRegionIgnoreCase(region.trim());
    }

    public Ficha obtener(Long id) {
        return fichas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Ficha", id));
    }

    public Ficha crear(Ficha ficha) {
        ficha.setId(null);
        return fichas.save(conFecha(ficha));
    }

    public Ficha actualizar(Long id, Ficha datos) {
        obtener(id);
        datos.setId(id);
        return fichas.save(conFecha(datos));
    }

    // Al borrar una ficha tambien se borran sus trabajos (con sus fotos) y sus consultas
    @Transactional
    public void eliminar(Long id) {
        obtener(id);
        catalogo.listar(id).forEach(publicacion -> catalogo.eliminar(publicacion.getId()));
        consultas.deleteAll(consultas.findByFichaId(id));
        fichas.deleteById(id);
    }

    private Ficha conFecha(Ficha ficha) {
        if (ficha.getActualizadoEn() == null) ficha.setActualizadoEn(System.currentTimeMillis());
        return ficha;
    }
}
