/*
 * nombre: AuthController.java
 * descripcion: Endpoint de inicio de sesión que devuelve un token JWT y los permisos del usuario.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

import com.ferreteria.security.UsuarioDtos.SesionUsuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;
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

    /** Token emitido y datos de la sesión. */
    public record LoginResponse(String token, String tipo, long expiraEnSegundos, SesionUsuario usuario) {
    }

    /** Valida las credenciales (usuario activo) y emite un JWT. */
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión")
    public LoginResponse login(@Valid @RequestBody LoginRequest r) {
        Usuario u = usuarios.findByUsername(r.username().trim().toLowerCase(Locale.ROOT))
                .filter(Usuario::isActivo)
                .filter(x -> encoder.matches(r.password(), x.getPassword()))
                .orElseThrow(CredencialesInvalidasException::new);
        return new LoginResponse(jwt.generar(u), "Bearer", jwt.segundosVigencia(), SesionUsuario.de(u));
    }
}
