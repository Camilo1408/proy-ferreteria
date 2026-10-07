/*
 * nombre: OpenApiConfig.java
 * descripcion: Metadatos OpenAPI y esquema de seguridad Bearer.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentación OpenAPI disponible en {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    /** Define información general y autenticación Bearer JWT. */
    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("API Ferretería - Productos").version("1.0.0")
                        .description("CRUD de productos con seguridad JWT e internacionalización es/en"))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
