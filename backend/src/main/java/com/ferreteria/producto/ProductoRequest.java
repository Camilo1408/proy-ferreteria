/*
 * nombre: ProductoRequest.java
 * descripcion: DTO de entrada para crear o actualizar un producto.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.producto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada de un producto.
 *
 * @param nombre      3 a 100 caracteres, obligatorio
 * @param categoria   obligatoria, hasta 60 caracteres
 * @param descripcion opcional, hasta 500 caracteres
 * @param estado      opcional; por defecto ACTIVO
 */
public record ProductoRequest(
        @NotBlank(message = "{producto.nombre.requerido}")
        @Size(min = 3, max = 100, message = "{producto.nombre.tamano}")
        String nombre,
        @NotBlank(message = "{producto.categoria.requerida}")
        @Size(max = 60, message = "{producto.categoria.tamano}")
        String categoria,
        @Size(max = 500, message = "{producto.descripcion.tamano}")
        String descripcion,
        Estado estado) {
}
