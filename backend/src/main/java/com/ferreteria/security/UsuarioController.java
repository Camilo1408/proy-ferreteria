/*
 * nombre: UsuarioController.java
 * descripcion: API de administración de usuarios (permiso USUARIOS_GESTIONAR).
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

import com.ferreteria.producto.PaginaResponse;
import com.ferreteria.security.UsuarioDtos.ClaveRequest;
import com.ferreteria.security.UsuarioDtos.UsuarioActualizacion;
import com.ferreteria.security.UsuarioDtos.UsuarioRequest;
import com.ferreteria.security.UsuarioDtos.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints bajo {@code /api/v1/usuarios}. */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERM_USUARIOS_GESTIONAR')")
@Tag(name = "Usuarios")
@SecurityRequirement(name = "bearer")
public class UsuarioController {

    private final UsuarioService service;

    /** Lista paginada con búsqueda. */
    @GetMapping
    @Operation(summary = "Listar usuarios")
    public PaginaResponse<UsuarioResponse> listar(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(required = false) String q) {
        return service.listar(q, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id")));
    }

    /** Crea un usuario. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear usuario")
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest body) {
        return service.crear(body);
    }

    /** Edita un usuario, incluido su perfil y su estado. */
    @PutMapping("/{id}")
    @Operation(summary = "Editar usuario")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioActualizacion body,
                                      Authentication auth) {
        return service.actualizar(id, body, auth.getName());
    }

    /** Restablece la contraseña de un usuario. */
    @PutMapping("/{id}/clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Restablecer contraseña")
    public void restablecerClave(@PathVariable Long id, @Valid @RequestBody ClaveRequest body) {
        service.restablecerClave(id, body.nueva());
    }
}
