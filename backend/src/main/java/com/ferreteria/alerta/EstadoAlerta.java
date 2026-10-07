/*
 * nombre: EstadoAlerta.java
 * descripcion: Ciclo de vida de una alerta.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.alerta;

/** ABIERTA: pendiente; RECONOCIDA: vista por un usuario, el stock sigue bajo; RESUELTA: el stock se repuso. */
public enum EstadoAlerta {
    ABIERTA, RECONOCIDA, RESUELTA
}
