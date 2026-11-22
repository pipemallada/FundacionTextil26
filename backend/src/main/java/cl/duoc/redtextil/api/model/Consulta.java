package cl.duoc.redtextil.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

// Consulta que un cliente le envia a un emprendimiento desde el directorio
@Entity
@Data
@NoArgsConstructor
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long idLocal;
    private Long fichaId;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String nombreCliente;

    private String correoCliente;
    private String telefonoCliente;
    private String servicio;

    @NotBlank(message = "El mensaje es obligatorio")
    @Column(length = 2000)
    private String mensaje;

    private String estado;

    @Column(length = 2000)
    private String respuesta;

    private Long fechaEnvio;
}
