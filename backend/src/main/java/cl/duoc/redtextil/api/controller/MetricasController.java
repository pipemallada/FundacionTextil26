package cl.duoc.redtextil.api.controller;

import cl.duoc.redtextil.api.dto.RespuestaApi;
import cl.duoc.redtextil.api.model.Interaccion;
import cl.duoc.redtextil.api.repository.InteraccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/metricas")
@RequiredArgsConstructor
public class MetricasController {

    private final InteraccionRepository interacciones;

    // La app manda las interacciones en lote cuando recupera la conexion
    @PostMapping("/interacciones")
    public RespuestaApi registrar(@RequestBody List<Interaccion> lote) {
        if (lote.stream().anyMatch(i -> i.getTipo() == null || i.getTipo().isBlank())) {
            throw new IllegalArgumentException("Cada interacción debe indicar su tipo");
        }
        lote.forEach(i -> i.setId(null));
        interacciones.saveAll(lote);
        return new RespuestaApi(true, lote.size() + " registros recibidos");
    }

    @GetMapping("/interacciones")
    public List<Interaccion> listar(@RequestParam(required = false) Long fichaId) {
        return fichaId == null ? interacciones.findAll() : interacciones.findByFichaId(fichaId);
    }

    // Total de interacciones por tipo, por ejemplo {"BUSQUEDA": 12, "CLIC_WHATSAPP": 3}
    @GetMapping("/resumen")
    public Map<String, Long> resumen() {
        return interacciones.findAll().stream()
                .collect(Collectors.groupingBy(Interaccion::getTipo, TreeMap::new, Collectors.counting()));
    }
}
