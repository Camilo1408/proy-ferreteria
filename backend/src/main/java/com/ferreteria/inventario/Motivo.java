/*
 * nombre: Motivo.java
 * descripcion: Motivos de movimiento, cada uno válido para un único tipo.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.inventario;

/** Motivo de un movimiento. Cada motivo pertenece a un tipo y no puede usarse con otro. */
public enum Motivo {
    COMPRA(TipoMovimiento.ENTRADA),
    DEVOLUCION_CLIENTE(TipoMovimiento.ENTRADA),
    STOCK_INICIAL(TipoMovimiento.ENTRADA),
    OTRA_ENTRADA(TipoMovimiento.ENTRADA),
    VENTA(TipoMovimiento.SALIDA),
    CONSUMO_INTERNO(TipoMovimiento.SALIDA),
    MERMA(TipoMovimiento.SALIDA),
    DEVOLUCION_PROVEEDOR(TipoMovimiento.SALIDA),
    OTRA_SALIDA(TipoMovimiento.SALIDA),
    CONTEO_FISICO(TipoMovimiento.AJUSTE),
    DANO(TipoMovimiento.AJUSTE),
    CORRECCION(TipoMovimiento.AJUSTE);

    private final TipoMovimiento tipo;

    Motivo(TipoMovimiento tipo) {
        this.tipo = tipo;
    }

    /** @return tipo de movimiento al que pertenece el motivo */
    public TipoMovimiento tipo() {
        return tipo;
    }
}
