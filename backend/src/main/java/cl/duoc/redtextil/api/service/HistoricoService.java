package cl.duoc.redtextil.api.service;

import cl.duoc.redtextil.api.model.FichaHistorica;
import cl.duoc.redtextil.api.repository.FichaHistoricaRepository;
import cl.duoc.redtextil.api.util.CsvLector;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

// Carga una sola vez la planilla antigua (982 filas) en la base de datos para servirla a la app
@Service
@RequiredArgsConstructor
public class HistoricoService implements ApplicationRunner {

    private static final String ARCHIVO = "historico.csv";

    private final FichaHistoricaRepository historicas;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (historicas.count() == 0) {
            historicas.saveAll(leerDataset());
        }
    }

    public List<FichaHistorica> listar() {
        return historicas.findAll();
    }

    List<FichaHistorica> leerDataset() throws IOException {
        String texto = new ClassPathResource(ARCHIVO).getContentAsString(StandardCharsets.UTF_8);
        return CsvLector.leer(texto).stream().map(HistoricoService::aFicha).toList();
    }

    private static FichaHistorica aFicha(Map<String, String> fila) {
        FichaHistorica ficha = new FichaHistorica();
        ficha.setIdHistorico(fila.get("id_historico"));
        ficha.setMarcaTemporal(fila.get("marca_temporal"));
        ficha.setNombreEmprendedora(fila.get("nombre_emprendedora"));
        ficha.setNombreComercial(fila.get("nombre_comercial"));
        ficha.setCorreo(fila.get("correo"));
        ficha.setTelefono(fila.get("telefono"));
        ficha.setRegion(fila.get("region"));
        fila.forEach((columna, valor) -> {
            if (columna.startsWith("comuna_") && !valor.isBlank()) {
                ficha.getComunas().put(columna.substring("comuna_".length()), valor);
            }
        });
        ficha.setRubros(fila.get("rubros"));
        ficha.setInstagram(fila.get("instagram"));
        ficha.setDescripcion(fila.get("descripcion"));
        return ficha;
    }
}
