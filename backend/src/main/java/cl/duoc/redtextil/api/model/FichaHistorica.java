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
import jakarta.persistence.MapKeyColumn;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.Map;
import java.util.TreeMap;

// Fila de la planilla antigua de Google Sheets (982 emprendimientos), tal como venia.
// La depuracion (region, comuna, telefono, rubros) la hace la app con DepuradorHistorico.
@Entity
@Data
@NoArgsConstructor
public class FichaHistorica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String idHistorico;

    private String marcaTemporal;
    private String nombreEmprendedora;
    private String nombreComercial;
    private String correo;
    private String telefono;
    private String region;

    // Columnas comuna_XX de la planilla (XX = codigo de region). Algunas filas traen mas de una llena
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ficha_historica_comuna", joinColumns = @JoinColumn(name = "ficha_historica_id"))
    @MapKeyColumn(name = "codigo_region")
    @Column(name = "comuna")
    @Fetch(FetchMode.SUBSELECT)
    private Map<String, String> comunas = new TreeMap<>();

    private String rubros;
    private String instagram;

    @Column(length = 2000)
    private String descripcion;
}
