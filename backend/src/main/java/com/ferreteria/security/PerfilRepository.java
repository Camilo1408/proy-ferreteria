/*
 * nombre: PerfilRepository.java
 * descripcion: Repositorio de perfiles.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de {@link Perfil}. */
public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    Optional<Perfil> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
