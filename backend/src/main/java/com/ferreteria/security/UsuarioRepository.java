/*
 * nombre: UsuarioRepository.java
 * descripcion: Repositorio de usuarios.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de {@link Usuario}. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Busca un usuario por su nombre de usuario. */
    Optional<Usuario> findByUsername(String username);
}
