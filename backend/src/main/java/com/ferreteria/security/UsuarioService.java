/*
 * nombre: UsuarioService.java
 * descripcion: Reglas de negocio de usuarios y de la cuenta propia.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

import com.ferreteria.error.NegocioException;
import com.ferreteria.producto.PaginaResponse;
import com.ferreteria.security.UsuarioDtos.CambioClaveRequest;
import com.ferreteria.security.UsuarioDtos.CuentaRequest;
import com.ferreteria.security.UsuarioDtos.SesionUsuario;
import com.ferreteria.security.UsuarioDtos.UsuarioActualizacion;
import com.ferreteria.security.UsuarioDtos.UsuarioRequest;
import com.ferreteria.security.UsuarioDtos.UsuarioResponse;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Servicio de usuarios: administración y autogestión de la cuenta. */
@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PerfilRepository perfiles;
    private final PasswordEncoder encoder;
    private final AdminGuard guard;

    /** Lista usuarios, con búsqueda opcional por usuario o nombre. */
    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> listar(String q, Pageable pag) {
        Specification<Usuario> spec = (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return cb.conjunction();
            }
            String patron = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            return cb.or(cb.like(cb.lower(root.get("username")), patron),
                    cb.like(cb.lower(root.get("nombreCompleto")), patron));
        };
        return PaginaResponse.de(usuarios.findAll(spec, pag).map(UsuarioResponse::de));
    }

    /** Crea un usuario con el perfil indicado. */
    public UsuarioResponse crear(UsuarioRequest r) {
        String username = r.username().trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByUsernameIgnoreCase(username)) {
            throw NegocioException.conflicto("USUARIO_DUPLICADO", "error.usuarioDuplicado", username);
        }
        Usuario u = Usuario.builder()
                .username(username)
                .password(encoder.encode(r.password()))
                .nombreCompleto(r.nombreCompleto().trim())
                .email(limpiar(r.email()))
                .perfil(perfil(r.perfilId()))
                .build();
        return UsuarioResponse.de(usuarios.save(u));
    }

    /** Edita un usuario. Nadie puede desactivarse ni cambiarse el perfil a sí mismo. */
    public UsuarioResponse actualizar(Long id, UsuarioActualizacion r, String actor) {
        Usuario u = usuarios.findById(id)
                .orElseThrow(() -> NegocioException.noEncontrado("USUARIO_NO_ENCONTRADO", "error.usuarioNoEncontrado", id));
        boolean esUnoMismo = u.getUsername().equals(actor);
        if (esUnoMismo && (!r.activo() || !u.getPerfil().getId().equals(r.perfilId()))) {
            throw NegocioException.conflicto("AUTO_MODIFICACION", "error.autoModificacion");
        }
        u.setNombreCompleto(r.nombreCompleto().trim());
        u.setEmail(limpiar(r.email()));
        u.setActivo(r.activo());
        u.setPerfil(perfil(r.perfilId()));
        UsuarioResponse resp = UsuarioResponse.de(usuarios.save(u));
        guard.verificar();
        return resp;
    }

    /** Restablece la contraseña de otro usuario (acción de administrador). */
    public void restablecerClave(Long id, String nueva) {
        Usuario u = usuarios.findById(id)
                .orElseThrow(() -> NegocioException.noEncontrado("USUARIO_NO_ENCONTRADO", "error.usuarioNoEncontrado", id));
        u.setPassword(encoder.encode(nueva));
        usuarios.save(u);
    }

    /** Datos de la sesión del usuario autenticado. */
    @Transactional(readOnly = true)
    public SesionUsuario sesion(String username) {
        return SesionUsuario.de(propio(username));
    }

    /** Edita nombre y correo del usuario autenticado. */
    public SesionUsuario actualizarCuenta(String username, CuentaRequest r) {
        Usuario u = propio(username);
        u.setNombreCompleto(r.nombreCompleto().trim());
        u.setEmail(limpiar(r.email()));
        return SesionUsuario.de(usuarios.save(u));
    }

    /** Cambia la contraseña propia exigiendo la actual; la nueva debe ser distinta. */
    public void cambiarClave(String username, CambioClaveRequest r) {
        Usuario u = propio(username);
        if (!encoder.matches(r.actual(), u.getPassword())) {
            throw NegocioException.invalido("CLAVE_ACTUAL_INCORRECTA", "error.claveActual");
        }
        if (r.actual().equals(r.nueva())) {
            throw NegocioException.invalido("CLAVE_IGUAL", "error.claveIgual");
        }
        u.setPassword(encoder.encode(r.nueva()));
        usuarios.save(u);
    }

    private Usuario propio(String username) {
        return usuarios.findByUsername(username)
                .orElseThrow(() -> NegocioException.noEncontrado("USUARIO_NO_ENCONTRADO", "error.usuarioNoEncontrado", username));
    }

    private Perfil perfil(Long id) {
        return perfiles.findById(id)
                .orElseThrow(() -> NegocioException.noEncontrado("PERFIL_NO_ENCONTRADO", "error.perfilNoEncontrado", id));
    }

    private static String limpiar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
