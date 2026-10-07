/*
 * nombre: PaginaResponse.java
 * descripcion: Respuesta paginada con estructura JSON estable.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.producto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Página de resultados con estructura JSON estable. */
public record PaginaResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /** Construye la respuesta a partir de una {@link Page}. */
    public static <T> PaginaResponse<T> de(Page<T> p) {
        return new PaginaResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
