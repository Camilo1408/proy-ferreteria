/*
 * nombre: MovimientoController.java
 * descripcion: API de movimientos de inventario (entradas, salidas, ajustes e historial).
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.inventario;

import com.ferreteria.inventario.MovimientoDtos.AjusteRequest;
import com.ferreteria.inventario.MovimientoDtos.MovimientoRequest;
import com.ferreteria.inventario.MovimientoDtos.MovimientoResponse;
import com.ferreteria.producto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints bajo {@code /api/v1/movimientos}. No existe edición ni borrado de movimientos. */
@RestController
@RequestMapping("/api/v1/movimientos")
@RequiredArgsConstructor
@Tag(name = "Movimientos")
@SecurityRequirement(name = "bearer")
public class MovimientoController {

    private final MovimientoService service;

    /** Historial (kárdex), más recientes primero. */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_MOVIMIENTOS_VER')")
    @Operation(summary = "Historial de movimientos")
    public PaginaResponse<MovimientoResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listar(productoId, tipo, desde, hasta, PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "fecha", "id")));
    }

    /** Registra una entrada. */
    @PostMapping("/entradas")
    @PreAuthorize("hasAuthority('PERM_MOVIMIENTOS_REGISTRAR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar entrada")
    public MovimientoResponse entrada(@Valid @RequestBody MovimientoRequest body) {
        return service.entrada(body);
    }

    /** Registra una salida. */
    @PostMapping("/salidas")
    @PreAuthorize("hasAuthority('PERM_MOVIMIENTOS_REGISTRAR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar salida")
    public MovimientoResponse salida(@Valid @RequestBody MovimientoRequest body) {
        return service.salida(body);
    }

    /** Registra un ajuste por conteo físico. */
    @PostMapping("/ajustes")
    @PreAuthorize("hasAuthority('PERM_AJUSTES_REGISTRAR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar ajuste")
    public MovimientoResponse ajuste(@Valid @RequestBody AjusteRequest body) {
        return service.ajuste(body);
    }
}
