/*
 * nombre: ProductoService.java
 * descripcion: Lógica de negocio del CRUD de productos (el stock solo cambia por movimientos).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.producto;

import com.ferreteria.alerta.AlertaService;
import com.ferreteria.error.NegocioException;
import com.ferreteria.inventario.MovimientoService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Servicio con las reglas de negocio de {@link Producto}. */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductoService {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(3);

    private final ProductoRepository repo;
    private final MovimientoService movimientos;
    private final AlertaService alertas;

    /**
     * Lista productos con filtros opcionales.
     *
     * @param estado      filtro de estado (puede ser null)
     * @param q           texto a buscar en código, nombre o categoría (puede ser null)
     * @param bajoMinimo  si es true, solo productos activos en o bajo su mínimo
     * @param pag         paginación
     * @return página de productos
     */
    @Transactional(readOnly = true)
    public PaginaResponse<ProductoResponse> listar(Estado estado, String q, boolean bajoMinimo, Pageable pag) {
        return PaginaResponse.de(repo.findAll(filtro(estado, q, bajoMinimo), pag).map(ProductoResponse::de));
    }

    /** Todos los productos que cumplen el filtro, ordenados por código (para el reporte). */
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarTodos(Estado estado, String q, boolean bajoMinimo) {
        return repo.findAll(filtro(estado, q, bajoMinimo), Sort.by("codigo")).stream().map(ProductoResponse::de).toList();
    }

    /** Obtiene un producto por id o lanza {@link ProductoNoEncontradoException}. */
    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.de(buscar(id));
    }

    /** Crea un producto; con stock inicial positivo registra una entrada STOCK_INICIAL. */
    public ProductoResponse crear(ProductoRequest r) {
        String codigo = r.codigo().trim();
        String nombre = r.nombre().trim();
        if (repo.existsByCodigoIgnoreCase(codigo)) {
            throw NegocioException.conflicto("CODIGO_DUPLICADO", "error.codigoDuplicado", codigo);
        }
        if (repo.existsByNombreIgnoreCase(nombre)) {
            throw new NombreDuplicadoException(nombre);
        }
        Producto p = repo.save(Producto.builder()
                .codigo(codigo)
                .nombre(nombre)
                .categoria(r.categoria().trim())
                .descripcion(r.descripcion())
                .unidad(r.unidad())
                .stockActual(CERO)
                .stockMinimo(escala(r.stockMinimo()))
                .estado(r.estado() == null ? Estado.ACTIVO : r.estado())
                .build());
        if (r.stockInicial() != null && r.stockInicial().signum() > 0) {
            movimientos.registrarInicial(p, r.stockInicial());
        }
        alertas.evaluar(p);
        return ProductoResponse.de(p);
    }

    /** Actualiza los datos de un producto (no su stock). */
    public ProductoResponse actualizar(Long id, ProductoRequest r) {
        Producto p = buscar(id);
        String codigo = r.codigo().trim();
        String nombre = r.nombre().trim();
        if (repo.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw NegocioException.conflicto("CODIGO_DUPLICADO", "error.codigoDuplicado", codigo);
        }
        if (repo.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new NombreDuplicadoException(nombre);
        }
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setCategoria(r.categoria().trim());
        p.setDescripcion(r.descripcion());
        p.setUnidad(r.unidad());
        p.setStockMinimo(escala(r.stockMinimo()));
        if (r.estado() != null) {
            p.setEstado(r.estado());
        }
        repo.save(p);
        alertas.evaluar(p);
        return ProductoResponse.de(p);
    }

    /** Borrado lógico: deja el producto INACTIVO y resuelve su alerta. */
    public void desactivar(Long id) {
        Producto p = buscar(id);
        p.setEstado(Estado.INACTIVO);
        repo.save(p);
        alertas.evaluar(p);
    }

    private Producto buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new ProductoNoEncontradoException(id));
    }

    private static BigDecimal escala(BigDecimal v) {
        return v == null ? CERO : v.setScale(3);
    }

    private static Specification<Producto> filtro(Estado estado, String q, boolean bajoMinimo) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (estado != null) {
                ps.add(cb.equal(root.get("estado"), estado));
            }
            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("codigo")), patron),
                        cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("categoria")), patron)));
            }
            if (bajoMinimo) {
                ps.add(cb.equal(root.get("estado"), Estado.ACTIVO));
                ps.add(cb.greaterThan(root.<BigDecimal>get("stockMinimo"), BigDecimal.ZERO));
                ps.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("stockActual"), root.<BigDecimal>get("stockMinimo")));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
