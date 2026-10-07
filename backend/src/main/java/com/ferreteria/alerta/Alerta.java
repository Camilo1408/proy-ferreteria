/*
 * nombre: Alerta.java
 * descripcion: Entidad de alerta de stock mínimo.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.alerta;

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
import lombok.Setter;

/** Alerta de un producto en o bajo su mínimo. Hay como máximo una alerta activa por producto. */
@Entity
@Table(name = "alerta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NivelAlerta nivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoAlerta estado;

    /** Stock del producto cuando se detectó. */
    @Column(name = "stock_al_detectar", nullable = false, precision = 14, scale = 3)
    private BigDecimal stockAlDetectar;

    @Column(name = "stock_minimo", nullable = false, precision = 14, scale = 3)
    private BigDecimal stockMinimo;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "reconocida_por", length = 50)
    private String reconocidaPor;

    @Column(name = "reconocida_en")
    private Instant reconocidaEn;

    @Column(name = "resuelta_en")
    private Instant resueltaEn;
}
