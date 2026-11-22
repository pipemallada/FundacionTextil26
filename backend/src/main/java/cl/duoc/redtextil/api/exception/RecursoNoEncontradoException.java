package cl.duoc.redtextil.api.exception;

public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, Long id) {
        super(recurso + " " + id + " no existe");
    }
}
