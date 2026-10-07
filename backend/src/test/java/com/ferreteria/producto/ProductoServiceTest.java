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
