/*
 * nombre: AuthController.java
 * descripcion: Endpoint de inicio de sesión que devuelve un token JWT.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Autenticación: {@code POST /api/v1/auth/login}. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación")
public class AuthController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    /** Credenciales de entrada. */
    public record LoginRequest(
            @NotBlank(message = "{usuario.requerido}") String username,
            @NotBlank(message = "{clave.requerida}") String password) {
    }

    /** Token emitido. */
    public record LoginResponse(String token, String tipo, long expiraEnSegundos, String username, String rol) {
    }

    /** Valida las credenciales y emite un JWT. */
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión")
    public LoginResponse login(@Valid @RequestBody LoginRequest r) {
        Usuario u = usuarios.findByUsername(r.username())
                .filter(x -> encoder.matches(r.password(), x.getPassword()))
                .orElseThrow(CredencialesInvalidasException::new);
        return new LoginResponse(jwt.generar(u), "Bearer", jwt.segundosVigencia(), u.getUsername(),
                u.getRol().name());
    }
}
