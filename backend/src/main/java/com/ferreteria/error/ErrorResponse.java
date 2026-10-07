/*
 * nombre: ErrorResponse.java
 * descripcion: Formato uniforme de error de la API.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.error;

import java.time.Instant;
import java.util.List;

/**
 * Cuerpo JSON de todos los errores.
 *
 * @param codigo   código estable (por ejemplo VALIDACION, NO_ENCONTRADO)
 * @param mensaje  mensaje traducido según Accept-Language
 * @param detalles detalles por campo (puede estar vacío)
 * @param fecha    instante del error
 */
public record ErrorResponse(String codigo, String mensaje, List<String> detalles, Instant fecha) {
}
