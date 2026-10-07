/*
 * nombre: NombreDuplicadoException.java
 * descripcion: Excepción de nombre de producto duplicado (HTTP 409).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.producto;

/** Se lanza cuando ya existe un producto con el mismo nombre. */
public class NombreDuplicadoException extends RuntimeException {

    /** @param nombre nombre repetido */
    public NombreDuplicadoException(String nombre) {
        super(nombre);
    }
}
