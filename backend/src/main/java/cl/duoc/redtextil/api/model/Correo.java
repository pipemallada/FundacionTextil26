package cl.duoc.redtextil.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

// Correo automatico (bienvenida, confirmacion, aprobacion u observacion) enviado por el servicio
@Entity
@Data
@NoArgsConstructor
public class Correo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El destinatario es obligatorio")
    private String destinatario;

    @NotBlank(message = "El asunto es obligatorio")
    private String asunto;

    @Column(length = 4000)
    private String cuerpo;

    private String tipo;
    private String estado;
    private Long fechaEnvio;
}
