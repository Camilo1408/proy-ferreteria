/*
 * nombre: AlertaDtos.java
 * descripcion: DTO de alertas.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.alerta;

import com.ferreteria.producto.Unidad;
import java.math.BigDecimal;
import java.time.Instant;

/** Contenedor de los DTO de alertas. */
public final class AlertaDtos {

    private AlertaDtos() {
    }

    /** Alerta con los datos actuales del producto y cuánto falta para llegar al mínimo. */
    public record AlertaResponse(Long id, Long productoId, String codigo, String nombre, Unidad unidad,
                                 NivelAlerta nivel, EstadoAlerta estado, BigDecimal stockActual,
                                 BigDecimal stockMinimo, BigDecimal faltante, Instant creadaEn,
                                 String reconocidaPor, Instant reconocidaEn, Instant resueltaEn) {
        /** Convierte la entidad (requiere sesión de persistencia abierta). */
        public static AlertaResponse de(Alerta a) {
            var p = a.getProducto();
            BigDecimal faltante = p.getStockMinimo().subtract(p.getStockActual()).max(BigDecimal.ZERO);
            return new AlertaResponse(a.getId(), p.getId(), p.getCodigo(), p.getNombre(), p.getUnidad(), a.getNivel(),
                    a.getEstado(), p.getStockActual(), p.getStockMinimo(), faltante, a.getCreadaEn(),
                    a.getReconocidaPor(), a.getReconocidaEn(), a.getResueltaEn());
        }
    }

    /** Contadores para la campana de alertas y el panel. */
    public record ResumenAlertas(long abiertas, long bajos, long agotados) {
    }
}
