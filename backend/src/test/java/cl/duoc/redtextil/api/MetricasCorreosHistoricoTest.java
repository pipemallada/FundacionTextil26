package cl.duoc.redtextil.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MetricasCorreosHistoricoTest {

    @Autowired
    MockMvc mvc;

    @Test
    void registraInteraccionesYEntregaResumen() throws Exception {
        mvc.perform(post("/metricas/interacciones").contentType(MediaType.APPLICATION_JSON).content("""
                        [{"tipo": "BUSQUEDA", "fichaId": 3, "region": "Valparaíso", "rubro": "", "detalle": "chaquetas", "fecha": 1},
                         {"tipo": "CLIC_WHATSAPP", "fichaId": 3, "region": "Valparaíso", "rubro": "UPCYCLING", "detalle": "", "fecha": 2}]
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("2 registros recibidos"));

        mvc.perform(get("/metricas/resumen"))
                .andExpect(jsonPath("$.BUSQUEDA").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.CLIC_WHATSAPP").value(greaterThanOrEqualTo(1)));
        mvc.perform(get("/metricas/interacciones").param("fichaId", "3"))
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void rechazaInteraccionSinTipo() throws Exception {
        mvc.perform(post("/metricas/interacciones").contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"fichaId\": 3}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enviaYListaCorreos() throws Exception {
        mvc.perform(post("/correos/enviar").contentType(MediaType.APPLICATION_JSON).content("""
                        {"destinatario": "ana@example.com", "asunto": "Bienvenida", "cuerpo": "Hola Ana", "tipo": "BIENVENIDA"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
        mvc.perform(get("/correos"))
                .andExpect(jsonPath("$[0].estado").value("ENVIADO_OK"));
        mvc.perform(post("/correos/enviar").contentType(MediaType.APPLICATION_JSON).content("{\"asunto\": \"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void entregaLas982FichasHistoricas() throws Exception {
        mvc.perform(get("/historico/emprendimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(982))
                .andExpect(jsonPath("$[0].idHistorico").value("HIST-0001"))
                .andExpect(jsonPath("$[0].comunas.RM").value("Lo Prado"));
    }
}
