package cl.duoc.redtextil.api.exception;

import cl.duoc.redtextil.api.dto.RespuestaApi;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

// Todas las respuestas de error usan el mismo formato que entiende la app
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public RespuestaApi noEncontrado(RecursoNoEncontradoException e) {
        return new RespuestaApi(false, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RespuestaApi datosInvalidos(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return new RespuestaApi(false, detalle);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RespuestaApi solicitudInvalida(IllegalArgumentException e) {
        return new RespuestaApi(false, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public RespuestaApi fotoMuyGrande(MaxUploadSizeExceededException e) {
        return new RespuestaApi(false, "La foto supera el tamaño máximo permitido (10 MB)");
    }
}
