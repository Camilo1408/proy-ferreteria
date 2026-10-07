/*
 * nombre: MigracionTest.java
 * descripcion: CA-57: una base creada con la primera versión arranca con el código actual y conserva sus productos.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.esquema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ferreteria.producto.Estado;
import com.ferreteria.producto.NivelStock;
import com.ferreteria.producto.ProductoRepository;
import com.ferreteria.producto.Unidad;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:legacy;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:legacy-schema.sql,classpath:migracion-previa.sql"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MigracionTest {

    @Autowired ProductoRepository productos;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @Test @DisplayName("CA-57 los productos heredados se conservan con código LEG-n, unidad UND y stock 0; los usuarios se rehacen")
    void conservaProductos() throws Exception {
        var todos = productos.findAll();
        assertEquals(2, todos.size());
        var martillo = todos.stream().filter(p -> p.getNombre().equals("Martillo heredado")).findFirst().orElseThrow();
        assertEquals("LEG-" + martillo.getId(), martillo.getCodigo());
        assertEquals(Unidad.UND, martillo.getUnidad());
        assertEquals(0, martillo.getStockActual().signum());
        assertEquals(0, martillo.getStockMinimo().signum());
        assertEquals(NivelStock.OK, martillo.nivel());
        assertEquals(Estado.INACTIVO, todos.stream().filter(p -> p.getNombre().equals("Taladro heredado")).findFirst().orElseThrow().getEstado());
        assertEquals(List.of(), jdbc.queryForList("select 1 from information_schema.tables where lower(table_name) = 'usuario'"),
                "la tabla de usuarios antigua (con rol) debe haberse eliminado");
        assertTrue(jdbc.queryForObject("select count(*) from app_usuario", Integer.class) >= 2);

        String r = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"AdminTest1*\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = new com.fasterxml.jackson.databind.ObjectMapper().readTree(r).get("token").asText();
        mvc.perform(get("/api/v1/productos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        // Se pueden registrar movimientos sobre un producto heredado.
        mvc.perform(post("/api/v1/movimientos/entradas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":" + martillo.getId() + ",\"cantidad\":5,\"motivo\":\"COMPRA\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.stockResultante").value(5.0));
    }
}
