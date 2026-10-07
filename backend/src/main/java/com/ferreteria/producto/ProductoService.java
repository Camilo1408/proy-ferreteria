/*
 * nombre: ProductoService.java
 * descripcion: Lógica de negocio del CRUD de productos.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.producto;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Servicio con las reglas de negocio de {@link Producto}. */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductoService {

    private final ProductoRepository repo;

    /**
     * Lista productos con filtro opcional por estado y búsqueda por nombre o categoría.
     *
     * @param estado filtro de estado (puede ser null)
     * @param q      texto a buscar (puede ser null)
     * @param pag    paginación
     * @return página de productos
     */
    @Transactional(readOnly = true)
    public PaginaResponse<ProductoResponse> listar(Estado estado, String q, Pageable pag) {
        Specification<Producto> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (estado != null) {
                ps.add(cb.equal(root.get("estado"), estado));
            }
            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("categoria")), patron)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PaginaResponse.de(repo.findAll(spec, pag).map(ProductoResponse::de));
    }

    /** Obtiene un producto por id o lanza {@link ProductoNoEncontradoException}. */
    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.de(buscar(id));
    }

    /** Crea un producto; lanza {@link NombreDuplicadoException} si el nombre ya existe. */
    public ProductoResponse crear(ProductoRequest r) {
        String nombre = r.nombre().trim();
        if (repo.existsByNombreIgnoreCase(nombre)) {
            throw new NombreDuplicadoException(nombre);
        }
        Producto p = Producto.builder()
                .nombre(nombre)
                .categoria(r.categoria().trim())
                .descripcion(r.descripcion())
                .estado(r.estado() == null ? Estado.ACTIVO : r.estado())
                .build();
        return ProductoResponse.de(repo.save(p));
    }

    /** Actualiza un producto existente. */
    public ProductoResponse actualizar(Long id, ProductoRequest r) {
        Producto p = buscar(id);
        String nombre = r.nombre().trim();
        if (repo.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new NombreDuplicadoException(nombre);
        }
        p.setNombre(nombre);
        p.setCategoria(r.categoria().trim());
        p.setDescripcion(r.descripcion());
        if (r.estado() != null) {
            p.setEstado(r.estado());
        }
        return ProductoResponse.de(repo.save(p));
    }

    /** Borrado lógico: deja el producto en estado INACTIVO. */
    public void desactivar(Long id) {
        Producto p = buscar(id);
        p.setEstado(Estado.INACTIVO);
        repo.save(p);
    }

    private Producto buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new ProductoNoEncontradoException(id));
    }
}
