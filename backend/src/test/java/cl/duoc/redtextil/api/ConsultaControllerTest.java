package cl.duoc.redtextil.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConsultaControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void crudCompletoDeConsulta() throws Exception {
        String creada = mvc.perform(post("/consultas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fichaId": 801, "nombreCliente": "Pedro Soto", "correoCliente": "pedro@example.com",
                         "servicio": "Reparación", "mensaje": "¿Arreglan cierres?"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.fechaEnvio").isNumber())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(creada).get("id").asLong();

        mvc.perform(put("/consultas/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"fichaId": 801, "nombreCliente": "Pedro Soto", "mensaje": "¿Arreglan cierres?",
                         "estado": "RESPONDIDA", "respuesta": "Sí, desde $3.000"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESPONDIDA"));

        mvc.perform(get("/consultas").param("fichaId", "801"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].respuesta").value("Sí, desde $3.000"));

        mvc.perform(delete("/consultas/{id}", id)).andExpect(status().isOk());
        mvc.perform(get("/consultas/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void rechazaConsultaSinMensaje() throws Exception {
        mvc.perform(post("/consultas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreCliente\": \"Pedro\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("mensaje: El mensaje es obligatorio"));
    }
}
