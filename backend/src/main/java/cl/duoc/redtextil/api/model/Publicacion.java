package cl.duoc.redtextil.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.NoArgsConstructor;

// Trabajo del catalogo fotografico de una ficha
@Entity
@Data
@NoArgsConstructor
public class Publicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long idLocal;
    private Long fichaId;

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    @NotBlank(message = "La categoría es obligatoria")
    private String categoria;

    private String tecnica;

    @PositiveOrZero(message = "El precio no puede ser negativo")
    private Integer precioReferencial;

    @Column(length = 2000)
    private String descripcion;

    // Ruta relativa a /api para descargar la foto, por ejemplo "catalogo/5/foto"
    private String urlFoto;
}
