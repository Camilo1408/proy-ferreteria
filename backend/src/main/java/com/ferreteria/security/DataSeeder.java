/*
 * nombre: DataSeeder.java
 * descripcion: Crea los usuarios iniciales admin y user si no existen.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Siembra usuarios cuando {@code app.seed.enabled=true}. Las contraseñas llegan por configuración. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    @Value("${app.seed.admin-password}")
    private String claveAdmin;

    @Value("${app.seed.user-password}")
    private String claveUser;

    @Override
    public void run(String... args) {
        crear("admin", claveAdmin, Usuario.Rol.ADMIN);
        crear("user", claveUser, Usuario.Rol.USER);
    }

    private void crear(String nombre, String clave, Usuario.Rol rol) {
        if (repo.findByUsername(nombre).isEmpty()) {
            repo.save(Usuario.builder().username(nombre).password(encoder.encode(clave)).rol(rol).build());
        }
    }
}
