/*
 * nombre: ErrorWriter.java
 * descripcion: Escribe errores 401/403 en JSON traducido desde los filtros de seguridad.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.security;

import com.ferreteria.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

/** Respuesta de error fuera de los controladores (401 y 403). */
@Component
@RequiredArgsConstructor
public class ErrorWriter {

    private final ObjectMapper mapper;
    private final MessageSource messages;
    private final LocaleResolver locales;

    /** Escribe el error con el idioma de la petición. */
    public void escribir(HttpServletRequest req, HttpServletResponse res, int status, String codigo, String clave)
            throws IOException {
        String mensaje = messages.getMessage(clave, null, clave, locales.resolveLocale(req));
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getOutputStream(), new ErrorResponse(codigo, mensaje, List.of(), Instant.now()));
    }
}
