/*
 * nombre: ReporteService.java
 * descripcion: Resumen del panel y generación del reporte CSV de inventario.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.reporte;

import com.ferreteria.alerta.AlertaService;
import com.ferreteria.alerta.AlertaDtos.ResumenAlertas;
import com.ferreteria.inventario.MovimientoRepository;
import com.ferreteria.producto.Estado;
import com.ferreteria.producto.ProductoRepository;
import com.ferreteria.producto.ProductoResponse;
import com.ferreteria.producto.ProductoService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cálculos de solo lectura para el panel y el reporte. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {

    private final ProductoRepository productos;
    private final MovimientoRepository movimientos;
    private final ProductoService productoService;
    private final AlertaService alertas;
    private final MessageSource messages;

    @Value("${app.zona:America/Bogota}")
    private String zona;

    /** Números del panel. */
    public record Dashboard(long productosActivos, long bajoMinimo, long agotados, long alertasAbiertas,
                            long movimientosHoy) {
    }

    /** Calcula el panel; «hoy» se mide en la zona horaria configurada. */
    public Dashboard dashboard() {
        var inicioDia = LocalDate.now(ZoneId.of(zona)).atStartOfDay(ZoneId.of(zona)).toInstant();
        ResumenAlertas r = alertas.resumen();
        return new Dashboard(productos.countByEstado(Estado.ACTIVO), productos.contarBajoMinimo(),
                productos.contarAgotados(), r.abiertas(), movimientos.countByFechaGreaterThanEqual(inicioDia));
    }

    /** Genera el CSV (con BOM para Excel) con el encabezado en el idioma pedido. */
    public String inventarioCsv(boolean bajoMinimo, Locale locale) {
        List<ProductoResponse> filas = productoService.listarTodos(null, null, bajoMinimo);
        StringBuilder sb = new StringBuilder("﻿");
        sb.append(String.join(",", titulo("codigo", locale), titulo("nombre", locale), titulo("categoria", locale),
                titulo("unidad", locale), titulo("stockActual", locale), titulo("stockMinimo", locale),
                titulo("nivel", locale), titulo("estado", locale))).append("\r\n");
        for (ProductoResponse p : filas) {
            sb.append(String.join(",", celda(p.codigo()), celda(p.nombre()), celda(p.categoria()),
                    celda(p.unidad().name()), celda(plano(p.stockActual())), celda(plano(p.stockMinimo())),
                    celda(p.nivel().name()), celda(p.estado().name()))).append("\r\n");
        }
        return sb.toString();
    }

    private String titulo(String clave, Locale locale) {
        return celda(messages.getMessage("csv." + clave, null, clave, locale));
    }

    private static String plano(java.math.BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    /**
     * Escapa una celda CSV y neutraliza la inyección de fórmulas: si empieza por {@code = + - @} (o tabulación
     * o retorno) se antepone una comilla simple para que Excel la trate como texto.
     */
    static String celda(String valor) {
        String v = valor == null ? "" : valor;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
