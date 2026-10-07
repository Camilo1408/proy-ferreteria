/*
 * nombre: ProductoController.java
 * descripcion: API REST del CRUD de productos.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.producto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints REST de productos bajo {@code /api/v1/productos}. */
@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "CRUD de productos (escritura solo ADMIN)")
@SecurityRequirement(name = "bearer")
public class ProductoController {

    private static final int TAM_MAX = 100;

    private final ProductoService service;

    /** Lista paginada con filtros opcionales. */
    @GetMapping
    @Operation(summary = "Listar productos")
    public PaginaResponse<ProductoResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Estado estado,
            @RequestParam(required = false) String q) {
        int pagina = Math.max(page, 0);
        int tam = Math.min(Math.max(size, 1), TAM_MAX);
        return service.listar(estado, q, PageRequest.of(pagina, tam, Sort.by("id")));
    }

    /** Obtiene un producto por id. */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener un producto")
    public ProductoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /** Crea un producto (ADMIN). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un producto")
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest body) {
        return service.crear(body);
    }

    /** Actualiza un producto (ADMIN). */
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un producto")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest body) {
        return service.actualizar(id, body);
    }

    /** Desactiva un producto: borrado lógico (ADMIN). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactivar un producto")
    public void desactivar(@PathVariable Long id) {
        service.desactivar(id);
    }
}
