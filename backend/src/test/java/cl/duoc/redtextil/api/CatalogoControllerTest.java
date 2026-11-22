package cl.duoc.redtextil.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogoControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void crudCompletoConFoto() throws Exception {
        String creada = mvc.perform(post("/catalogo").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fichaId": 501, "titulo": "Chaqueta reciclada", "categoria": "PRENDA",
                         "tecnica": "Patchwork", "precioReferencial": 25000}
                        """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(creada).get("id").asLong();

        byte[] imagen = {(byte) 0xFF, (byte) 0xD8, 1, 2, 3};
        mvc.perform(multipart("/catalogo/{id}/foto", id)
                        .file(new MockMultipartFile("foto", "foto.jpg", "image/jpeg", imagen)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
        mvc.perform(get("/catalogo/{id}/foto", id))
                .andExpect(status().isOk())
                .andExpect(content().bytes(imagen));

        // Al editar se conserva la foto ya subida aunque la app no mande la URL
        mvc.perform(put("/catalogo/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"fichaId": 501, "titulo": "Chaqueta reciclada XL", "categoria": "PRENDA", "precioReferencial": 30000}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Chaqueta reciclada XL"))
                .andExpect(jsonPath("$.urlFoto").value("catalogo/" + id + "/foto"));

        mvc.perform(get("/catalogo").param("fichaId", "501"))
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(delete("/catalogo/{id}", id)).andExpect(status().isOk());
        mvc.perform(get("/catalogo/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/catalogo/{id}/foto", id)).andExpect(status().isNotFound());
    }

    @Test
    void validaPrecioYArchivo() throws Exception {
        mvc.perform(post("/catalogo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Bolso\", \"categoria\": \"ACCESORIO\", \"precioReferencial\": -1}"))
                .andExpect(status().isBadRequest());

        String creada = mvc.perform(post("/catalogo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Bolso\", \"categoria\": \"ACCESORIO\"}"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(creada).get("id").asLong();
        mvc.perform(multipart("/catalogo/{id}/foto", id)
                        .file(new MockMultipartFile("foto", "nota.txt", "text/plain", "hola".getBytes())))
                .andExpect(status().isBadRequest());
    }
}
