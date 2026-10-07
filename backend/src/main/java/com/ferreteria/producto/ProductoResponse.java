/*
 * nombre: ProductoResponse.java
 * descripcion: DTO de salida de un producto.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.producto;

/** Representación JSON de un producto. */
public record ProductoResponse(Long id, String nombre, String categoria, String descripcion, Estado estado) {

    /** Convierte la entidad en su DTO. */
    public static ProductoResponse de(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getCategoria(), p.getDescripcion(), p.getEstado());
    }
}
