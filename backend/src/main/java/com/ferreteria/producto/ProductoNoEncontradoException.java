/*
 * nombre: ProductoNoEncontradoException.java
 * descripcion: Excepción de producto inexistente (HTTP 404).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.producto;

/** Se lanza cuando no existe un producto con el id solicitado. */
public class ProductoNoEncontradoException extends RuntimeException {

    /** @param id identificador buscado */
    public ProductoNoEncontradoException(Long id) {
        super(String.valueOf(id));
    }
}
