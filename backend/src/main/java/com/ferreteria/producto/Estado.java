/*
 * nombre: Estado.java
 * descripcion: Estado de un producto (borrado lógico).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.producto;

/** Estado de un producto. Un producto desactivado pasa a {@link #INACTIVO}; nunca se elimina. */
public enum Estado {
    /** Producto disponible para uso. */
    ACTIVO,
    /** Producto desactivado (borrado lógico). */
    INACTIVO
}
