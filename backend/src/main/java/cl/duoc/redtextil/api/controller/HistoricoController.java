package cl.duoc.redtextil.api.controller;

import cl.duoc.redtextil.api.model.FichaHistorica;
import cl.duoc.redtextil.api.service.HistoricoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/historico")
@RequiredArgsConstructor
public class HistoricoController {

    private final HistoricoService servicio;

    @GetMapping("/emprendimientos")
    public List<FichaHistorica> emprendimientos() {
        return servicio.listar();
    }
}
