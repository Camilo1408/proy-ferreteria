/*
 * nombre: UsuarioDtos.java
 * descripcion: Objetos de transferencia de usuarios, cuenta propia y sesión.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Contenedor de los DTO relacionados con usuarios. */
public final class UsuarioDtos {

    /** 8 a 72 caracteres, sin espacios, con al menos una letra y un número. */
    public static final String CLAVE_PATRON = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,72}$";

    private UsuarioDtos() {
    }

    /** Referencia corta a un perfil. */
    public record PerfilResumen(Long id, String nombre) {
    }

    /** Usuario tal como se lista. Nunca incluye la contraseña. */
    public record UsuarioResponse(Long id, String username, String nombreCompleto, String email, boolean activo,
                                  PerfilResumen perfil, Instant creadoEn) {
        /** Convierte la entidad. */
        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getUsername(), u.getNombreCompleto(), u.getEmail(), u.isActivo(),
                    new PerfilResumen(u.getPerfil().getId(), u.getPerfil().getNombre()), u.getCreadoEn());
        }
    }

    /** Alta de usuario. */
    public record UsuarioRequest(
            @NotBlank(message = "{usuario.username.requerido}")
            @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$", message = "{usuario.username.formato}") String username,
            @NotBlank(message = "{clave.requerida}")
            @Pattern(regexp = CLAVE_PATRON, message = "{clave.formato}") String password,
            @NotBlank(message = "{usuario.nombre.requerido}")
            @Size(min = 2, max = 100, message = "{usuario.nombre.tamano}") String nombreCompleto,
            @Email(message = "{usuario.email.formato}")
            @Size(max = 120, message = "{usuario.email.tamano}") String email,
            @NotNull(message = "{usuario.perfil.requerido}") Long perfilId) {
    }

    /** Edición de usuario por un administrador. */
    public record UsuarioActualizacion(
            @NotBlank(message = "{usuario.nombre.requerido}")
            @Size(min = 2, max = 100, message = "{usuario.nombre.tamano}") String nombreCompleto,
            @Email(message = "{usuario.email.formato}")
            @Size(max = 120, message = "{usuario.email.tamano}") String email,
            @NotNull(message = "{usuario.perfil.requerido}") Long perfilId,
            @NotNull(message = "{usuario.activo.requerido}") Boolean activo) {
    }

    /** Restablecimiento de contraseña por un administrador. */
    public record ClaveRequest(
            @NotBlank(message = "{clave.requerida}")
            @Pattern(regexp = CLAVE_PATRON, message = "{clave.formato}") String nueva) {
    }

    /** Edición de la cuenta propia. */
    public record CuentaRequest(
            @NotBlank(message = "{usuario.nombre.requerido}")
            @Size(min = 2, max = 100, message = "{usuario.nombre.tamano}") String nombreCompleto,
            @Email(message = "{usuario.email.formato}")
            @Size(max = 120, message = "{usuario.email.tamano}") String email) {
    }

    /** Cambio de contraseña propio. */
    public record CambioClaveRequest(
            @NotBlank(message = "{clave.actual.requerida}") String actual,
            @NotBlank(message = "{clave.requerida}")
            @Pattern(regexp = CLAVE_PATRON, message = "{clave.formato}") String nueva) {
    }

    /** Datos de la sesión: quién es y qué puede hacer. */
    public record SesionUsuario(String username, String nombreCompleto, String email, String perfil,
                                List<String> permisos) {
        /** Convierte la entidad; los permisos salen ordenados. */
        public static SesionUsuario de(Usuario u) {
            return new SesionUsuario(u.getUsername(), u.getNombreCompleto(), u.getEmail(), u.getPerfil().getNombre(),
                    u.getPerfil().getPermisos().stream().map(Enum::name).sorted().toList());
        }
    }
}
