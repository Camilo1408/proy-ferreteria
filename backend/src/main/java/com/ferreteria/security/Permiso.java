/*
 * nombre: Permiso.java
 * descripcion: Catálogo de permisos modulares asignables a un perfil.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

/** Permisos del sistema, agrupados por módulo. Spring Security los expone como {@code PERM_<NOMBRE>}. */
public enum Permiso {
    PRODUCTOS_VER("productos"),
    PRODUCTOS_GESTIONAR("productos"),
    MOVIMIENTOS_VER("inventario"),
    MOVIMIENTOS_REGISTRAR("inventario"),
    AJUSTES_REGISTRAR("inventario"),
    ALERTAS_VER("alertas"),
    ALERTAS_GESTIONAR("alertas"),
    REPORTES_VER("reportes"),
    USUARIOS_GESTIONAR("administracion"),
    PERFILES_GESTIONAR("administracion");

    private final String modulo;

    Permiso(String modulo) {
        this.modulo = modulo;
    }

    /** @return nombre del módulo al que pertenece el permiso */
    public String modulo() {
        return modulo;
    }

    /** @return autoridad de Spring Security equivalente */
    public String authority() {
        return "PERM_" + name();
    }
}
