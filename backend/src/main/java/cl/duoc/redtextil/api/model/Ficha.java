package cl.duoc.redtextil.api.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// Ficha publica de un emprendimiento. Los nombres de campo coinciden con FichaDto de la app
@Entity
@Data
@NoArgsConstructor
public class Ficha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long idLocal;

    @NotBlank(message = "El nombre de la emprendedora es obligatorio")
    private String nombreEmprendedora;

    private String rut;
    private String correo;
    private String telefono;

    @NotBlank(message = "El nombre comercial es obligatorio")
    private String nombreComercial;

    @Column(length = 2000)
    private String descripcion;

    private String region;
    private String comuna;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ficha_rubro", joinColumns = @JoinColumn(name = "ficha_id"))
    @Column(name = "rubro")
    private List<String> rubros = new ArrayList<>();

    private String whatsapp;
    private String instagram;
    private String sitioWeb;
    private String horarioAtencion;
    private String estado;
    private String origen;
    private Long actualizadoEn;
}
