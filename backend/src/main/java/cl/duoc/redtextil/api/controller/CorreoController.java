package cl.duoc.redtextil.api.controller;

import cl.duoc.redtextil.api.dto.RespuestaApi;
import cl.duoc.redtextil.api.model.Correo;
import cl.duoc.redtextil.api.repository.CorreoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/correos")
@RequiredArgsConstructor
public class CorreoController {

    private final CorreoRepository correos;

    // Sin servidor SMTP configurado: el correo queda registrado como enviado.
    // Para envio real se agrega spring-boot-starter-mail y se llama a JavaMailSender aqui.
    @PostMapping("/enviar")
    public RespuestaApi enviar(@Valid @RequestBody Correo correo) {
        correo.setId(null);
        correo.setEstado("ENVIADO_OK");
        correo.setFechaEnvio(System.currentTimeMillis());
        correos.save(correo);
        return new RespuestaApi(true, "Correo enviado a " + correo.getDestinatario());
    }

    @GetMapping
    public List<Correo> listar() {
        return correos.findAll();
    }
}
