/*
 * nombre: AlertaService.java
 * descripcion: Generación, resolución y reconocimiento de alertas de stock mínimo.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.alerta;

import com.ferreteria.alerta.AlertaDtos.AlertaResponse;
import com.ferreteria.alerta.AlertaDtos.ResumenAlertas;
import com.ferreteria.error.NegocioException;
import com.ferreteria.producto.Estado;
import com.ferreteria.producto.NivelStock;
import com.ferreteria.producto.PaginaResponse;
import com.ferreteria.producto.Producto;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mantiene las alertas coherentes con el stock. {@link #evaluar(Producto)} se invoca después de cada cambio
 * de stock, de mínimo o de estado del producto.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AlertaService {

    /** Estados en los que una alerta sigue vigente. */
    public static final Set<EstadoAlerta> ACTIVAS = Set.of(EstadoAlerta.ABIERTA, EstadoAlerta.RECONOCIDA);

    private final AlertaRepository alertas;

    /** Crea, actualiza o resuelve la alerta del producto según su nivel actual. */
    public void evaluar(Producto p) {
        NivelStock nivel = p.getEstado() == Estado.ACTIVO ? p.nivel() : NivelStock.OK;
        List<Alerta> activas = alertas.findByProductoIdAndEstadoIn(p.getId(), ACTIVAS);
        if (nivel == NivelStock.OK) {
            Instant ahora = Instant.now();
            activas.forEach(a -> {
                a.setEstado(EstadoAlerta.RESUELTA);
                a.setResueltaEn(ahora);
            });
            return;
        }
        NivelAlerta nuevo = nivel == NivelStock.AGOTADO ? NivelAlerta.AGOTADO : NivelAlerta.BAJO;
        if (activas.isEmpty()) {
            alertas.save(Alerta.builder().producto(p).nivel(nuevo).estado(EstadoAlerta.ABIERTA)
                    .stockAlDetectar(p.getStockActual()).stockMinimo(p.getStockMinimo()).creadaEn(Instant.now()).build());
            return;
        }
        Alerta a = activas.get(0);
        a.setStockMinimo(p.getStockMinimo());
        if (a.getNivel() != nuevo) {
            a.setNivel(nuevo);
            if (nuevo == NivelAlerta.AGOTADO) {
                // Empeoró: vuelve a requerir atención.
                a.setEstado(EstadoAlerta.ABIERTA);
                a.setReconocidaPor(null);
                a.setReconocidaEn(null);
            }
        }
    }

    /** Lista alertas; sin filtro devuelve las vigentes (abiertas y reconocidas), las más recientes primero. */
    @Transactional(readOnly = true)
    public PaginaResponse<AlertaResponse> listar(EstadoAlerta estado, Pageable pag) {
        Set<EstadoAlerta> estados = estado == null ? ACTIVAS : Set.of(estado);
        return PaginaResponse.de(alertas.findByEstadoIn(estados, pag).map(AlertaResponse::de));
    }

    /** Contadores: pendientes (abiertas) y vigentes por nivel. */
    @Transactional(readOnly = true)
    public ResumenAlertas resumen() {
        return new ResumenAlertas(alertas.countByEstado(EstadoAlerta.ABIERTA),
                alertas.countByEstadoInAndNivel(ACTIVAS, NivelAlerta.BAJO),
                alertas.countByEstadoInAndNivel(ACTIVAS, NivelAlerta.AGOTADO));
    }

    /** Marca una alerta abierta como reconocida por el usuario. Es idempotente si ya estaba reconocida. */
    public AlertaResponse reconocer(Long id, String usuario) {
        Alerta a = alertas.findById(id)
                .orElseThrow(() -> NegocioException.noEncontrado("ALERTA_NO_ENCONTRADA", "error.alertaNoEncontrada", id));
        if (a.getEstado() == EstadoAlerta.RESUELTA) {
            throw NegocioException.conflicto("ALERTA_NO_RECONOCIBLE", "error.alertaNoReconocible");
        }
        if (a.getEstado() == EstadoAlerta.ABIERTA) {
            a.setEstado(EstadoAlerta.RECONOCIDA);
            a.setReconocidaPor(usuario);
            a.setReconocidaEn(Instant.now());
        }
        return AlertaResponse.de(a);
    }
}
