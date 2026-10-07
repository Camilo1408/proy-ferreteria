/*
 * nombre: MovimientoService.java
 * descripcion: Registro de entradas, salidas y ajustes con bloqueo por producto y consulta del historial.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.inventario;

import com.ferreteria.alerta.AlertaService;
import com.ferreteria.common.Actor;
import com.ferreteria.error.NegocioException;
import com.ferreteria.inventario.MovimientoDtos.AjusteRequest;
import com.ferreteria.inventario.MovimientoDtos.MovimientoRequest;
import com.ferreteria.inventario.MovimientoDtos.MovimientoResponse;
import com.ferreteria.producto.Estado;
import com.ferreteria.producto.PaginaResponse;
import com.ferreteria.producto.Producto;
import com.ferreteria.producto.ProductoNoEncontradoException;
import com.ferreteria.producto.ProductoRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cada movimiento bloquea la fila del producto ({@code SELECT ... FOR UPDATE}), de modo que dos movimientos
 * simultáneos sobre el mismo producto se ejecutan en serie y el stock nunca queda negativo.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MovimientoService {

    private static final int ESCALA = 3;

    private final MovimientoRepository movimientos;
    private final ProductoRepository productos;
    private final AlertaService alertas;

    @Value("${app.zona:America/Bogota}")
    private String zona;

    /** Registra una entrada de stock. */
    public MovimientoResponse entrada(MovimientoRequest r) {
        return mover(TipoMovimiento.ENTRADA, r.productoId(), r.cantidad(), r.motivo(), r.referencia(), r.nota());
    }

    /** Registra una salida de stock; falla si no hay existencias suficientes. */
    public MovimientoResponse salida(MovimientoRequest r) {
        return mover(TipoMovimiento.SALIDA, r.productoId(), r.cantidad(), r.motivo(), r.referencia(), r.nota());
    }

    /** Registra un ajuste llevando el stock al valor contado; falla si no hay diferencia. */
    public MovimientoResponse ajuste(AjusteRequest r) {
        validarMotivo(TipoMovimiento.AJUSTE, r.motivo());
        Producto p = bloquear(r.productoId());
        BigDecimal anterior = p.getStockActual();
        BigDecimal contado = r.stockContado().setScale(ESCALA);
        BigDecimal variacion = contado.subtract(anterior);
        if (variacion.signum() == 0) {
            throw NegocioException.conflicto("AJUSTE_SIN_DIFERENCIA", "error.ajusteSinDiferencia");
        }
        return aplicar(p, TipoMovimiento.AJUSTE, r.motivo(), variacion.abs(), variacion, null, r.nota());
    }

    /** Registra el stock inicial de un producto recién creado como una entrada. */
    public void registrarInicial(Producto p, BigDecimal cantidad) {
        BigDecimal c = cantidad.setScale(ESCALA);
        aplicar(p, TipoMovimiento.ENTRADA, Motivo.STOCK_INICIAL, c, c, null, null);
    }

    /** Historial paginado con filtros opcionales por producto, tipo y rango de fechas (zona horaria local). */
    @Transactional(readOnly = true)
    public PaginaResponse<MovimientoResponse> listar(Long productoId, TipoMovimiento tipo, LocalDate desde,
                                                     LocalDate hasta, Pageable pag) {
        ZoneId z = ZoneId.of(zona);
        Specification<Movimiento> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (productoId != null) {
                ps.add(cb.equal(root.get("producto").get("id"), productoId));
            }
            if (tipo != null) {
                ps.add(cb.equal(root.get("tipo"), tipo));
            }
            if (desde != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde.atStartOfDay(z).toInstant()));
            }
            if (hasta != null) {
                ps.add(cb.lessThan(root.get("fecha"), hasta.plusDays(1).atStartOfDay(z).toInstant()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PaginaResponse.de(movimientos.findAll(spec, pag).map(MovimientoResponse::de));
    }

    private MovimientoResponse mover(TipoMovimiento tipo, Long productoId, BigDecimal cantidad, Motivo motivo,
                                     String referencia, String nota) {
        validarMotivo(tipo, motivo);
        Producto p = bloquear(productoId);
        BigDecimal c = cantidad.setScale(ESCALA);
        BigDecimal variacion = tipo == TipoMovimiento.ENTRADA ? c : c.negate();
        if (p.getStockActual().add(variacion).signum() < 0) {
            throw NegocioException.conflicto("STOCK_INSUFICIENTE", "error.stockInsuficiente",
                    p.getStockActual().stripTrailingZeros().toPlainString());
        }
        return aplicar(p, tipo, motivo, c, variacion, limpiar(referencia), limpiar(nota));
    }

    private MovimientoResponse aplicar(Producto p, TipoMovimiento tipo, Motivo motivo, BigDecimal cantidad,
                                       BigDecimal variacion, String referencia, String nota) {
        BigDecimal anterior = p.getStockActual();
        BigDecimal resultante = anterior.add(variacion);
        p.setStockActual(resultante);
        productos.save(p);
        Movimiento m = movimientos.save(Movimiento.builder().producto(p).tipo(tipo).motivo(motivo)
                .cantidad(cantidad).variacion(variacion).stockAnterior(anterior).stockResultante(resultante)
                .referencia(referencia).nota(nota).usuario(Actor.nombre()).fecha(Instant.now()).build());
        alertas.evaluar(p);
        return MovimientoResponse.de(m);
    }

    private Producto bloquear(Long productoId) {
        Producto p = productos.findByIdForUpdate(productoId).orElseThrow(() -> new ProductoNoEncontradoException(productoId));
        if (p.getEstado() != Estado.ACTIVO) {
            throw NegocioException.conflicto("PRODUCTO_INACTIVO", "error.productoInactivo");
        }
        return p;
    }

    private static void validarMotivo(TipoMovimiento tipo, Motivo motivo) {
        if (motivo.tipo() != tipo) {
            throw NegocioException.invalido("MOTIVO_INVALIDO", "error.motivoInvalido", motivo.name(), tipo.name());
        }
    }

    private static String limpiar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
