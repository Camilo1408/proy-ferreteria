/*
 * nombre: ProductoResponse.java
 * descripcion: DTO de salida de un producto con existencias y nivel.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.producto;

import java.math.BigDecimal;

/** Representación JSON de un producto. */
public record ProductoResponse(Long id, String codigo, String nombre, String categoria, String descripcion,
                               Unidad unidad, BigDecimal stockActual, BigDecimal stockMinimo, NivelStock nivel,
                               Estado estado) {

    /** Convierte la entidad en su DTO. */
    public static ProductoResponse de(Producto p) {
        return new ProductoResponse(p.getId(), p.getCodigo(), p.getNombre(), p.getCategoria(), p.getDescripcion(),
                p.getUnidad(), p.getStockActual(), p.getStockMinimo(), p.nivel(), p.getEstado());
    }
}
