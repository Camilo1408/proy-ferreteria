/*
 * nombre: ProductoRepository.java
 * descripcion: Repositorio JPA de productos, con bloqueo pesimista y conteos para el panel.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.producto;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a datos de {@link Producto}. */
public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    /** Indica si existe un producto con ese nombre, sin distinguir mayúsculas. */
    boolean existsByNombreIgnoreCase(String nombre);

    /** Indica si otro producto (distinto del id dado) usa ese nombre. */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    /** Indica si existe un producto con ese código, sin distinguir mayúsculas. */
    boolean existsByCodigoIgnoreCase(String codigo);

    /** Indica si otro producto (distinto del id dado) usa ese código. */
    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    /** Carga el producto bloqueando su fila hasta el fin de la transacción (serializa los movimientos). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);

    long countByEstado(Estado estado);

    /** Productos activos en o bajo su mínimo (con mínimo definido). */
    @Query("select count(p) from Producto p where p.estado = com.ferreteria.producto.Estado.ACTIVO "
            + "and p.stockMinimo > 0 and p.stockActual <= p.stockMinimo")
    long contarBajoMinimo();

    /** Productos activos agotados (stock cero con mínimo definido). */
    @Query("select count(p) from Producto p where p.estado = com.ferreteria.producto.Estado.ACTIVO "
            + "and p.stockMinimo > 0 and p.stockActual = 0")
    long contarAgotados();
}
