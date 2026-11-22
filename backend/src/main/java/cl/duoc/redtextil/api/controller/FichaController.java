package cl.duoc.redtextil.api.controller;

import cl.duoc.redtextil.api.dto.RespuestaApi;
import cl.duoc.redtextil.api.model.Ficha;
import cl.duoc.redtextil.api.service.FichaService;
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
@RequestMapping("/fichas")
@RequiredArgsConstructor
public class FichaController {

    private final FichaService servicio;

    @GetMapping
    public List<Ficha> listar(@RequestParam(required = false) String region) {
        return servicio.listar(region);
    }

    @GetMapping("/{id}")
    public Ficha obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Ficha crear(@Valid @RequestBody Ficha ficha) {
        return servicio.crear(ficha);
    }

    @PutMapping("/{id}")
    public Ficha actualizar(@PathVariable Long id, @Valid @RequestBody Ficha ficha) {
        return servicio.actualizar(id, ficha);
    }

    @DeleteMapping("/{id}")
    public RespuestaApi eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return new RespuestaApi(true, "Ficha " + id + " eliminada");
    }
}
