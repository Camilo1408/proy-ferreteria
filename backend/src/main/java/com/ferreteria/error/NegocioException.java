/*
 * nombre: NegocioException.java
 * descripcion: Excepción genérica de reglas de negocio con estado HTTP, código estable y mensaje i18n.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Se lanza cuando se viola una regla de negocio; el manejador global la traduce a JSON. */
@Getter
public class NegocioException extends RuntimeException {

    private final transient HttpStatus status;
    private final String codigo;
    private final String clave;
    private final transient Object[] args;

    /**
     * @param status estado HTTP de la respuesta
     * @param codigo código estable de la API (por ejemplo STOCK_INSUFICIENTE)
     * @param clave  clave del mensaje en messages*.properties
     * @param args   argumentos del mensaje
     */
    public NegocioException(HttpStatus status, String codigo, String clave, Object... args) {
        super(codigo);
        this.status = status;
        this.codigo = codigo;
        this.clave = clave;
        this.args = args;
    }

    /** Atajo para 409 Conflict. */
    public static NegocioException conflicto(String codigo, String clave, Object... args) {
        return new NegocioException(HttpStatus.CONFLICT, codigo, clave, args);
    }

    /** Atajo para 404 Not Found. */
    public static NegocioException noEncontrado(String codigo, String clave, Object... args) {
        return new NegocioException(HttpStatus.NOT_FOUND, codigo, clave, args);
    }

    /** Atajo para 400 Bad Request. */
    public static NegocioException invalido(String codigo, String clave, Object... args) {
        return new NegocioException(HttpStatus.BAD_REQUEST, codigo, clave, args);
    }
}
