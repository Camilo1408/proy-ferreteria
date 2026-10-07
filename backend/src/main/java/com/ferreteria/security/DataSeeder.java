/*
 * nombre: DataSeeder.java
 * descripcion: Crea los perfiles base y los usuarios iniciales admin y user si no existen.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Siembra perfiles y usuarios cuando {@code app.seed.enabled=true}. Las contraseñas llegan por configuración. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    /** Nombre del perfil protegido con todos los permisos. */
    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    /** Perfil de solo consulta. */
    public static final String CONSULTA = "CONSULTA";

    private final UsuarioRepository usuarios;
    private final PerfilRepository perfiles;
    private final PasswordEncoder encoder;

    @Value("${app.seed.admin-password}")
    private String claveAdmin;

    @Value("${app.seed.user-password}")
    private String claveUser;

    @Override
    @Transactional
    public void run(String... args) {
        Perfil admin = perfil(ADMINISTRADOR, "Acceso total al sistema", true, EnumSet.allOf(Permiso.class));
        // Si el perfil ya existía, se asegura que conserve todos los permisos del catálogo vigente.
        admin.getPermisos().addAll(EnumSet.allOf(Permiso.class));
        Perfil consulta = perfil(CONSULTA, "Consulta de productos, movimientos, alertas y reportes", false,
                EnumSet.of(Permiso.PRODUCTOS_VER, Permiso.MOVIMIENTOS_VER, Permiso.ALERTAS_VER, Permiso.REPORTES_VER));
        usuario("admin", claveAdmin, "Administrador", admin);
        usuario("user", claveUser, "Usuario de consulta", consulta);
    }

    private Perfil perfil(String nombre, String descripcion, boolean sistema, Set<Permiso> permisos) {
        return perfiles.findByNombreIgnoreCase(nombre).orElseGet(() -> perfiles.save(
                Perfil.builder().nombre(nombre).descripcion(descripcion).sistema(sistema)
                        .permisos(new java.util.HashSet<>(permisos)).build()));
    }

    private void usuario(String username, String clave, String nombre, Perfil perfil) {
        if (usuarios.findByUsername(username).isEmpty()) {
            usuarios.save(Usuario.builder().username(username).password(encoder.encode(clave))
                    .nombreCompleto(nombre).perfil(perfil).build());
        }
    }
}
