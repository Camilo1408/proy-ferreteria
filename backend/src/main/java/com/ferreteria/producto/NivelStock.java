/*
 * nombre: NivelStock.java
 * descripcion: Nivel de existencias de un producto respecto a su mínimo.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.producto;

/** OK: sobre el mínimo (o sin mínimo); BAJO: en o bajo el mínimo; AGOTADO: stock cero con mínimo definido. */
public enum NivelStock {
    OK, BAJO, AGOTADO
}
