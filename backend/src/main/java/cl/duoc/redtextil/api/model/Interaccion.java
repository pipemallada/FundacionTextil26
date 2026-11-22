package cl.duoc.redtextil.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

// Busqueda, consulta o clic en WhatsApp/Instagram registrado por la app (trazabilidad)
@Entity
@Data
@NoArgsConstructor
public class Interaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El tipo de interacción es obligatorio")
    private String tipo;

    private Long fichaId;
    private String region;
    private String rubro;
    private String detalle;
    private Long fecha;
}
