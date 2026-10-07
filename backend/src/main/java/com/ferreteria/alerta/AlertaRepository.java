/*
 * nombre: AlertaRepository.java
 * descripcion: Repositorio de alertas.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.alerta;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de {@link Alerta}. */
public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    List<Alerta> findByProductoIdAndEstadoIn(Long productoId, Collection<EstadoAlerta> estados);

    Page<Alerta> findByEstadoIn(Collection<EstadoAlerta> estados, Pageable pageable);

    long countByEstado(EstadoAlerta estado);

    long countByEstadoInAndNivel(Collection<EstadoAlerta> estados, NivelAlerta nivel);
}
