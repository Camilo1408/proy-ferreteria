/*
 * nombre: JwtService.java
 * descripcion: Generación y validación de tokens JWT (HS256).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Emite y valida tokens JWT. */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration vigencia;

    /**
     * @param secreto    clave de firma (mínimo 32 caracteres)
     * @param minutos    vigencia del token en minutos
     */
    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.minutes:60}") long minutos) {
        this.key = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.vigencia = Duration.ofMinutes(minutos);
    }

    /** Genera un token para el usuario. */
    public String generar(Usuario u) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(u.getUsername())
                .claim("rol", u.getRol().name())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + vigencia.toMillis()))
                .signWith(key)
                .compact();
    }

    /** Segundos de vigencia del token. */
    public long segundosVigencia() {
        return vigencia.toSeconds();
    }

    /** Valida y devuelve los claims, o vacío si el token es inválido o expiró. */
    public Optional<Claims> validar(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
