/*
 * nombre: MovimientoDtos.java
 * descripcion: DTO de movimientos (entrada, salida, ajuste y respuesta).
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.inventario;

import com.ferreteria.producto.Unidad;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/** Contenedor de los DTO de movimientos. */
public final class MovimientoDtos {

    private MovimientoDtos() {
    }

    /** Entrada o salida: cantidad positiva con hasta 3 decimales. */
    public record MovimientoRequest(
            @NotNull(message = "{mov.producto.requerido}") Long productoId,
            @NotNull(message = "{mov.cantidad.requerida}")
            @DecimalMin(value = "0", inclusive = false, message = "{mov.cantidad.positiva}")
            @Digits(integer = 11, fraction = 3, message = "{mov.cantidad.formato}") BigDecimal cantidad,
            @NotNull(message = "{mov.motivo.requerido}") Motivo motivo,
            @Size(max = 40, message = "{mov.referencia.tamano}") String referencia,
            @Size(max = 300, message = "{mov.nota.tamano}") String nota) {
    }

    /** Ajuste: se informa el stock contado y una nota obligatoria. */
    public record AjusteRequest(
            @NotNull(message = "{mov.producto.requerido}") Long productoId,
            @NotNull(message = "{mov.contado.requerido}")
            @DecimalMin(value = "0", message = "{mov.contado.negativo}")
            @Digits(integer = 11, fraction = 3, message = "{mov.cantidad.formato}") BigDecimal stockContado,
            @NotNull(message = "{mov.motivo.requerido}") Motivo motivo,
            @NotBlank(message = "{mov.nota.requerida}")
            @Size(min = 5, max = 300, message = "{mov.nota.requerida}") String nota) {
    }

    /** Movimiento tal como se consulta. */
    public record MovimientoResponse(Long id, Long productoId, String codigo, String nombre, Unidad unidad,
                                     TipoMovimiento tipo, Motivo motivo, BigDecimal cantidad, BigDecimal variacion,
                                     BigDecimal stockAnterior, BigDecimal stockResultante, String referencia,
                                     String nota, String usuario, Instant fecha) {
        /** Convierte la entidad (requiere sesión de persistencia abierta). */
        public static MovimientoResponse de(Movimiento m) {
            var p = m.getProducto();
            return new MovimientoResponse(m.getId(), p.getId(), p.getCodigo(), p.getNombre(), p.getUnidad(),
                    m.getTipo(), m.getMotivo(), m.getCantidad(), m.getVariacion(), m.getStockAnterior(),
                    m.getStockResultante(), m.getReferencia(), m.getNota(), m.getUsuario(), m.getFecha());
        }
    }
}
