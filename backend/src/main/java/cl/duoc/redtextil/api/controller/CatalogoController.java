package cl.duoc.redtextil.api.controller;

import cl.duoc.redtextil.api.dto.RespuestaApi;
import cl.duoc.redtextil.api.model.Publicacion;
import cl.duoc.redtextil.api.service.CatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService servicio;

    @GetMapping
    public List<Publicacion> listar(@RequestParam(required = false) Long fichaId) {
        return servicio.listar(fichaId);
    }

    @GetMapping("/{id}")
    public Publicacion obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Publicacion crear(@Valid @RequestBody Publicacion publicacion) {
        return servicio.crear(publicacion);
    }

    @PutMapping("/{id}")
    public Publicacion actualizar(@PathVariable Long id, @Valid @RequestBody Publicacion publicacion) {
        return servicio.actualizar(id, publicacion);
    }

    @DeleteMapping("/{id}")
    public RespuestaApi eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return new RespuestaApi(true, "Publicación " + id + " eliminada");
    }

    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RespuestaApi subirFoto(@PathVariable Long id, @RequestPart("foto") MultipartFile foto) {
        Publicacion publicacion = servicio.guardarFoto(id, foto);
        return new RespuestaApi(true, "Foto recibida: " + publicacion.getUrlFoto());
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<byte[]> foto(@PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(servicio.leerFoto(id));
    }
}
