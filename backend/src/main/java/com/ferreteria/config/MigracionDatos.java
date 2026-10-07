/*
 * nombre: MigracionDatos.java
 * descripcion: Completa datos de bases creadas con la versión anterior (código de productos).
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Se ejecuta antes que el sembrado. Es idempotente: sin filas por completar no hace nada. */
@Component
@Order(0)
@RequiredArgsConstructor
public class MigracionDatos implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        jdbc.update("UPDATE producto SET codigo = 'LEG-' || id WHERE codigo IS NULL");
    }
}
