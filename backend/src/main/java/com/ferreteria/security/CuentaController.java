/*
 * nombre: CuentaController.java
 * descripcion: Autogestión de la cuenta: datos propios y cambio de contraseña (cualquier sesión iniciada).
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

import com.ferreteria.security.UsuarioDtos.CambioClaveRequest;
import com.ferreteria.security.UsuarioDtos.CuentaRequest;
import com.ferreteria.security.UsuarioDtos.SesionUsuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints bajo {@code /api/v1/cuenta}. */
@RestController
@RequestMapping("/api/v1/cuenta")
@RequiredArgsConstructor
@Tag(name = "Cuenta")
@SecurityRequirement(name = "bearer")
public class CuentaController {

    private final UsuarioService service;

    /** Datos y permisos del usuario autenticado. */
    @GetMapping
    @Operation(summary = "Mi cuenta")
    public SesionUsuario ver(Authentication auth) {
        return service.sesion(auth.getName());
    }

    /** Edita nombre y correo propios. */
    @PutMapping
    @Operation(summary = "Editar mi cuenta")
    public SesionUsuario editar(@Valid @RequestBody CuentaRequest body, Authentication auth) {
        return service.actualizarCuenta(auth.getName(), body);
    }

    /** Cambia la contraseña propia. */
    @PutMapping("/clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cambiar mi contraseña")
    public void cambiarClave(@Valid @RequestBody CambioClaveRequest body, Authentication auth) {
        service.cambiarClave(auth.getName(), body);
    }
}
