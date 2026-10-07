/*
 * nombre: ProductoServiceTest.java
 * descripcion: Pruebas unitarias del servicio de productos con Mockito.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.producto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock ProductoRepository repo;
    @InjectMocks ProductoService service;

    private ProductoRequest req(String nombre) {
        return new ProductoRequest(nombre, " Herramientas ", "desc", null);
    }

    @Test @DisplayName("crear: recorta espacios, estado por defecto ACTIVO")
    void creaConValoresPorDefecto() {
        when(repo.existsByNombreIgnoreCase("Martillo")).thenReturn(false);
        when(repo.save(any(Producto.class))).thenAnswer(i -> {
            Producto p = i.getArgument(0);
            p.setId(7L);
            return p;
        });
        ProductoResponse r = service.crear(req("  Martillo  "));
        assertEquals(7L, r.id());
        assertEquals(Estado.ACTIVO, r.estado());
        assertEquals("Herramientas", r.categoria());
        assertEquals("Martillo", r.nombre());
    }

    @Test @DisplayName("crear: guarda todos los campos y respeta un estado explícito")
    void creaGuardaTodosLosCampos() {
        when(repo.existsByNombreIgnoreCase("Sierra")).thenReturn(false);
        when(repo.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));
        service.crear(new ProductoRequest("Sierra", "Corte", "dientes finos", Estado.INACTIVO));
        ArgumentCaptor<Producto> cap = ArgumentCaptor.forClass(Producto.class);
        verify(repo).save(cap.capture());
        assertEquals("Sierra", cap.getValue().getNombre());
        assertEquals("Corte", cap.getValue().getCategoria());
        assertEquals("dientes finos", cap.getValue().getDescripcion());
        assertEquals(Estado.INACTIVO, cap.getValue().getEstado());
    }

    @Test @DisplayName("obtener: devuelve el producto existente")
    void obtenerExistente() {
        when(repo.findById(5L)).thenReturn(Optional.of(
                Producto.builder().id(5L).nombre("Llave").categoria("Mecánica").descripcion("d").build()));
        ProductoResponse r = service.obtener(5L);
        assertEquals(5L, r.id());
        assertEquals("Llave", r.nombre());
        assertEquals("d", r.descripcion());
    }

    @Test @DisplayName("actualizar: modifica nombre, categoría, descripción y estado")
    void actualizarModificaCampos() {
        Producto p = Producto.builder().id(3L).nombre("Sierra").categoria("C").descripcion("a").build();
        when(repo.findById(3L)).thenReturn(Optional.of(p));
        when(repo.existsByNombreIgnoreCaseAndIdNot("Sierra Pro", 3L)).thenReturn(false);
        when(repo.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));
        ProductoResponse r = service.actualizar(3L, new ProductoRequest(" Sierra Pro ", " Corte ", "b", Estado.INACTIVO));
        assertEquals("Sierra Pro", r.nombre());
        assertEquals("Corte", r.categoria());
        assertEquals("b", r.descripcion());
        assertEquals(Estado.INACTIVO, r.estado());
    }

    @Test @DisplayName("listar: mapea la página del repositorio")
    @SuppressWarnings("unchecked")
    void listarMapeaPagina() {
        org.springframework.data.domain.Pageable pag = org.springframework.data.domain.PageRequest.of(1, 2);
        var pagina = new org.springframework.data.domain.PageImpl<>(
                java.util.List.of(Producto.builder().id(1L).nombre("A").categoria("C").build()), pag, 5);
        when(repo.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                org.mockito.ArgumentMatchers.eq(pag))).thenReturn(pagina);
        PaginaResponse<ProductoResponse> r = service.listar(Estado.ACTIVO, "a", pag);
        assertEquals(1, r.content().size());
        assertEquals(5, r.totalElements());
        assertEquals(3, r.totalPages());
        assertEquals(1, r.page());
        assertEquals(2, r.size());
    }

    @Test @DisplayName("crear: nombre repetido lanza excepción y no guarda")
    void rechazaDuplicado() {
        when(repo.existsByNombreIgnoreCase("Martillo")).thenReturn(true);
        assertThrows(NombreDuplicadoException.class, () -> service.crear(req("Martillo")));
        verify(repo, never()).save(any());
    }

    @Test @DisplayName("obtener: inexistente lanza ProductoNoEncontradoException")
    void obtenerInexistente() {
        when(repo.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ProductoNoEncontradoException.class, () -> service.obtener(1L));
    }

    @Test @DisplayName("desactivar: pasa a INACTIVO y guarda")
    void desactivaLogicamente() {
        Producto p = Producto.builder().id(3L).nombre("Sierra").categoria("C").build();
        when(repo.findById(3L)).thenReturn(Optional.of(p));
        service.desactivar(3L);
        ArgumentCaptor<Producto> cap = ArgumentCaptor.forClass(Producto.class);
        verify(repo).save(cap.capture());
        assertEquals(Estado.INACTIVO, cap.getValue().getEstado());
    }

    @Test @DisplayName("actualizar: nombre de otro producto lanza duplicado")
    void actualizarDuplicado() {
        Producto p = Producto.builder().id(3L).nombre("Sierra").categoria("C").build();
        when(repo.findById(3L)).thenReturn(Optional.of(p));
        when(repo.existsByNombreIgnoreCaseAndIdNot("Pinza", 3L)).thenReturn(true);
        assertThrows(NombreDuplicadoException.class, () -> service.actualizar(3L, req("Pinza")));
        verify(repo, never()).save(any());
    }

    @Test @DisplayName("actualizar: sin estado en la petición conserva el estado actual")
    void actualizarConservaEstado() {
        Producto p = Producto.builder().id(3L).nombre("Sierra").categoria("C").estado(Estado.INACTIVO).build();
        when(repo.findById(3L)).thenReturn(Optional.of(p));
        when(repo.existsByNombreIgnoreCaseAndIdNot("Sierra 2", 3L)).thenReturn(false);
        when(repo.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));
        assertEquals(Estado.INACTIVO, service.actualizar(3L, req("Sierra 2")).estado());
    }
}
