/*
 * nombre: ReporteApiIT.java
 * descripcion: Pruebas de integración del panel y del reporte CSV (CA-40 y CA-41).
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.reporte;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ferreteria.ApiTestBase;
import com.ferreteria.security.Permiso;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReporteApiIT extends ApiTestBase {

    @Test @DisplayName("CA-40 el panel coincide con los datos")
    void ca40() throws Exception {
        crearProducto("D-1", "Normal", 2, 10);
        crearProducto("D-2", "Bajo", 5, 3);
        crearProducto("D-3", "Agotado", 5, 0);
        long baja = crearProducto("D-4", "Dado de baja", 5, 0);
        send(delete("/api/v1/productos/" + baja), admin, null);
        send(post("/api/v1/movimientos/salidas"), admin, Map.of("productoId", crearProducto("D-5", "Con salida", 0, 5),
                "cantidad", 1, "motivo", "VENTA"));
        send(get("/api/v1/dashboard"), admin, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.productosActivos").value(4)).andExpect(jsonPath("$.bajoMinimo").value(2))
                .andExpect(jsonPath("$.agotados").value(1)).andExpect(jsonPath("$.alertasAbiertas").value(2))
                .andExpect(jsonPath("$.movimientosHoy").value(4));
        send(get("/api/v1/dashboard"), null, null).andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("CA-41 el CSV tiene encabezado traducido, escapa comas y comillas y neutraliza fórmulas")
    void ca41() throws Exception {
        long id = crearProducto("CSV-1", "Tornillo, 1\" galvanizado", 0, 2);
        send(get("/api/v1/productos/" + id), admin, null);
        crearProducto("CSV-2", "=HYPERLINK(\"http://malo\")", 0, 0);
        crearProducto("CSV-3", "+cmd|calc", 0, 0);
        crearProducto("CSV-4", "@SUMA(1)", 0, 0);
        crearProducto("CSV-5", "-2+3", 0, 0);
        String csv = send(get("/api/v1/reportes/inventario.csv"), admin, null).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("inventario.csv")))
                .andExpect(content().contentTypeCompatibleWith("text/csv")).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("﻿Código,Nombre,Categoría,Unidad,Stock actual,Stock mínimo,Nivel,Estado\r\n"), csv);
        assertTrue(csv.contains("CSV-1,\"Tornillo, 1\"\" galvanizado\",Herramientas,UND,2,0,OK,ACTIVO"), csv);
        assertTrue(csv.contains("'=HYPERLINK(\"\"http://malo\"\")"), csv);
        assertTrue(csv.contains(",'+cmd|calc,"), csv);
        assertTrue(csv.contains(",'@SUMA(1),"), csv);
        assertTrue(csv.contains(",'-2+3,"), csv);
        assertEquals(6, csv.split("\r\n").length);
    }

    @Test @DisplayName("CA-41 el CSV sale en inglés con Accept-Language y puede limitarse a productos en mínimos")
    void ca41Ingles() throws Exception {
        crearProducto("L-1", "En mínimo", 5, 2);
        crearProducto("L-2", "Sobrado", 1, 9);
        String csv = mvc.perform(get("/api/v1/reportes/inventario.csv?bajoMinimo=true").header("Authorization", "Bearer " + admin)
                        .header("Accept-Language", "en")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertTrue(csv.contains("Code,Name,Category,Unit,Current stock,Minimum stock,Level,Status"), csv);
        assertTrue(csv.contains("L-1,"), csv);
        assertTrue(!csv.contains("L-2,"), csv);
    }

    @Test @DisplayName("El reporte exige REPORTES_VER y el panel exige PRODUCTOS_VER")
    void permisos() throws Exception {
        long sinReportes = crearPerfil("Sin reportes", Permiso.PRODUCTOS_VER);
        crearUsuario("norep", "Clave1234", sinReportes);
        String t = login("norep", "Clave1234");
        send(get("/api/v1/reportes/inventario.csv"), t, null).andExpect(status().isForbidden());
        send(get("/api/v1/dashboard"), t, null).andExpect(status().isOk());
        long soloReportes = crearPerfil("Solo reportes", Permiso.REPORTES_VER);
        crearUsuario("solorep", "Clave1234", soloReportes);
        String t2 = login("solorep", "Clave1234");
        send(get("/api/v1/reportes/inventario.csv"), t2, null).andExpect(status().isOk());
        send(get("/api/v1/dashboard"), t2, null).andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("PROHIBIDO"));
    }
}
