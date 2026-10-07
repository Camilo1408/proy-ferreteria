/*
 * nombre: ApiTestBase.java
 * descripcion: Base de las pruebas de integración: contexto Spring con H2, limpieza de datos y ayudas HTTP.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ferreteria.alerta.AlertaRepository;
import com.ferreteria.inventario.MovimientoRepository;
import com.ferreteria.producto.ProductoRepository;
import com.ferreteria.security.Permiso;
import com.ferreteria.security.PerfilRepository;
import com.ferreteria.security.UsuarioRepository;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/** Cada prueba parte de una base limpia con solo los usuarios admin (ADMINISTRADOR) y user (CONSULTA). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestBase {

    protected static final String ADMIN_PW = "AdminTest1*";
    protected static final String USER_PW = "UserTest1*";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected AlertaRepository alertaRepo;
    @Autowired protected MovimientoRepository movimientoRepo;
    @Autowired protected ProductoRepository productoRepo;
    @Autowired protected UsuarioRepository usuarioRepo;
    @Autowired protected PerfilRepository perfilRepo;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected TransactionTemplate tx;

    /** Token del usuario admin (todos los permisos). */
    protected String admin;
    /** Token del usuario user (perfil CONSULTA). */
    protected String consulta;

    @BeforeEach
    void reiniciar() throws Exception {
        tx.executeWithoutResult(s -> {
            alertaRepo.deleteAll();
            movimientoRepo.deleteAll();
            productoRepo.deleteAll();
            usuarioRepo.findAll().stream().filter(u -> !Set.of("admin", "user").contains(u.getUsername()))
                    .forEach(usuarioRepo::delete);
            usuarioRepo.flush();
            perfilRepo.findAll().stream().filter(p -> !Set.of("ADMINISTRADOR", "CONSULTA").contains(p.getNombre()))
                    .forEach(perfilRepo::delete);
            perfilRepo.flush();
            var adminPerfil = perfilRepo.findByNombreIgnoreCase("ADMINISTRADOR").orElseThrow();
            var consultaPerfil = perfilRepo.findByNombreIgnoreCase("CONSULTA").orElseThrow();
            consultaPerfil.getPermisos().clear();
            consultaPerfil.getPermisos().addAll(EnumSet.of(Permiso.PRODUCTOS_VER, Permiso.MOVIMIENTOS_VER,
                    Permiso.ALERTAS_VER, Permiso.REPORTES_VER));
            for (var par : Map.of("admin", ADMIN_PW, "user", USER_PW).entrySet()) {
                var u = usuarioRepo.findByUsername(par.getKey()).orElseThrow();
                u.setPassword(encoder.encode(par.getValue()));
                u.setActivo(true);
                u.setPerfil(par.getKey().equals("admin") ? adminPerfil : consultaPerfil);
            }
        });
        admin = login("admin", ADMIN_PW);
        consulta = login("user", USER_PW);
    }

    /** Inicia sesión y devuelve el token. */
    protected String login(String usuario, String clave) throws Exception {
        String r = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", usuario, "password", clave))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(r).get("token").asText();
    }

    /** Ejecuta una petición con token opcional y cuerpo JSON opcional. */
    protected ResultActions send(MockHttpServletRequestBuilder b, String token, Object body) throws Exception {
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return mvc.perform(b);
    }

    /** Lee el cuerpo de una respuesta como JSON. */
    protected JsonNode cuerpo(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    /** Cuerpo base de un producto válido; se pueden sobrescribir campos con pares clave-valor. */
    protected Map<String, Object> producto(String codigo, String nombre, Object... extra) {
        Map<String, Object> m = new HashMap<>();
        m.put("codigo", codigo);
        m.put("nombre", nombre);
        m.put("categoria", "Herramientas");
        m.put("unidad", "UND");
        for (int i = 0; i < extra.length; i += 2) {
            m.put((String) extra[i], extra[i + 1]);
        }
        return m;
    }

    /** Crea un producto como admin y devuelve su id. */
    protected long crearProducto(String codigo, String nombre, Object minimo, Object inicial) throws Exception {
        var r = send(post("/api/v1/productos"), admin,
                producto(codigo, nombre, "stockMinimo", minimo, "stockInicial", inicial)).andExpect(status().isCreated());
        return cuerpo(r).get("id").asLong();
    }

    /** Crea un perfil como admin con los permisos dados y devuelve su id. */
    protected long crearPerfil(String nombre, Permiso... permisos) throws Exception {
        var r = send(post("/api/v1/perfiles"), admin, Map.of("nombre", nombre,
                "permisos", List.of(permisos).stream().map(Enum::name).toList())).andExpect(status().isCreated());
        return cuerpo(r).get("id").asLong();
    }

    /** Crea un usuario como admin con el perfil dado y devuelve su id. */
    protected long crearUsuario(String username, String clave, long perfilId) throws Exception {
        var r = send(post("/api/v1/usuarios"), admin, Map.of("username", username, "password", clave,
                "nombreCompleto", "Persona " + username, "perfilId", perfilId)).andExpect(status().isCreated());
        return cuerpo(r).get("id").asLong();
    }
}
