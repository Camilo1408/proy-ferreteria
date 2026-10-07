/*
 * nombre: JwtAuthFilter.java
 * descripcion: Autentica la petición con el token Bearer y recarga usuario y permisos desde la base en cada petición.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Si el token es válido y el usuario sigue activo, deja la petición autenticada con los permisos vigentes de su perfil.
 * Así un cambio de perfil o una desactivación rigen de inmediato, sin esperar a que expire el token.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UsuarioRepository usuarios;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String cab = req.getHeader("Authorization");
        if (cab != null && cab.startsWith("Bearer ")) {
            jwt.validar(cab.substring(7)).flatMap(c -> usuarios.findByUsername(c.getSubject()))
                    .filter(Usuario::isActivo).ifPresent(u -> {
                        var autoridades = u.getPerfil().getPermisos().stream()
                                .map(p -> new SimpleGrantedAuthority(p.authority())).toList();
                        SecurityContextHolder.getContext()
                                .setAuthentication(new UsernamePasswordAuthenticationToken(u.getUsername(), null, autoridades));
                    });
        }
        chain.doFilter(req, res);
    }
}
