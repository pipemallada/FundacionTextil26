package cl.duoc.redtextil.api.service;

import cl.duoc.redtextil.api.exception.RecursoNoEncontradoException;
import cl.duoc.redtextil.api.model.Publicacion;
import cl.duoc.redtextil.api.repository.PublicacionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class CatalogoService {

    private final PublicacionRepository publicaciones;
    private final Path carpetaFotos;

    public CatalogoService(PublicacionRepository publicaciones, @Value("${app.fotos-dir}") String carpetaFotos) {
        this.publicaciones = publicaciones;
        this.carpetaFotos = Path.of(carpetaFotos);
    }

    public List<Publicacion> listar(Long fichaId) {
        return fichaId == null ? publicaciones.findAll() : publicaciones.findByFichaId(fichaId);
    }

    public Publicacion obtener(Long id) {
        return publicaciones.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Publicación", id));
    }

    public Publicacion crear(Publicacion publicacion) {
        publicacion.setId(null);
        publicacion.setUrlFoto(null);
        return publicaciones.save(publicacion);
    }

    public Publicacion actualizar(Long id, Publicacion datos) {
        Publicacion actual = obtener(id);
        datos.setId(id);
        // La app no manda la URL de la foto: se conserva la que ya estaba subida
        datos.setUrlFoto(actual.getUrlFoto());
        return publicaciones.save(datos);
    }

    public void eliminar(Long id) {
        obtener(id);
        try {
            Files.deleteIfExists(archivoDe(id));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        publicaciones.deleteById(id);
    }

    public Publicacion guardarFoto(Long id, MultipartFile foto) {
        Publicacion publicacion = obtener(id);
        if (foto == null || foto.isEmpty()) {
            throw new IllegalArgumentException("La foto está vacía");
        }
        String tipo = foto.getContentType();
        if (tipo == null || !tipo.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        }
        try (InputStream entrada = foto.getInputStream()) {
            Files.createDirectories(carpetaFotos);
            Files.copy(entrada, archivoDe(id), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        publicacion.setUrlFoto("catalogo/" + id + "/foto");
        return publicaciones.save(publicacion);
    }

    public byte[] leerFoto(Long id) {
        obtener(id);
        Path archivo = archivoDe(id);
        if (!Files.exists(archivo)) {
            throw new RecursoNoEncontradoException("Foto de la publicación", id);
        }
        try {
            return Files.readAllBytes(archivo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path archivoDe(Long id) {
        return carpetaFotos.resolve(id + ".jpg");
    }
}
