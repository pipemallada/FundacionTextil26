package cl.duoc.redtextil.api.controller;

import cl.duoc.redtextil.api.dto.RespuestaApi;
import cl.duoc.redtextil.api.model.Consulta;
import cl.duoc.redtextil.api.service.ConsultaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/consultas")
@RequiredArgsConstructor
public class ConsultaController {

    private final ConsultaService servicio;

    @GetMapping
    public List<Consulta> listar(@RequestParam(required = false) Long fichaId) {
        return servicio.listar(fichaId);
    }

    @GetMapping("/{id}")
    public Consulta obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Consulta crear(@Valid @RequestBody Consulta consulta) {
        return servicio.crear(consulta);
    }

    @PutMapping("/{id}")
    public Consulta actualizar(@PathVariable Long id, @Valid @RequestBody Consulta consulta) {
        return servicio.actualizar(id, consulta);
    }

    @DeleteMapping("/{id}")
    public RespuestaApi eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return new RespuestaApi(true, "Consulta " + id + " eliminada");
    }
}
