/*
 * nombre: SecurityConfig.java
 * descripcion: Configuración de Spring Security (JWT sin estado, permisos modulares, CORS).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.security;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Toda la API exige sesión; el permiso de cada operación se declara con {@code @PreAuthorize} en los controladores. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtFilter;
    private final ErrorWriter errores;

    @Value("${app.cors.origins:http://localhost:5173}")
    private String origenes;

    /** Cadena de filtros de seguridad. */
    @Bean
    SecurityFilterChain cadena(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasAuthority(Permiso.USUARIOS_GESTIONAR.authority())
                        // Defensa en profundidad: el permiso se exige por ruta (antes de validar el cuerpo) y de nuevo
                        // con @PreAuthorize en cada controlador. Un usuario sin permiso recibe 403, no un 400 de validación.
                        .requestMatchers(HttpMethod.GET, "/api/v1/productos/**", "/api/v1/dashboard")
                                .hasAuthority(Permiso.PRODUCTOS_VER.authority())
                        .requestMatchers("/api/v1/productos/**").hasAuthority(Permiso.PRODUCTOS_GESTIONAR.authority())
                        .requestMatchers(HttpMethod.GET, "/api/v1/movimientos/**").hasAuthority(Permiso.MOVIMIENTOS_VER.authority())
                        .requestMatchers("/api/v1/movimientos/ajustes").hasAuthority(Permiso.AJUSTES_REGISTRAR.authority())
                        .requestMatchers("/api/v1/movimientos/**").hasAuthority(Permiso.MOVIMIENTOS_REGISTRAR.authority())
                        .requestMatchers(HttpMethod.GET, "/api/v1/alertas/**").hasAuthority(Permiso.ALERTAS_VER.authority())
                        .requestMatchers("/api/v1/alertas/**").hasAuthority(Permiso.ALERTAS_GESTIONAR.authority())
                        .requestMatchers("/api/v1/reportes/**").hasAuthority(Permiso.REPORTES_VER.authority())
                        .requestMatchers("/api/v1/usuarios/**").hasAuthority(Permiso.USUARIOS_GESTIONAR.authority())
                        .requestMatchers(HttpMethod.GET, "/api/v1/perfiles/**")
                                .hasAnyAuthority(Permiso.PERFILES_GESTIONAR.authority(), Permiso.USUARIOS_GESTIONAR.authority())
                        .requestMatchers("/api/v1/perfiles/**").hasAuthority(Permiso.PERFILES_GESTIONAR.authority())
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((rq, rs, ex) ->
                                errores.escribir(rq, rs, 401, "NO_AUTENTICADO", "error.noAutenticado"))
                        .accessDeniedHandler((rq, rs, ex) ->
                                errores.escribir(rq, rs, 403, "PROHIBIDO", "error.prohibido")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Codificador BCrypt para contraseñas. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** CORS: orígenes permitidos desde la propiedad {@code app.cors.origins}. */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(Arrays.stream(origenes.split(",")).map(String::trim).toList());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept-Language"));
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
