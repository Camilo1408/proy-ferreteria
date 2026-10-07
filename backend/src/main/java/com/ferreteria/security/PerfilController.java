/*
 * nombre: PerfilController.java
 * descripcion: API de perfiles y catálogo de permisos.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

import com.ferreteria.security.PerfilDtos.PerfilRequest;
import com.ferreteria.security.PerfilDtos.PerfilResponse;
import com.ferreteria.security.PerfilDtos.PermisoInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints bajo {@code /api/v1/perfiles}. */
@RestController
@RequestMapping("/api/v1/perfiles")
@RequiredArgsConstructor
@Tag(name = "Perfiles")
@SecurityRequirement(name = "bearer")
public class PerfilController {

    private static final String LECTURA = "hasAnyAuthority('PERM_PERFILES_GESTIONAR','PERM_USUARIOS_GESTIONAR')";
    private static final String ESCRITURA = "hasAuthority('PERM_PERFILES_GESTIONAR')";

    private final PerfilService service;

    /** Lista de perfiles (también la usa el formulario de usuarios para elegir perfil). */
    @GetMapping
    @PreAuthorize(LECTURA)
    @Operation(summary = "Listar perfiles")
    public List<PerfilResponse> listar() {
        return service.listar();
    }

    /** Catálogo de permisos asignables. */
    @GetMapping("/permisos")
    @PreAuthorize(LECTURA)
    @Operation(summary = "Catálogo de permisos")
    public List<PermisoInfo> permisos() {
        return service.catalogo();
    }

    /** Crea un perfil. */
    @PostMapping
    @PreAuthorize(ESCRITURA)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear perfil")
    public PerfilResponse crear(@Valid @RequestBody PerfilRequest body) {
        return service.crear(body);
    }

    /** Edita un perfil. */
    @PutMapping("/{id}")
    @PreAuthorize(ESCRITURA)
    @Operation(summary = "Editar perfil")
    public PerfilResponse actualizar(@PathVariable Long id, @Valid @RequestBody PerfilRequest body) {
        return service.actualizar(id, body);
    }

    /** Elimina un perfil sin usuarios. */
    @DeleteMapping("/{id}")
    @PreAuthorize(ESCRITURA)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar perfil")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
