/*
 * nombre: MovimientoRepository.java
 * descripcion: Repositorio de movimientos.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.inventario;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Acceso a datos de {@link Movimiento}. */
public interface MovimientoRepository extends JpaRepository<Movimiento, Long>, JpaSpecificationExecutor<Movimiento> {

    /** Cantidad de movimientos desde un instante (para el panel). */
    long countByFechaGreaterThanEqual(Instant desde);
}
