/*
 * nombre: Producto.java
 * descripcion: Entidad JPA Producto.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entidad persistente que representa un producto del catálogo. */
@Entity
@Table(name = "producto")
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

    /** Nombre único (sin distinguir mayúsculas), 3 a 100 caracteres. */
    @Column(nullable = false, length = 100)
    private String nombre;

    /** Categoría obligatoria, hasta 60 caracteres. */
    @Column(nullable = false, length = 60)
    private String categoria;

    /** Descripción opcional, hasta 500 caracteres. */
    @Column(length = 500)
    private String descripcion;

    /** Estado del producto; por defecto ACTIVO. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Estado estado = Estado.ACTIVO;
}
