package cl.duoc.redtextil.api.dto;

// Misma forma que RespuestaApi de la app: { "ok": true, "mensaje": "..." }
public record RespuestaApi(boolean ok, String mensaje) {
}
