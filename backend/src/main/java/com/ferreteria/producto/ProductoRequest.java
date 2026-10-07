/*
 * nombre: ProductoRequest.java
 * descripcion: DTO de entrada para crear o actualizar un producto.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.producto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Datos de entrada de un producto.
 *
 * @param codigo       código interno único, 2 a 30 caracteres (letras, números, punto, guion y guion bajo)
 * @param nombre       3 a 100 caracteres, obligatorio
 * @param categoria    obligatoria, hasta 60 caracteres
 * @param descripcion  opcional, hasta 500 caracteres
 * @param unidad       unidad de medida, obligatoria
 * @param stockMinimo  existencia mínima (&gt;= 0); si es nulo se toma 0
 * @param stockInicial solo al crear: cantidad inicial (&gt;= 0), se registra como una entrada
 * @param estado       opcional; por defecto ACTIVO
 */
public record ProductoRequest(
        @NotBlank(message = "{producto.codigo.requerido}")
        @Pattern(regexp = "^[A-Za-z0-9._-]{2,30}$", message = "{producto.codigo.formato}")
        String codigo,
        @NotBlank(message = "{producto.nombre.requerido}")
        @Size(min = 3, max = 100, message = "{producto.nombre.tamano}")
        String nombre,
        @NotBlank(message = "{producto.categoria.requerida}")
        @Size(max = 60, message = "{producto.categoria.tamano}")
        String categoria,
        @Size(max = 500, message = "{producto.descripcion.tamano}")
        String descripcion,
        @NotNull(message = "{producto.unidad.requerida}")
        Unidad unidad,
        @DecimalMin(value = "0", message = "{producto.stock.negativo}")
        @Digits(integer = 11, fraction = 3, message = "{producto.stock.formato}")
        BigDecimal stockMinimo,
        @DecimalMin(value = "0", message = "{producto.stock.negativo}")
        @Digits(integer = 11, fraction = 3, message = "{producto.stock.formato}")
        BigDecimal stockInicial,
        Estado estado) {
}
