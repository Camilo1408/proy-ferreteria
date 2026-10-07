/*
 * nombre: AdministracionApiIT.java
 * descripcion: Pruebas de integración de perfiles, usuarios, permisos modulares y cuenta propia (CA-42 a CA-56).
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ferreteria.ApiTestBase;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdministracionApiIT extends ApiTestBase {

    private long idDe(String username) {
        return usuarioRepo.findByUsername(username).orElseThrow().getId();
    }

    private Map<String, Object> edicion(String nombre, long perfilId, boolean activo) {
        Map<String, Object> m = new HashMap<>();
        m.put("nombreCompleto", nombre);
        m.put("email", null);
        m.put("perfilId", perfilId);
        m.put("activo", activo);
        return m;
    }

    private long perfilAdmin() {
        return perfilRepo.findByNombreIgnoreCase("ADMINISTRADOR").orElseThrow().getId();
    }

    @Test @DisplayName("CA-42 un perfil nuevo con permisos elegidos se asigna a un usuario que solo puede lo permitido")
    void ca42() throws Exception {
        long perfil = crearPerfil("Vendedor", Permiso.PRODUCTOS_VER, Permiso.MOVIMIENTOS_REGISTRAR);
        crearUsuario("vendedor1", "Clave1234", perfil);
        String t = login("vendedor1", "Clave1234");
        send(get("/api/v1/productos"), t, null).andExpect(status().isOk());
        send(post("/api/v1/productos"), t, producto("V-1", "No puede")).andExpect(status().isForbidden());
        send(get("/api/v1/usuarios"), t, null).andExpect(status().isForbidden());
        send(get("/api/v1/perfiles"), t, null).andExpect(status().isForbidden());
        send(get("/api/v1/cuenta"), t, null).andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("Vendedor"))
                .andExpect(jsonPath("$.permisos", hasSize(2)));
    }

    @Test @DisplayName("CA-43 nombre de perfil duplicado devuelve 409; validaciones de perfil devuelven 400")
    void ca43() throws Exception {
        crearPerfil("Compras", Permiso.PRODUCTOS_VER);
        send(post("/api/v1/perfiles"), admin, Map.of("nombre", "compras", "permisos", List.of("PRODUCTOS_VER")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("PERFIL_DUPLICADO"));
        send(post("/api/v1/perfiles"), admin, Map.of("nombre", "ab", "permisos", List.of("PRODUCTOS_VER"))).andExpect(status().isBadRequest());
        send(post("/api/v1/perfiles"), admin, Map.of("nombre", "Sin permisos", "permisos", List.of())).andExpect(status().isBadRequest());
        send(post("/api/v1/perfiles"), admin, Map.of("nombre", "Permiso falso", "permisos", List.of("VOLAR"))).andExpect(status().isBadRequest());
        send(post("/api/v1/perfiles"), admin, Map.of("nombre", "Largo", "descripcion", "d".repeat(201),
                "permisos", List.of("PRODUCTOS_VER"))).andExpect(status().isBadRequest());
    }

    @Test @DisplayName("CA-44 el perfil ADMINISTRADOR no se edita ni se elimina")
    void ca44() throws Exception {
        long id = perfilAdmin();
        send(put("/api/v1/perfiles/" + id), admin, Map.of("nombre", "ADMINISTRADOR", "permisos", List.of("PRODUCTOS_VER")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("PERFIL_SISTEMA"));
        send(delete("/api/v1/perfiles/" + id), admin, null).andExpect(status().isConflict());
        send(get("/api/v1/perfiles"), admin, null).andExpect(jsonPath("$[?(@.nombre=='ADMINISTRADOR')].sistema", hasItem(true)));
    }

    @Test @DisplayName("CA-45 un perfil con usuarios no se elimina; sin usuarios sí; inexistente da 404")
    void ca45() throws Exception {
        long perfil = crearPerfil("Temporal", Permiso.PRODUCTOS_VER);
        crearUsuario("temp1", "Clave1234", perfil);
        send(delete("/api/v1/perfiles/" + perfil), admin, null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("PERFIL_EN_USO"));
        long libre = crearPerfil("Libre", Permiso.PRODUCTOS_VER);
        send(delete("/api/v1/perfiles/" + libre), admin, null).andExpect(status().isNoContent());
        send(delete("/api/v1/perfiles/" + libre), admin, null).andExpect(status().isNotFound());
    }

    @Test @DisplayName("CA-46 no se puede dejar al sistema sin un usuario activo que administre usuarios y perfiles")
    void ca46() throws Exception {
        long delegado = crearPerfil("Delegado", Permiso.USUARIOS_GESTIONAR, Permiso.PERFILES_GESTIONAR);
        crearUsuario("delegado1", "Clave1234", delegado);
        String t = login("delegado1", "Clave1234");
        // El delegado intenta quitarle a su propio perfil la gestión de perfiles: dejaría a admin como único responsable, válido.
        send(put("/api/v1/perfiles/" + delegado), t, Map.of("nombre", "Delegado", "permisos", List.of("USUARIOS_GESTIONAR")))
                .andExpect(status().isOk());
        // Ahora admin (ADMINISTRADOR) desactiva al delegado: sigue habiendo un administrador activo.
        send(put("/api/v1/usuarios/" + idDe("delegado1")), admin, edicion("Persona delegado1", delegado, false)).andExpect(status().isOk());
        // El delegado (inactivo) ya no entra.
        send(get("/api/v1/cuenta"), t, null).andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("CA-46 (último gestor) quitarle los permisos de administración al único gestor activo se rechaza y se revierte")
    void ca46Ultimo() throws Exception {
        long gestor = crearPerfil("Gestor", Permiso.USUARIOS_GESTIONAR, Permiso.PERFILES_GESTIONAR);
        crearUsuario("solo", "Clave1234", gestor);
        String solo = login("solo", "Clave1234");
        // Se desactiva a admin directamente en la base: 'solo' queda como único gestor activo.
        tx.executeWithoutResult(s -> usuarioRepo.findByUsername("admin").orElseThrow().setActivo(false));
        send(put("/api/v1/perfiles/" + gestor), solo, Map.of("nombre", "Gestor", "permisos", List.of("PRODUCTOS_VER")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("ULTIMO_ADMIN"));
        send(put("/api/v1/perfiles/" + gestor), solo, Map.of("nombre", "Gestor", "permisos", List.of("USUARIOS_GESTIONAR")))
                .andExpect(status().isConflict());
        send(get("/api/v1/perfiles"), solo, null).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre=='Gestor')].permisos[*]", hasItem("PERFILES_GESTIONAR")))
                .andExpect(jsonPath("$[?(@.nombre=='Gestor')].permisos[*]", hasItem("USUARIOS_GESTIONAR")));
        // Otro gestor activo permite el cambio.
        long segundo = cuerpo(send(post("/api/v1/perfiles"), solo, Map.of("nombre", "Gestor 2",
                "permisos", List.of("USUARIOS_GESTIONAR", "PERFILES_GESTIONAR"))).andExpect(status().isCreated())).get("id").asLong();
        send(post("/api/v1/usuarios"), solo, Map.of("username", "otro", "password", "Clave1234", "nombreCompleto", "Otro",
                "perfilId", segundo)).andExpect(status().isCreated());
        send(put("/api/v1/perfiles/" + gestor), solo, Map.of("nombre", "Gestor", "permisos", List.of("PRODUCTOS_VER")))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("CA-47 crear usuario con perfil; usuario duplicado (sin mayúsculas) da 409; el nombre se guarda en minúscula")
    void ca47() throws Exception {
        long perfil = crearPerfil("Operario", Permiso.PRODUCTOS_VER);
        send(post("/api/v1/usuarios"), admin, Map.of("username", "Maria.Lopez", "password", "Clave1234",
                "nombreCompleto", "María López", "email", "maria@ferreteria.test", "perfilId", perfil))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.username").value("maria.lopez"))
                .andExpect(jsonPath("$.perfil.nombre").value("Operario")).andExpect(jsonPath("$.password").doesNotExist());
        send(post("/api/v1/usuarios"), admin, Map.of("username", "MARIA.LOPEZ", "password", "Clave1234",
                "nombreCompleto", "Otra", "perfilId", perfil)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("USUARIO_DUPLICADO"));
        send(post("/api/v1/usuarios"), admin, Map.of("username", "x y", "password", "Clave1234", "nombreCompleto", "Mal",
                "perfilId", perfil)).andExpect(status().isBadRequest());
        send(post("/api/v1/usuarios"), admin, Map.of("username", "sinperfil", "password", "Clave1234", "nombreCompleto", "Mal",
                "perfilId", 99999)).andExpect(status().isNotFound());
        send(post("/api/v1/usuarios"), admin, Map.of("username", "malmail", "password", "Clave1234", "nombreCompleto", "Mal",
                "email", "no-es-correo", "perfilId", perfil)).andExpect(status().isBadRequest());
        send(get("/api/v1/usuarios?q=lópez"), admin, null).andExpect(jsonPath("$.content", hasSize(1)));
        send(get("/api/v1/usuarios?q=maria"), admin, null).andExpect(jsonPath("$.content", hasSize(1)));
        send(get("/api/v1/usuarios"), admin, null).andExpect(jsonPath("$.totalElements").value(3));
        login("MARIA.LOPEZ", "Clave1234");
    }

    @Test @DisplayName("CA-48 un usuario desactivado no inicia sesión y su token anterior deja de servir; reactivar lo devuelve")
    void ca48() throws Exception {
        long perfil = crearPerfil("Lector", Permiso.PRODUCTOS_VER);
        long id = crearUsuario("lector1", "Clave1234", perfil);
        String t = login("lector1", "Clave1234");
        send(get("/api/v1/productos"), t, null).andExpect(status().isOk());
        send(put("/api/v1/usuarios/" + id), admin, edicion("Lector Uno", perfil, false)).andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        send(get("/api/v1/productos"), t, null).andExpect(status().isUnauthorized());
        send(post("/api/v1/auth/login"), null, Map.of("username", "lector1", "password", "Clave1234"))
                .andExpect(status().isUnauthorized());
        send(put("/api/v1/usuarios/" + id), admin, edicion("Lector Uno", perfil, true)).andExpect(status().isOk());
        send(get("/api/v1/productos"), t, null).andExpect(status().isOk());
    }

    @Test @DisplayName("CA-49 nadie puede desactivarse ni cambiarse el perfil a sí mismo")
    void ca49() throws Exception {
        long perfil = crearPerfil("Otro perfil", Permiso.PRODUCTOS_VER);
        long miId = idDe("admin");
        send(put("/api/v1/usuarios/" + miId), admin, edicion("Administrador", perfilAdmin(), false))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("AUTO_MODIFICACION"));
        send(put("/api/v1/usuarios/" + miId), admin, edicion("Administrador", perfil, true)).andExpect(status().isConflict());
        send(put("/api/v1/usuarios/" + miId), admin, edicion("Administrador Renombrado", perfilAdmin(), true))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombreCompleto").value("Administrador Renombrado"));
        send(put("/api/v1/usuarios/99999"), admin, edicion("Nadie", perfil, true)).andExpect(status().isNotFound());
    }

    @Test @DisplayName("CA-50 contraseñas débiles o con espacios devuelven 400")
    void ca50() throws Exception {
        long perfil = crearPerfil("Perfil Pw", Permiso.PRODUCTOS_VER);
        for (String clave : new String[]{"corta1A", "sinnumeroaqui", "123456789", "con espacio 12", "x".repeat(73) + "1"}) {
            send(post("/api/v1/usuarios"), admin, Map.of("username", "pwuser", "password", clave, "nombreCompleto", "Pw",
                    "perfilId", perfil)).andExpect(status().isBadRequest());
        }
        send(post("/api/v1/usuarios"), admin, Map.of("username", "pwuser", "password", "Abcdef12", "nombreCompleto", "Pw",
                "perfilId", perfil)).andExpect(status().isCreated());
        send(put("/api/v1/usuarios/" + idDe("pwuser") + "/clave"), admin, Map.of("nueva", "débil")).andExpect(status().isBadRequest());
        send(put("/api/v1/usuarios/" + idDe("pwuser") + "/clave"), admin, Map.of("nueva", "Nueva2345")).andExpect(status().isNoContent());
        login("pwuser", "Nueva2345");
    }

    @Test @DisplayName("CA-51 cada endpoint exige su permiso: 403 sin él y 401 sin sesión")
    void ca51() throws Exception {
        String[][] rutas = {{"GET", "/api/v1/usuarios"}, {"GET", "/api/v1/perfiles"}, {"GET", "/api/v1/perfiles/permisos"},
                {"POST", "/api/v1/perfiles"}, {"POST", "/api/v1/usuarios"}, {"GET", "/actuator/metrics"},
                {"POST", "/api/v1/movimientos/entradas"}, {"POST", "/api/v1/movimientos/ajustes"},
                {"POST", "/api/v1/productos"}, {"POST", "/api/v1/alertas/1/reconocer"}};
        for (String[] r : rutas) {
            var b = r[0].equals("GET") ? get(r[1]) : post(r[1]);
            send(b, consulta, r[0].equals("GET") ? null : Map.of("x", 1)).andExpect(status().isForbidden());
            var b2 = r[0].equals("GET") ? get(r[1]) : post(r[1]);
            send(b2, null, r[0].equals("GET") ? null : Map.of("x", 1)).andExpect(status().isUnauthorized());
        }
        send(get("/actuator/health"), null, null).andExpect(status().isOk());
        send(get("/v3/api-docs"), null, null).andExpect(status().isOk());
    }

    @Test @DisplayName("CA-52 un cambio de permisos del perfil rige en el siguiente request, sin volver a iniciar sesión")
    void ca52() throws Exception {
        long perfil = crearPerfil("Cambiante", Permiso.PRODUCTOS_VER);
        crearUsuario("cambia1", "Clave1234", perfil);
        String t = login("cambia1", "Clave1234");
        send(post("/api/v1/productos"), t, producto("CH-1", "Antes")).andExpect(status().isForbidden());
        send(put("/api/v1/perfiles/" + perfil), admin, Map.of("nombre", "Cambiante",
                "permisos", List.of("PRODUCTOS_VER", "PRODUCTOS_GESTIONAR"))).andExpect(status().isOk());
        send(post("/api/v1/productos"), t, producto("CH-1", "Despues")).andExpect(status().isCreated());
        send(put("/api/v1/perfiles/" + perfil), admin, Map.of("nombre", "Cambiante", "permisos", List.of("PRODUCTOS_VER")))
                .andExpect(status().isOk());
        send(post("/api/v1/productos"), t, producto("CH-2", "Otra vez")).andExpect(status().isForbidden());
        send(get("/api/v1/perfiles"), admin, null).andExpect(jsonPath("$[?(@.nombre=='Cambiante')].usuarios", hasItem(1)));
    }

    @Test @DisplayName("Catálogo de permisos con su módulo")
    void catalogo() throws Exception {
        send(get("/api/v1/perfiles/permisos"), admin, null).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(Permiso.values().length)))
                .andExpect(jsonPath("$[?(@.codigo=='AJUSTES_REGISTRAR')].modulo", hasItem("inventario")));
    }

    @Test @DisplayName("CA-54 cambiar la contraseña con la actual incorrecta da 400 y no cambia nada")
    void ca54() throws Exception {
        send(put("/api/v1/cuenta/clave"), consulta, Map.of("actual", "incorrecta1", "nueva", "Nueva12345"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("CLAVE_ACTUAL_INCORRECTA"));
        login("user", USER_PW);
        send(put("/api/v1/cuenta/clave"), consulta, Map.of("nueva", "Nueva12345")).andExpect(status().isBadRequest());
        send(put("/api/v1/cuenta/clave"), consulta, Map.of("actual", USER_PW, "nueva", "corta1")).andExpect(status().isBadRequest());
    }

    @Test @DisplayName("CA-55 tras cambiar la contraseña la anterior ya no sirve y la nueva sí")
    void ca55() throws Exception {
        send(put("/api/v1/cuenta/clave"), consulta, Map.of("actual", USER_PW, "nueva", "Nueva12345")).andExpect(status().isNoContent());
        send(post("/api/v1/auth/login"), null, Map.of("username", "user", "password", USER_PW)).andExpect(status().isUnauthorized());
        login("user", "Nueva12345");
    }

    @Test @DisplayName("CA-56 la nueva contraseña no puede ser igual a la actual")
    void ca56() throws Exception {
        send(put("/api/v1/cuenta/clave"), consulta, Map.of("actual", USER_PW, "nueva", USER_PW))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("CLAVE_IGUAL"));
    }

    @Test @DisplayName("Cuenta propia: ver y editar nombre y correo (sin permisos especiales)")
    void cuentaPropia() throws Exception {
        send(get("/api/v1/cuenta"), consulta, null).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.perfil").value("CONSULTA")).andExpect(jsonPath("$.permisos", not(hasItem("USUARIOS_GESTIONAR"))));
        Map<String, Object> cambio = new HashMap<>();
        cambio.put("nombreCompleto", "  Nombre Nuevo ");
        cambio.put("email", "nuevo@correo.test");
        send(put("/api/v1/cuenta"), consulta, cambio).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Nombre Nuevo")).andExpect(jsonPath("$.email").value("nuevo@correo.test"));
        cambio.put("email", "malo");
        send(put("/api/v1/cuenta"), consulta, cambio).andExpect(status().isBadRequest());
        cambio.put("email", "");
        cambio.put("nombreCompleto", "");
        send(put("/api/v1/cuenta"), consulta, cambio).andExpect(status().isBadRequest());
        cambio.put("nombreCompleto", "Solo nombre");
        send(put("/api/v1/cuenta"), consulta, cambio).andExpect(status().isOk()).andExpect(jsonPath("$.email").doesNotExist());
    }
}
