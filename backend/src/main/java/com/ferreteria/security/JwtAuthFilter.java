/*
 * nombre: JwtAuthFilter.java
 * descripcion: Filtro que autentica la petición a partir del encabezado Authorization Bearer.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Si el token es válido, deja el usuario autenticado; si no, la petición sigue anónima (401 después). */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String cab = req.getHeader("Authorization");
        if (cab != null && cab.startsWith("Bearer ")) {
            jwt.validar(cab.substring(7)).ifPresent(c -> {
                var auth = new UsernamePasswordAuthenticationToken(c.getSubject(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + c.get("rol", String.class))));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(req, res);
    }
}
