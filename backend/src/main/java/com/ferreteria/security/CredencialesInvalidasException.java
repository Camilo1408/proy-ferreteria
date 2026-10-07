/*
 * nombre: CredencialesInvalidasException.java
 * descripcion: Excepción de credenciales incorrectas (HTTP 401).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.security;

/** Usuario o contraseña incorrectos. No revela cuál de los dos falló. */
public class CredencialesInvalidasException extends RuntimeException {
}
