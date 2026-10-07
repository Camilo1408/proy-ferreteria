/*
 * nombre: UsuarioRepository.java
 * descripcion: Repositorio de usuarios.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a datos de {@link Usuario}. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByPerfilId(Long perfilId);

    long countByPerfilId(Long perfilId);

    /** Cuenta usuarios activos cuyo perfil incluye el permiso dado. */
    @Query("select count(distinct u.id) from Usuario u join u.perfil p join p.permisos pm "
            + "where u.activo = true and pm = :permiso")
    long contarActivosConPermiso(@Param("permiso") Permiso permiso);
}
