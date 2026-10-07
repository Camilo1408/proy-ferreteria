/*
 * nombre: AlertaApiIT.java
 * descripcion: Pruebas de integración de alertas de stock mínimo y listado en mínimos (CA-31 a CA-39).
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.alerta;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ferreteria.ApiTestBase;
import com.ferreteria.security.Permiso;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlertaApiIT extends ApiTestBase {

    private void salida(long id, Object cantidad) throws Exception {
        send(post("/api/v1/movimientos/salidas"), admin, Map.of("productoId", id, "cantidad", cantidad, "motivo", "VENTA"))
                .andExpect(status().isCreated());
    }

    private void entrada(long id, Object cantidad) throws Exception {
        send(post("/api/v1/movimientos/entradas"), admin, Map.of("productoId", id, "cantidad", cantidad, "motivo", "COMPRA"))
                .andExpect(status().isCreated());
    }

    private long alertaId() throws Exception {
        return cuerpo(send(get("/api/v1/alertas"), admin, null)).get("content").get(0).get("id").asLong();
    }

    @Test @DisplayName("CA-31 una salida que lleva el stock al mínimo crea una alerta ABIERTA nivel BAJO")
    void ca31() throws Exception {
        long id = crearProducto("AL-1", "Alertable", 5, 10);
        send(get("/api/v1/alertas"), admin, null).andExpect(jsonPath("$.content", hasSize(0)));
        salida(id, 4);
        send(get("/api/v1/alertas"), admin, null).andExpect(jsonPath("$.content", hasSize(0)));
        salida(id, 1);
        send(get("/api/v1/alertas"), admin, null).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].estado").value("ABIERTA")).andExpect(jsonPath("$.content[0].nivel").value("BAJO"))
                .andExpect(jsonPath("$.content[0].stockActual").value(5.0)).andExpect(jsonPath("$.content[0].faltante").value(0.0))
                .andExpect(jsonPath("$.content[0].codigo").value("AL-1"));
    }

    @Test @DisplayName("CA-32 si el stock llega a 0 el nivel pasa a AGOTADO")
    void ca32() throws Exception {
        long id = crearProducto("AL-2", "Agotable", 5, 6);
        salida(id, 2);
        send(get("/api/v1/alertas"), admin, null).andExpect(jsonPath("$.content[0].nivel").value("BAJO"))
                .andExpect(jsonPath("$.content[0].faltante").value(1.0));
        salida(id, 4);
        send(get("/api/v1/alertas"), admin, null).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nivel").value("AGOTADO"));
        send(get("/api/v1/alertas/resumen"), admin, null).andExpect(jsonPath("$.abiertas").value(1))
                .andExpect(jsonPath("$.agotados").value(1)).andExpect(jsonPath("$.bajos").value(0));
    }

    @Test @DisplayName("CA-33 no se duplican alertas por producto")
    void ca33() throws Exception {
        long id = crearProducto("AL-3", "Sin duplicar", 5, 10);
        salida(id, 6);
        salida(id, 1);
        salida(id, 1);
        send(get("/api/v1/alertas?size=50"), admin, null).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test @DisplayName("CA-34 al reponer por encima del mínimo la alerta pasa a RESUELTA sola")
    void ca34() throws Exception {
        long id = crearProducto("AL-4", "Reponible", 5, 5);
        send(get("/api/v1/alertas"), admin, null).andExpect(jsonPath("$.content", hasSize(1)));
        entrada(id, "0.001");
        send(get("/api/v1/alertas"), admin, null).andExpect(jsonPath("$.content", hasSize(0)));
        send(get("/api/v1/alertas?estado=RESUELTA"), admin, null).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].resueltaEn").isNotEmpty());
        send(get("/api/v1/alertas/resumen"), admin, null).andExpect(jsonPath("$.abiertas").value(0));
        salida(id, "0.001");
        send(get("/api/v1/alertas?size=50"), admin, null).andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test @DisplayName("CA-35 desactivar el producto resuelve su alerta; subir el mínimo la crea; mínimo 0 no alerta")
    void ca35() throws Exception {
        long id = crearProducto("AL-5", "Desactivable", 5, 2);
        send(get("/api/v1/alertas"), admin, null).andExpect(jsonPath("$.content", hasSize(1)));
        send(delete("/api/v1/productos/" + id), admin, null).andExpect(status().isNoContent());
        send(get("/api/v1/alertas"), admin, null).andExpect(jsonPath("$.content", hasSize(0)));
        long otro = crearProducto("AL-6", "Sin mínimo", 0, 0);
        send(get("/api/v1/alertas"), admin, null).andExpect(jsonPath("$.content", hasSize(0)));
        send(put("/api/v1/productos/" + otro), admin, producto("AL-6", "Sin mínimo", "stockMinimo", 3))
                .andExpect(jsonPath("$.nivel").value("AGOTADO"));
        send(get("/api/v1/alertas"), admin, null).andExpect(jsonPath("$.content", hasSize(1)));
        send(put("/api/v1/productos/" + otro), admin, producto("AL-6", "Sin mínimo", "stockMinimo", 0));
        send(get("/api/v1/alertas"), admin, null).andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test @DisplayName("CA-36 reconocer una alerta la pasa a RECONOCIDA, registra quién y deja de contar como pendiente")
    void ca36() throws Exception {
        long id = crearProducto("AL-7", "Reconocible", 5, 3);
        send(get("/api/v1/alertas/resumen"), admin, null).andExpect(jsonPath("$.abiertas").value(1));
        long alerta = alertaId();
        send(post("/api/v1/alertas/" + alerta + "/reconocer"), admin, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECONOCIDA")).andExpect(jsonPath("$.reconocidaPor").value("admin"));
        send(post("/api/v1/alertas/" + alerta + "/reconocer"), admin, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECONOCIDA"));
        send(get("/api/v1/alertas/resumen"), admin, null).andExpect(jsonPath("$.abiertas").value(0))
                .andExpect(jsonPath("$.bajos").value(1));
        send(get("/api/v1/alertas"), admin, null).andExpect(jsonPath("$.content", hasSize(1)));
        send(post("/api/v1/alertas/99999/reconocer"), admin, null).andExpect(status().isNotFound());
        entrada(id, 10);
        send(post("/api/v1/alertas/" + alerta + "/reconocer"), admin, null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("ALERTA_NO_RECONOCIBLE"));
    }

    @Test @DisplayName("CA-37 si el nivel empeora hasta AGOTADO la alerta reconocida vuelve a ABIERTA")
    void ca37() throws Exception {
        long id = crearProducto("AL-8", "Empeora", 5, 4);
        long alerta = alertaId();
        send(post("/api/v1/alertas/" + alerta + "/reconocer"), admin, null).andExpect(status().isOk());
        salida(id, 4);
        send(get("/api/v1/alertas"), admin, null).andExpect(jsonPath("$.content[0].estado").value("ABIERTA"))
                .andExpect(jsonPath("$.content[0].nivel").value("AGOTADO")).andExpect(jsonPath("$.content[0].reconocidaPor").doesNotExist());
        entrada(id, 2);
        send(get("/api/v1/alertas"), admin, null).andExpect(jsonPath("$.content[0].nivel").value("BAJO"))
                .andExpect(jsonPath("$.content[0].estado").value("ABIERTA"));
    }

    @Test @DisplayName("CA-38 bajoMinimo=true lista solo productos activos en o bajo su mínimo")
    void ca38() throws Exception {
        crearProducto("M-1", "En el mínimo", 5, 5);
        crearProducto("M-2", "Sobre el mínimo", 5, 6);
        crearProducto("M-3", "Sin mínimo", 0, 0);
        long inactivo = crearProducto("M-4", "Inactivo bajo", 5, 1);
        send(delete("/api/v1/productos/" + inactivo), admin, null);
        send(get("/api/v1/productos?bajoMinimo=true"), admin, null).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].codigo").value("M-1")).andExpect(jsonPath("$.content[0].nivel").value("BAJO"));
        send(get("/api/v1/productos?bajoMinimo=false"), admin, null).andExpect(jsonPath("$.content", hasSize(4)));
    }

    @Test @DisplayName("CA-39 el resumen devuelve pendientes, bajos y agotados")
    void ca39() throws Exception {
        crearProducto("R-1", "Bajo", 5, 3);
        crearProducto("R-2", "Agotado", 5, 0);
        crearProducto("R-3", "Agotado dos", 2, 0);
        send(get("/api/v1/alertas/resumen"), admin, null).andExpect(jsonPath("$.abiertas").value(3))
                .andExpect(jsonPath("$.bajos").value(1)).andExpect(jsonPath("$.agotados").value(2));
    }

    @Test @DisplayName("Permisos: ver alertas y reconocer son permisos distintos")
    void permisos() throws Exception {
        crearProducto("PA-1", "Para permisos", 5, 1);
        long alerta = alertaId();
        send(get("/api/v1/alertas"), consulta, null).andExpect(status().isOk());
        send(post("/api/v1/alertas/" + alerta + "/reconocer"), consulta, null).andExpect(status().isForbidden());
        long perfil = crearPerfil("Encargado de alertas", Permiso.ALERTAS_VER, Permiso.ALERTAS_GESTIONAR);
        crearUsuario("encargado", "Clave1234", perfil);
        String t = login("encargado", "Clave1234");
        send(post("/api/v1/alertas/" + alerta + "/reconocer"), t, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.reconocidaPor").value("encargado"));
        send(get("/api/v1/productos"), t, null).andExpect(status().isForbidden());
    }
}
