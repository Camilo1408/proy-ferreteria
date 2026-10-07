/*
 * nombre: ProductoApiIT.java
 * descripcion: Pruebas de integración y sistema de la API (criterios CA-01 a CA-12).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.producto;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Pruebas de extremo a extremo de la API con base H2 y seguridad real. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductoApiIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProductoRepository repo;

    String admin;
    String user;

    @BeforeEach
    void preparar() throws Exception {
        repo.deleteAll();
        admin = token("admin", "AdminTest1*");
        user = token("user", "UserTest1*");
    }

    String token(String u, String p) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", u, "password", p))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder b, String tk) {
        return b.header("Authorization", "Bearer " + tk).contentType(MediaType.APPLICATION_JSON);
    }

    String cuerpo(String nombre, String categoria, String descripcion) throws Exception {
        return json.writeValueAsString(new ProductoRequest(nombre, categoria, descripcion, null));
    }

    long crear(String nombre) throws Exception {
        String r = mvc.perform(como(post("/api/v1/productos"), admin).content(cuerpo(nombre, "Herramientas", "d")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(r).get("id").asLong();
    }

    @Test @DisplayName("CA-01 crear producto válido devuelve 201 con id y estado ACTIVO")
    void ca01() throws Exception {
        mvc.perform(como(post("/api/v1/productos"), admin).content(cuerpo("Martillo", "Herramientas", "Acero")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.estado").value("ACTIVO")).andExpect(jsonPath("$.nombre").value("Martillo"));
    }

    @Test @DisplayName("CA-02 nombre fuera de 3..100 devuelve 400 (límites 2/3/100/101)")
    void ca02() throws Exception {
        mvc.perform(como(post("/api/v1/productos"), admin).content(cuerpo("ab", "C", null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACION"));
        mvc.perform(como(post("/api/v1/productos"), admin).content(cuerpo("abc", "C", null)))
                .andExpect(status().isCreated());
        mvc.perform(como(post("/api/v1/productos"), admin).content(cuerpo("x".repeat(100), "C", null)))
                .andExpect(status().isCreated());
        mvc.perform(como(post("/api/v1/productos"), admin).content(cuerpo("x".repeat(101), "C", null)))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("CA-03 nombre duplicado (sin distinguir mayúsculas) devuelve 409")
    void ca03() throws Exception {
        crear("Taladro");
        mvc.perform(como(post("/api/v1/productos"), admin).content(cuerpo("TALADRO", "C", null)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("NOMBRE_DUPLICADO"));
    }

    @Test @DisplayName("CA-04 DELETE desactiva; el GET lo devuelve INACTIVO")
    void ca04() throws Exception {
        long id = crear("Sierra");
        mvc.perform(como(delete("/api/v1/productos/" + id), admin)).andExpect(status().isNoContent());
        mvc.perform(como(get("/api/v1/productos/" + id), admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test @DisplayName("CA-05 USER no puede crear, editar ni desactivar (403) pero sí leer")
    void ca05() throws Exception {
        long id = crear("Llave");
        mvc.perform(como(post("/api/v1/productos"), user).content(cuerpo("Nuevo", "C", null)))
                .andExpect(status().isForbidden());
        mvc.perform(como(put("/api/v1/productos/" + id), user).content(cuerpo("Otro", "C", null)))
                .andExpect(status().isForbidden());
        mvc.perform(como(delete("/api/v1/productos/" + id), user)).andExpect(status().isForbidden());
        mvc.perform(como(get("/api/v1/productos"), user)).andExpect(status().isOk());
    }

    @Test @DisplayName("CA-06 sin token o con token inválido se recibe 401")
    void ca06() throws Exception {
        mvc.perform(get("/api/v1/productos")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mvc.perform(get("/api/v1/productos").header("Authorization", "Bearer basura"))
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("CA-07 mensajes según Accept-Language (es por defecto, en)")
    void ca07() throws Exception {
        mvc.perform(como(post("/api/v1/productos"), admin).content(cuerpo("ab", "C", null)))
                .andExpect(jsonPath("$.mensaje").value("Hay datos inválidos en la solicitud"));
        mvc.perform(como(post("/api/v1/productos"), admin).header("Accept-Language", "en")
                        .content(cuerpo("ab", "C", null)))
                .andExpect(jsonPath("$.mensaje").value("The request contains invalid data"))
                .andExpect(jsonPath("$.detalles[0]").value(containsString("between 3 and 100")));
    }

    @Test @DisplayName("CA-08 login correcto da token; credenciales malas dan 401")
    void ca08() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("CA-09 categoría obligatoria y descripción máx. 500 (límite 500/501)")
    void ca09() throws Exception {
        mvc.perform(como(post("/api/v1/productos"), admin).content(cuerpo("Martillo", " ", null)))
                .andExpect(status().isBadRequest());
        mvc.perform(como(post("/api/v1/productos"), admin).content(cuerpo("Martillo", "C", "d".repeat(500))))
                .andExpect(status().isCreated());
        mvc.perform(como(post("/api/v1/productos"), admin).content(cuerpo("Otro", "C", "d".repeat(501))))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("CA-10 actualizar cambia datos; renombrar a un nombre ajeno da 409; inexistente da 404")
    void ca10() throws Exception {
        long a = crear("Alicate");
        crear("Pinza");
        mvc.perform(como(put("/api/v1/productos/" + a), admin).content(cuerpo("Alicate Pro", "Manuales", "x")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Alicate Pro"));
        mvc.perform(como(put("/api/v1/productos/" + a), admin).content(cuerpo("pinza", "C", null)))
                .andExpect(status().isConflict());
        mvc.perform(como(put("/api/v1/productos/99999"), admin).content(cuerpo("Nada", "C", null)))
                .andExpect(status().isNotFound());
        mvc.perform(como(get("/api/v1/productos/99999"), admin)).andExpect(status().isNotFound());
    }

    @Test @DisplayName("CA-11 listado paginado, filtro por estado y búsqueda")
    void ca11() throws Exception {
        for (int i = 1; i <= 12; i++) crear("Producto " + i);
        long id = crear("Cinta");
        mvc.perform(como(delete("/api/v1/productos/" + id), admin));
        mvc.perform(como(get("/api/v1/productos?size=5"), admin)).andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(13)).andExpect(jsonPath("$.totalPages").value(3));
        mvc.perform(como(get("/api/v1/productos?estado=INACTIVO"), admin)).andExpect(jsonPath("$.content", hasSize(1)));
        mvc.perform(como(get("/api/v1/productos?q=cinta"), admin)).andExpect(jsonPath("$.content", hasSize(1)));
        mvc.perform(como(get("/api/v1/productos?page=50"), admin)).andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(como(get("/api/v1/productos?size=100000"), admin)).andExpect(jsonPath("$.size").value(100));
    }

    @Test @DisplayName("CA-12 seguridad: JSON roto da 400 y la búsqueda no es inyectable")
    void ca12() throws Exception {
        mvc.perform(como(post("/api/v1/productos"), admin).content("{roto")).andExpect(status().isBadRequest());
        crear("Normal");
        mvc.perform(como(get("/api/v1/productos?q=' OR '1'='1"), admin)).andExpect(jsonPath("$.content", hasSize(0)));
        JsonNode tabla = json.readTree(mvc.perform(como(get("/api/v1/productos"), admin)).andReturn().getResponse()
                .getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(1, tabla.get("totalElements").asInt());
    }
}
