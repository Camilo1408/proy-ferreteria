/*
 * nombre: FerreteriaApplication.java
 * descripcion: Punto de entrada de la aplicación Spring Boot.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Aplicación principal del sistema de inventario de ferretería. */
@SpringBootApplication
public class FerreteriaApplication {

    /**
     * Arranca la aplicación.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(FerreteriaApplication.class, args);
    }
}
