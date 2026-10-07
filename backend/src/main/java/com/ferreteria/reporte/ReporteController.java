/*
 * nombre: ReporteController.java
 * descripcion: API del panel de resumen y del reporte CSV de inventario.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.reporte;

import com.ferreteria.reporte.ReporteService.Dashboard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints {@code /api/v1/dashboard} y {@code /api/v1/reportes}. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Reportes")
@SecurityRequirement(name = "bearer")
public class ReporteController {

    private final ReporteService service;

    /** Números del panel de inicio. */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('PERM_PRODUCTOS_VER')")
    @Operation(summary = "Resumen del panel")
    public Dashboard dashboard() {
        return service.dashboard();
    }

    /** Descarga del inventario en CSV. */
    @GetMapping("/reportes/inventario.csv")
    @PreAuthorize("hasAuthority('PERM_REPORTES_VER')")
    @Operation(summary = "Reporte de inventario (CSV)")
    public ResponseEntity<byte[]> inventario(@RequestParam(defaultValue = "false") boolean bajoMinimo) {
        Locale idioma = LocaleContextHolder.getLocale();
        byte[] cuerpo = service.inventarioCsv(bajoMinimo, idioma).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("inventario.csv").build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(cuerpo);
    }
}
