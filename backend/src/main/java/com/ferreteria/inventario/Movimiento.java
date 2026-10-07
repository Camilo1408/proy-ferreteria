/*
 * nombre: Movimiento.java
 * descripcion: Entidad inmutable de movimiento de inventario (kárdex).
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.inventario;

import com.ferreteria.producto.Producto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Registro de un cambio de stock. No tiene setters: un movimiento nunca se modifica ni se borra. */
@Entity
@Table(name = "movimiento")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Motivo motivo;

    /** Magnitud del movimiento (siempre positiva). */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;

    /** Cambio de stock con signo: positivo suma, negativo resta. */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal variacion;

    @Column(name = "stock_anterior", nullable = false, precision = 14, scale = 3)
    private BigDecimal stockAnterior;

    @Column(name = "stock_resultante", nullable = false, precision = 14, scale = 3)
    private BigDecimal stockResultante;

    /** Documento asociado (factura, orden, etc.). */
    @Column(length = 40)
    private String referencia;

    @Column(length = 300)
    private String nota;

    @Column(nullable = false, length = 50)
    private String usuario;

    @Column(nullable = false)
    private Instant fecha;
}
