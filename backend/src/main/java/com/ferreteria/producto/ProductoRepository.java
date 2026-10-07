/*
 * nombre: ProductoRepository.java
 * descripcion: Repositorio JPA de productos.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.producto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Acceso a datos de {@link Producto}. */
public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    /** Indica si existe un producto con ese nombre, sin distinguir mayúsculas. */
    boolean existsByNombreIgnoreCase(String nombre);

    /** Indica si otro producto (distinto del id dado) usa ese nombre. */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
