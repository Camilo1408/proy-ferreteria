/*
 * nombre: ProductoApiIT.java
 * descripcion: Pruebas de integración de productos (CA-01 a CA-12 y CA-16 a CA-19).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
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

import com.ferreteria.ApiTestBase;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductoApiIT extends ApiTestBase {

    @Test @DisplayName("CA-01 crear producto válido devuelve 201 con id, estado ACTIVO y stock 0")
    void ca01() throws Exception {
        send(post("/api/v1/productos"), admin, producto("MAR-001", "Martillo", "descripcion", "Acero"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.estado").value("ACTIVO")).andExpect(jsonPath("$.stockActual").value(0.0))
                .andExpect(jsonPath("$.nivel").value("OK")).andExpect(jsonPath("$.unidad").value("UND"));
    }

    @Test @DisplayName("CA-02 nombre fuera de 3..100 devuelve 400 (límites 2/3/100/101)")
    void ca02() throws Exception {
        send(post("/api/v1/productos"), admin, producto("A-1", "ab"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACION"));
        send(post("/api/v1/productos"), admin, producto("A-2", "abc")).andExpect(status().isCreated());
        send(post("/api/v1/productos"), admin, producto("A-3", "x".repeat(100))).andExpect(status().isCreated());
        send(post("/api/v1/productos"), admin, producto("A-4", "x".repeat(101))).andExpect(status().isBadRequest());
    }

    @Test @DisplayName("CA-03 nombre duplicado (sin distinguir mayúsculas) devuelve 409")
    void ca03() throws Exception {
        crearProducto("TAL-1", "Taladro", 0, 0);
        send(post("/api/v1/productos"), admin, producto("TAL-2", "TALADRO"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("NOMBRE_DUPLICADO"));
    }

    @Test @DisplayName("CA-04 DELETE desactiva; el GET lo devuelve INACTIVO")
    void ca04() throws Exception {
        long id = crearProducto("SIE-1", "Sierra", 0, 0);
        send(delete("/api/v1/productos/" + id), admin, null).andExpect(status().isNoContent());
        send(get("/api/v1/productos/" + id), admin, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test @DisplayName("CA-05 sin permiso de gestión no se crea, edita ni desactiva (403) pero sí se lee")
    void ca05() throws Exception {
        long id = crearProducto("LLA-1", "Llave", 0, 0);
        send(post("/api/v1/productos"), consulta, producto("N-1", "Nuevo")).andExpect(status().isForbidden());
        send(put("/api/v1/productos/" + id), consulta, producto("LLA-1", "Otro")).andExpect(status().isForbidden());
        send(delete("/api/v1/productos/" + id), consulta, null).andExpect(status().isForbidden());
        send(get("/api/v1/productos"), consulta, null).andExpect(status().isOk());
    }

    @Test @DisplayName("CA-06 sin token o con token inválido se recibe 401")
    void ca06() throws Exception {
        send(get("/api/v1/productos"), null, null).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        send(get("/api/v1/productos"), "basura", null).andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("CA-07 mensajes según Accept-Language (es por defecto, en)")
    void ca07() throws Exception {
        send(post("/api/v1/productos"), admin, producto("A-1", "ab"))
                .andExpect(jsonPath("$.mensaje").value("Hay datos inválidos en la solicitud"));
        mvc.perform(post("/api/v1/productos").header("Authorization", "Bearer " + admin)
                        .header("Accept-Language", "en").contentType("application/json")
                        .content(json.writeValueAsString(producto("A-1", "ab"))))
                .andExpect(jsonPath("$.mensaje").value("The request contains invalid data"))
                .andExpect(jsonPath("$.detalles[0]").value(containsString("between 3 and 100")));
    }

    @Test @DisplayName("CA-08 login correcto da token y sesión; credenciales malas dan 401; vacío da 400")
    void ca08() throws Exception {
        send(post("/api/v1/auth/login"), null, Map.of("username", "admin", "password", "mala"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
        send(post("/api/v1/auth/login"), null, Map.of("username", "", "password", ""))
                .andExpect(status().isBadRequest());
        send(post("/api/v1/auth/login"), null, Map.of("username", "ADMIN", "password", ADMIN_PW))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuario.perfil").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.usuario.permisos", hasSize(10)));
    }

    @Test @DisplayName("CA-09 categoría obligatoria y descripción máx. 500 (límite 500/501)")
    void ca09() throws Exception {
        send(post("/api/v1/productos"), admin, producto("A-1", "Martillo", "categoria", " ")).andExpect(status().isBadRequest());
        send(post("/api/v1/productos"), admin, producto("A-2", "Martillo", "descripcion", "d".repeat(500)))
                .andExpect(status().isCreated());
        send(post("/api/v1/productos"), admin, producto("A-3", "Otro", "descripcion", "d".repeat(501)))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("CA-10 actualizar cambia datos; renombrar a un nombre ajeno da 409; inexistente da 404")
    void ca10() throws Exception {
        long a = crearProducto("ALI-1", "Alicate", 0, 0);
        crearProducto("PIN-1", "Pinza", 0, 0);
        send(put("/api/v1/productos/" + a), admin, producto("ALI-1", "Alicate Pro", "categoria", "Manuales"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Alicate Pro"));
        send(put("/api/v1/productos/" + a), admin, producto("ALI-1", "pinza")).andExpect(status().isConflict());
        send(put("/api/v1/productos/99999"), admin, producto("NAD-1", "Nada")).andExpect(status().isNotFound());
        send(get("/api/v1/productos/99999"), admin, null).andExpect(status().isNotFound());
    }

    @Test @DisplayName("CA-11 listado paginado, filtro por estado, búsqueda por código/nombre y bajoMinimo")
    void ca11() throws Exception {
        for (int i = 1; i <= 12; i++) {
            crearProducto("P-" + i, "Producto " + i, 0, 0);
        }
        long id = crearProducto("CIN-1", "Cinta", 0, 0);
        send(delete("/api/v1/productos/" + id), admin, null);
        send(get("/api/v1/productos?size=5"), admin, null).andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(13)).andExpect(jsonPath("$.totalPages").value(3));
        send(get("/api/v1/productos?estado=INACTIVO"), admin, null).andExpect(jsonPath("$.content", hasSize(1)));
        send(get("/api/v1/productos?q=cinta"), admin, null).andExpect(jsonPath("$.content", hasSize(1)));
        send(get("/api/v1/productos?q=cin-1"), admin, null).andExpect(jsonPath("$.content", hasSize(1)));
        send(get("/api/v1/productos?page=50"), admin, null).andExpect(jsonPath("$.content", hasSize(0)));
        send(get("/api/v1/productos?size=100000"), admin, null).andExpect(jsonPath("$.size").value(100));
    }

    @Test @DisplayName("CA-12 JSON roto da 400, la búsqueda no es inyectable, rutas y métodos inexistentes no dan 500")
    void ca12() throws Exception {
        mvc.perform(post("/api/v1/productos").header("Authorization", "Bearer " + admin)
                .contentType("application/json").content("{roto")).andExpect(status().isBadRequest());
        crearProducto("NOR-1", "Normal", 0, 0);
        send(get("/api/v1/productos?q=' OR '1'='1"), admin, null).andExpect(jsonPath("$.content", hasSize(0)));
        send(get("/api/v1/productos?estado=NOEXISTE"), admin, null).andExpect(status().isBadRequest());
        send(get("/api/v1/no-existe"), admin, null).andExpect(status().isNotFound());
        send(delete("/api/v1/productos"), admin, null).andExpect(status().isMethodNotAllowed());
    }

    @Test @DisplayName("CA-16 código duplicado (sin distinguir mayúsculas) devuelve 409 CODIGO_DUPLICADO")
    void ca16() throws Exception {
        crearProducto("ABC-1", "Uno", 0, 0);
        send(post("/api/v1/productos"), admin, producto("abc-1", "Dos"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CODIGO_DUPLICADO"));
        long otro = crearProducto("ABC-2", "Tres", 0, 0);
        send(put("/api/v1/productos/" + otro), admin, producto("ABC-1", "Tres")).andExpect(status().isConflict());
        send(put("/api/v1/productos/" + otro), admin, producto("ABC-2", "Tres")).andExpect(status().isOk());
    }

    @Test @DisplayName("CA-17 código con formato inválido, unidad ausente o mínimo negativo devuelven 400")
    void ca17() throws Exception {
        send(post("/api/v1/productos"), admin, producto("a b", "Uno")).andExpect(status().isBadRequest());
        send(post("/api/v1/productos"), admin, producto("x", "Uno")).andExpect(status().isBadRequest());
        send(post("/api/v1/productos"), admin, producto("X".repeat(31), "Uno")).andExpect(status().isBadRequest());
        send(post("/api/v1/productos"), admin, producto("OK-1", "Uno", "unidad", null)).andExpect(status().isBadRequest());
        send(post("/api/v1/productos"), admin, producto("OK-2", "Uno", "unidad", "GALON")).andExpect(status().isBadRequest());
        send(post("/api/v1/productos"), admin, producto("OK-3", "Uno", "stockMinimo", -1)).andExpect(status().isBadRequest());
        send(post("/api/v1/productos"), admin, producto("OK-4", "Uno", "stockInicial", -5)).andExpect(status().isBadRequest());
        send(post("/api/v1/productos"), admin, producto("OK-5", "Uno", "stockMinimo", "1.2345")).andExpect(status().isBadRequest());
        send(post("/api/v1/productos"), admin, producto("O.K_6-7", "Uno", "stockMinimo", "1.234")).andExpect(status().isCreated());
    }

    @Test @DisplayName("CA-18 editar el producto no cambia su stock actual")
    void ca18() throws Exception {
        long id = crearProducto("STK-1", "Con stock", 2, 10);
        send(put("/api/v1/productos/" + id), admin, producto("STK-1", "Con stock", "stockInicial", 99, "stockMinimo", 4))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stockActual").value(10.0))
                .andExpect(jsonPath("$.stockMinimo").value(4.0));
    }

    @Test @DisplayName("CA-19 stock inicial registra una entrada STOCK_INICIAL")
    void ca19() throws Exception {
        long id = crearProducto("INI-1", "Inicial", 0, 25);
        send(get("/api/v1/productos/" + id), admin, null).andExpect(jsonPath("$.stockActual").value(25.0));
        send(get("/api/v1/movimientos?productoId=" + id), admin, null).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.content[0].motivo").value("STOCK_INICIAL"))
                .andExpect(jsonPath("$.content[0].stockAnterior").value(0.0))
                .andExpect(jsonPath("$.content[0].stockResultante").value(25.0));
    }
}
