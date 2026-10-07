/*
 * nombre: AdminGuard.java
 * descripcion: Garantiza que siempre exista al menos un usuario activo que pueda administrar usuarios y perfiles.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

import com.ferreteria.error.NegocioException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Se invoca dentro de la transacción después de un cambio; si falla, la transacción se revierte. */
@Component
@RequiredArgsConstructor
public class AdminGuard {

    private final UsuarioRepository usuarios;
    private final EntityManager em;

    /** Verifica que sigan existiendo administradores de usuarios y de perfiles activos. */
    public void verificar() {
        em.flush();
        if (usuarios.contarActivosConPermiso(Permiso.USUARIOS_GESTIONAR) < 1
                || usuarios.contarActivosConPermiso(Permiso.PERFILES_GESTIONAR) < 1) {
            throw NegocioException.conflicto("ULTIMO_ADMIN", "error.ultimoAdmin");
        }
    }
}
