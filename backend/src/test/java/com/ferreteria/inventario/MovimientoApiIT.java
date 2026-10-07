/*
 * nombre: MovimientoApiIT.java
 * descripcion: Pruebas de integración de entradas, salidas, ajustes e historial (CA-20 a CA-30).
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.inventario;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ferreteria.ApiTestBase;
import com.ferreteria.security.Permiso;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MovimientoApiIT extends ApiTestBase {

    private Map<String, Object> mov(long productoId, Object cantidad, String motivo) {
        Map<String, Object> m = new HashMap<>();
        m.put("productoId", productoId);
        m.put("cantidad", cantidad);
        m.put("motivo", motivo);
        return m;
    }

    private Map<String, Object> ajuste(long productoId, Object contado, String nota) {
        Map<String, Object> m = new HashMap<>();
        m.put("productoId", productoId);
        m.put("stockContado", contado);
        m.put("motivo", "CONTEO_FISICO");
        m.put("nota", nota);
        return m;
    }

    private double stock(long id) throws Exception {
        return cuerpo(send(get("/api/v1/productos/" + id), admin, null)).get("stockActual").asDouble();
    }

    @Test @DisplayName("CA-20 entrada suma stock y registra anterior y resultante, usuario y referencia")
    void ca20() throws Exception {
        long id = crearProducto("E-1", "Producto E", 0, 5);
        Map<String, Object> body = mov(id, 10, "COMPRA");
        body.put("referencia", "FAC-100");
        body.put("nota", "  lote nuevo ");
        send(post("/api/v1/movimientos/entradas"), admin, body).andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ENTRADA")).andExpect(jsonPath("$.variacion").value(10.0))
                .andExpect(jsonPath("$.stockAnterior").value(5.0)).andExpect(jsonPath("$.stockResultante").value(15.0))
                .andExpect(jsonPath("$.usuario").value("admin")).andExpect(jsonPath("$.referencia").value("FAC-100"))
                .andExpect(jsonPath("$.nota").value("lote nuevo")).andExpect(jsonPath("$.fecha").isNotEmpty());
        assertEquals(15.0, stock(id));
    }

    @Test @DisplayName("CA-21 cantidad 0, negativa, ausente o con más de 3 decimales devuelve 400")
    void ca21() throws Exception {
        long id = crearProducto("E-2", "Producto E2", 0, 5);
        for (Object c : new Object[]{0, -3, "1.2345", null}) {
            send(post("/api/v1/movimientos/entradas"), admin, mov(id, c, "COMPRA")).andExpect(status().isBadRequest());
        }
        send(post("/api/v1/movimientos/entradas"), admin, mov(id, "0.001", "COMPRA")).andExpect(status().isCreated());
        send(post("/api/v1/movimientos/entradas"), admin, mov(0, 1, "COMPRA")).andExpect(status().isNotFound());
        assertEquals(5.001, stock(id), 1e-9);
    }

    @Test @DisplayName("CA-22 un motivo que no corresponde al tipo devuelve 400 MOTIVO_INVALIDO")
    void ca22() throws Exception {
        long id = crearProducto("E-3", "Producto E3", 0, 5);
        send(post("/api/v1/movimientos/entradas"), admin, mov(id, 1, "VENTA"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("MOTIVO_INVALIDO"));
        send(post("/api/v1/movimientos/salidas"), admin, mov(id, 1, "COMPRA")).andExpect(status().isBadRequest());
        send(post("/api/v1/movimientos/ajustes"), admin, Map.of("productoId", id, "stockContado", 3, "motivo", "VENTA",
                "nota", "no aplica")).andExpect(status().isBadRequest());
        send(post("/api/v1/movimientos/entradas"), admin, mov(id, 1, "NO_EXISTE")).andExpect(status().isBadRequest());
        assertEquals(5.0, stock(id));
    }

    @Test @DisplayName("CA-23 salida mayor al stock devuelve 409 STOCK_INSUFICIENTE y no cambia nada")
    void ca23() throws Exception {
        long id = crearProducto("S-1", "Producto S", 0, 4);
        send(post("/api/v1/movimientos/salidas"), admin, mov(id, "4.001", "VENTA"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"))
                .andExpect(jsonPath("$.mensaje").value("Stock insuficiente: hay 4 disponibles"));
        assertEquals(4.0, stock(id));
        send(get("/api/v1/movimientos?productoId=" + id), admin, null).andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test @DisplayName("CA-24 salida igual al stock deja exactamente 0")
    void ca24() throws Exception {
        long id = crearProducto("S-2", "Producto S2", 0, 4);
        send(post("/api/v1/movimientos/salidas"), admin, mov(id, 4, "VENTA")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.variacion").value(-4.0)).andExpect(jsonPath("$.stockResultante").value(0.0));
        assertEquals(0.0, stock(id));
    }

    @Test @DisplayName("CA-25 salidas simultáneas de la última unidad: solo una se registra y el stock no es negativo")
    void ca25() throws Exception {
        long id = crearProducto("C-1", "Concurrente", 0, 5);
        int hilos = 12;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        try {
            List<Callable<Integer>> tareas = java.util.stream.IntStream.range(0, hilos).<Callable<Integer>>mapToObj(i -> () ->
                    send(post("/api/v1/movimientos/salidas"), admin, mov(id, 1, "VENTA")).andReturn().getResponse().getStatus())
                    .toList();
            int exitos = 0;
            int conflictos = 0;
            for (Future<Integer> f : pool.invokeAll(tareas)) {
                int s = f.get();
                if (s == 201) {
                    exitos++;
                } else if (s == 409) {
                    conflictos++;
                }
            }
            assertEquals(5, exitos, "deben registrarse exactamente tantas salidas como unidades había");
            assertEquals(hilos - 5, conflictos);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(0.0, stock(id));
    }

    @Test @DisplayName("CA-26 ajuste lleva el stock al valor contado con variación negativa o positiva")
    void ca26() throws Exception {
        long id = crearProducto("A-1", "Producto A", 0, 10);
        send(post("/api/v1/movimientos/ajustes"), admin, ajuste(id, 8, "Conteo del mes"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.tipo").value("AJUSTE"))
                .andExpect(jsonPath("$.variacion").value(-2.0)).andExpect(jsonPath("$.cantidad").value(2.0))
                .andExpect(jsonPath("$.stockResultante").value(8.0));
        send(post("/api/v1/movimientos/ajustes"), admin, ajuste(id, "9.5", "Apareció una caja"))
                .andExpect(jsonPath("$.variacion").value(1.5)).andExpect(jsonPath("$.stockResultante").value(9.5));
        send(post("/api/v1/movimientos/ajustes"), admin, ajuste(id, 0, "Todo dañado")).andExpect(status().isCreated());
        assertEquals(0.0, stock(id));
    }

    @Test @DisplayName("CA-27 ajuste sin diferencia da 409; sin nota o con nota corta da 400; contado negativo da 400")
    void ca27() throws Exception {
        long id = crearProducto("A-2", "Producto A2", 0, 10);
        send(post("/api/v1/movimientos/ajustes"), admin, ajuste(id, 10, "Sin cambios"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("AJUSTE_SIN_DIFERENCIA"));
        send(post("/api/v1/movimientos/ajustes"), admin, ajuste(id, 8, null)).andExpect(status().isBadRequest());
        send(post("/api/v1/movimientos/ajustes"), admin, ajuste(id, 8, "abcd")).andExpect(status().isBadRequest());
        send(post("/api/v1/movimientos/ajustes"), admin, ajuste(id, -1, "Negativo")).andExpect(status().isBadRequest());
        send(post("/api/v1/movimientos/ajustes"), admin, ajuste(id, 8, "abcde")).andExpect(status().isCreated());
    }

    @Test @DisplayName("CA-28 los movimientos son inmutables: no hay rutas para editar ni borrar")
    void ca28() throws Exception {
        long id = crearProducto("I-1", "Inmutable", 0, 3);
        long movId = cuerpo(send(get("/api/v1/movimientos?productoId=" + id), admin, null)).get("content").get(0).get("id").asLong();
        send(put("/api/v1/movimientos/" + movId), admin, Map.of("cantidad", 1)).andExpect(status().is4xxClientError());
        send(delete("/api/v1/movimientos/" + movId), admin, null).andExpect(status().is4xxClientError());
        send(delete("/api/v1/movimientos"), admin, null).andExpect(status().is4xxClientError());
    }

    @Test @DisplayName("CA-29 historial filtrable por producto, tipo y fechas, más reciente primero")
    void ca29() throws Exception {
        long a = crearProducto("H-1", "Hist A", 0, 10);
        long b = crearProducto("H-2", "Hist B", 0, 10);
        send(post("/api/v1/movimientos/salidas"), admin, mov(a, 3, "VENTA"));
        send(post("/api/v1/movimientos/salidas"), admin, mov(b, 1, "MERMA"));
        send(get("/api/v1/movimientos?productoId=" + a), admin, null).andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].tipo").value("SALIDA")).andExpect(jsonPath("$.content[0].codigo").value("H-1"));
        send(get("/api/v1/movimientos?tipo=SALIDA"), admin, null).andExpect(jsonPath("$.totalElements").value(2));
        send(get("/api/v1/movimientos?tipo=ENTRADA"), admin, null).andExpect(jsonPath("$.totalElements").value(2));
        LocalDate hoyLocal = LocalDate.now(java.time.ZoneId.of("America/Bogota"));
        String hoy = hoyLocal.toString();
        String ayer = hoyLocal.minusDays(2).toString();
        send(get("/api/v1/movimientos?desde=" + hoy + "&hasta=" + hoy), admin, null).andExpect(jsonPath("$.totalElements").value(4));
        send(get("/api/v1/movimientos?desde=" + ayer + "&hasta=" + ayer), admin, null).andExpect(jsonPath("$.totalElements").value(0));
        send(get("/api/v1/movimientos?desde=2999-01-01"), admin, null).andExpect(jsonPath("$.totalElements").value(0));
        send(get("/api/v1/movimientos?desde=no-es-fecha"), admin, null).andExpect(status().isBadRequest());
        send(get("/api/v1/movimientos?size=1&page=1"), admin, null).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalPages").value(4));
    }

    @Test @DisplayName("CA-30 un producto inactivo no admite entradas, salidas ni ajustes")
    void ca30() throws Exception {
        long id = crearProducto("X-1", "Inactivo", 0, 5);
        send(delete("/api/v1/productos/" + id), admin, null).andExpect(status().isNoContent());
        send(post("/api/v1/movimientos/entradas"), admin, mov(id, 1, "COMPRA"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("PRODUCTO_INACTIVO"));
        send(post("/api/v1/movimientos/salidas"), admin, mov(id, 1, "VENTA")).andExpect(status().isConflict());
        send(post("/api/v1/movimientos/ajustes"), admin, ajuste(id, 1, "Intento")).andExpect(status().isConflict());
    }

    @Test @DisplayName("Permisos modulares: registrar entradas/salidas, ajustes y ver historial son permisos independientes")
    void permisosIndependientes() throws Exception {
        long id = crearProducto("P-1", "Permisos", 0, 5);
        long soloEntradas = crearPerfil("Bodega", Permiso.PRODUCTOS_VER, Permiso.MOVIMIENTOS_REGISTRAR);
        long soloAjustes = crearPerfil("Auditor de stock", Permiso.PRODUCTOS_VER, Permiso.AJUSTES_REGISTRAR, Permiso.MOVIMIENTOS_VER);
        crearUsuario("bodega", "Clave1234", soloEntradas);
        crearUsuario("auditor", "Clave1234", soloAjustes);
        String bodega = login("bodega", "Clave1234");
        String auditor = login("auditor", "Clave1234");
        send(post("/api/v1/movimientos/entradas"), bodega, mov(id, 1, "COMPRA")).andExpect(status().isCreated());
        send(post("/api/v1/movimientos/salidas"), bodega, mov(id, 1, "VENTA")).andExpect(status().isCreated());
        send(post("/api/v1/movimientos/ajustes"), bodega, ajuste(id, 1, "No debería")).andExpect(status().isForbidden());
        send(get("/api/v1/movimientos"), bodega, null).andExpect(status().isForbidden());
        send(post("/api/v1/movimientos/ajustes"), auditor, ajuste(id, 2, "Conteo físico")).andExpect(status().isCreated());
        send(post("/api/v1/movimientos/entradas"), auditor, mov(id, 1, "COMPRA")).andExpect(status().isForbidden());
        send(get("/api/v1/movimientos"), auditor, null).andExpect(status().isOk());
        send(post("/api/v1/movimientos/entradas"), consulta, mov(id, 1, "COMPRA")).andExpect(status().isForbidden());
    }
}
