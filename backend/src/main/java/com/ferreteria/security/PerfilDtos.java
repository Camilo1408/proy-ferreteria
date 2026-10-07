/*
 * nombre: PerfilDtos.java
 * descripcion: Objetos de transferencia de perfiles y catálogo de permisos.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

/** Contenedor de los DTO relacionados con perfiles. */
public final class PerfilDtos {

    private PerfilDtos() {
    }

    /** Alta o edición de un perfil. */
    public record PerfilRequest(
            @NotBlank(message = "{perfil.nombre.requerido}")
            @Size(min = 3, max = 60, message = "{perfil.nombre.tamano}") String nombre,
            @Size(max = 200, message = "{perfil.descripcion.tamano}") String descripcion,
            @NotEmpty(message = "{perfil.permisos.requeridos}") Set<Permiso> permisos) {
    }

    /** Perfil con la cantidad de usuarios que lo usan. */
    public record PerfilResponse(Long id, String nombre, String descripcion, boolean sistema, List<String> permisos,
                                 long usuarios) {
        /** Convierte la entidad; los permisos salen ordenados. */
        public static PerfilResponse de(Perfil p, long usuarios) {
            return new PerfilResponse(p.getId(), p.getNombre(), p.getDescripcion(), p.isSistema(),
                    p.getPermisos().stream().map(Enum::name).sorted().toList(), usuarios);
        }
    }

    /** Permiso del catálogo con su módulo. */
    public record PermisoInfo(String codigo, String modulo) {
    }
}
