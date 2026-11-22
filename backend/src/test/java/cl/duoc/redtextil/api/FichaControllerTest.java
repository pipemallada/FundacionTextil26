package cl.duoc.redtextil.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FichaControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private static final String FICHA = """
            {"idLocal": 7, "nombreEmprendedora": "Ana Pérez", "nombreComercial": "Renace Textil",
             "region": "Valparaíso", "comuna": "Viña del Mar", "rubros": ["UPCYCLING", "REPARACION"],
             "estado": "PENDIENTE", "origen": "APP"}
            """;

    @Test
    void crudCompletoDeFicha() throws Exception {
        String creada = mvc.perform(post("/fichas").contentType(MediaType.APPLICATION_JSON).content(FICHA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.rubros.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(creada).get("id").asLong();

        mvc.perform(get("/fichas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreComercial").value("Renace Textil"));

        mvc.perform(get("/fichas").param("region", "valparaíso"))
                .andExpect(jsonPath("$[*].id").value(hasItem((int) id)));

        JsonNode cambio = json.readTree(FICHA);
        ((com.fasterxml.jackson.databind.node.ObjectNode) cambio).put("estado", "ACTIVA");
        mvc.perform(put("/fichas/{id}", id).contentType(MediaType.APPLICATION_JSON).content(cambio.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));

        mvc.perform(delete("/fichas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
        mvc.perform(get("/fichas"))
                .andExpect(jsonPath("$[*].id").value(not(hasItem((int) id))));
    }

    @Test
    void rechazaFichaSinNombreComercial() throws Exception {
        mvc.perform(post("/fichas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreEmprendedora\": \"Ana\", \"nombreComercial\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void fichaInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/fichas/{id}", 999_999)).andExpect(status().isNotFound());
        mvc.perform(put("/fichas/{id}", 999_999).contentType(MediaType.APPLICATION_JSON).content(FICHA))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/fichas/{id}", 999_999)).andExpect(status().isNotFound());
    }
}
