/*
 * nombre: Actor.java
 * descripcion: Obtiene el nombre del usuario autenticado que ejecuta la operación.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.common;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Utilidad para conocer quién realiza una operación (para trazabilidad). */
public final class Actor {

    /** Nombre usado cuando la operación no proviene de un usuario autenticado. */
    public static final String SISTEMA = "sistema";

    private Actor() {
    }

    /** @return el nombre de usuario autenticado, o {@link #SISTEMA} si no hay sesión */
    public static String nombre() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) {
            return SISTEMA;
        }
        return a.getName();
    }
}
