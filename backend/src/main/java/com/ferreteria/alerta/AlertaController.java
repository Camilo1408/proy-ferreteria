/*
 * nombre: AlertaController.java
 * descripcion: API de alertas de stock mínimo.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.alerta;

import com.ferreteria.alerta.AlertaDtos.AlertaResponse;
import com.ferreteria.alerta.AlertaDtos.ResumenAlertas;
import com.ferreteria.producto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints bajo {@code /api/v1/alertas}. */
@RestController
@RequestMapping("/api/v1/alertas")
@RequiredArgsConstructor
@Tag(name = "Alertas")
@SecurityRequirement(name = "bearer")
public class AlertaController {

    private final AlertaService service;

    /** Alertas vigentes por defecto; con {@code estado} filtra por ese estado. */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_ALERTAS_VER')")
    @Operation(summary = "Listar alertas")
    public PaginaResponse<AlertaResponse> listar(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) EstadoAlerta estado) {
        return service.listar(estado, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "creadaEn", "id")));
    }

    /** Contadores para la campana. */
    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('PERM_ALERTAS_VER')")
    @Operation(summary = "Resumen de alertas")
    public ResumenAlertas resumen() {
        return service.resumen();
    }

    /** Reconoce una alerta. */
    @PostMapping("/{id}/reconocer")
    @PreAuthorize("hasAuthority('PERM_ALERTAS_GESTIONAR')")
    @Operation(summary = "Reconocer alerta")
    public AlertaResponse reconocer(@PathVariable Long id, Authentication auth) {
        return service.reconocer(id, auth.getName());
    }
}
