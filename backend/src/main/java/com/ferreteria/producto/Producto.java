/*
 * nombre: Producto.java
 * descripcion: Entidad JPA Producto con código, unidad y existencias.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.producto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Producto del catálogo. {@code stockActual} solo cambia mediante movimientos (entradas, salidas y ajustes);
 * {@link DynamicUpdate} evita que una edición del producto sobrescriba un stock modificado en paralelo.
 */
@Entity
@Table(name = "producto")
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    /** Identificador generado por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código interno único (sin distinguir mayúsculas), 2 a 30 caracteres. */
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    /** Nombre único (sin distinguir mayúsculas), 3 a 100 caracteres. */
    @Column(nullable = false, length = 100)
    private String nombre;

    /** Categoría obligatoria, hasta 60 caracteres. */
    @Column(nullable = false, length = 60)
    private String categoria;

    /** Descripción opcional, hasta 500 caracteres. */
    @Column(length = 500)
    private String descripcion;

    /** Unidad en que se cuenta el stock. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Unidad unidad = Unidad.UND;

    /** Existencias actuales; nunca negativas. */
    @Column(name = "stock_actual", nullable = false, precision = 14, scale = 3)
    @Builder.Default
    private BigDecimal stockActual = BigDecimal.ZERO.setScale(3);

    /** Existencia mínima; con 0 no se generan alertas. */
    @Column(name = "stock_minimo", nullable = false, precision = 14, scale = 3)
    @Builder.Default
    private BigDecimal stockMinimo = BigDecimal.ZERO.setScale(3);

    /** Estado del producto; por defecto ACTIVO. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Estado estado = Estado.ACTIVO;

    /** @return nivel de existencias según la regla de mínimos */
    public NivelStock nivel() {
        if (stockMinimo.signum() > 0 && stockActual.compareTo(stockMinimo) <= 0) {
            return stockActual.signum() == 0 ? NivelStock.AGOTADO : NivelStock.BAJO;
        }
        return NivelStock.OK;
    }
}
