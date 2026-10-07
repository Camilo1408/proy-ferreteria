/*
 * nombre: ProductoServiceTest.java
 * descripcion: Pruebas unitarias del servicio de productos con Mockito.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.producto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ferreteria.alerta.AlertaService;
import com.ferreteria.error.NegocioException;
import com.ferreteria.inventario.MovimientoService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock ProductoRepository repo;
    @Mock MovimientoService movimientos;
    @Mock AlertaService alertas;
    @InjectMocks ProductoService service;

    private ProductoRequest req(String codigo, String nombre) {
        return new ProductoRequest(codigo, nombre, " Herramientas ", "desc", Unidad.UND, null, null, null);
    }

    private ProductoRequest req(BigDecimal minimo, BigDecimal inicial, Estado estado) {
        return new ProductoRequest(" C-1 ", " Martillo ", "Cat", null, Unidad.KG, minimo, inicial, estado);
    }

    private void guardarDevuelveArgumento() {
        when(repo.save(any(Producto.class))).thenAnswer(i -> {
            Producto p = i.getArgument(0);
            if (p.getId() == null) {
                p.setId(7L);
            }
            return p;
        });
    }

    @Test @DisplayName("crear: recorta espacios, estado ACTIVO, stock 0 y mínimo 0 por defecto, y evalúa alertas")
    void creaConValoresPorDefecto() {
        guardarDevuelveArgumento();
        ProductoResponse r = service.crear(req(null, null, null));
        assertEquals(7L, r.id());
        assertEquals("C-1", r.codigo());
        assertEquals("Martillo", r.nombre());
        assertEquals(Estado.ACTIVO, r.estado());
        assertEquals(Unidad.KG, r.unidad());
        assertEquals(0, r.stockActual().signum());
        assertEquals(0, r.stockMinimo().signum());
        assertEquals(NivelStock.OK, r.nivel());
        verify(alertas).evaluar(any(Producto.class));
        verify(movimientos, never()).registrarInicial(any(), any());
    }

    @Test @DisplayName("crear: guarda mínimo y estado explícitos; con stock inicial registra la entrada")
    void creaConStockInicial() {
        guardarDevuelveArgumento();
        ProductoResponse r = service.crear(req(new BigDecimal("5"), new BigDecimal("12.5"), Estado.INACTIVO));
        assertEquals(new BigDecimal("5.000"), r.stockMinimo());
        assertEquals(Estado.INACTIVO, r.estado());
        verify(movimientos).registrarInicial(any(Producto.class), eq(new BigDecimal("12.5")));
    }

    @Test @DisplayName("crear: stock inicial 0 no registra movimiento")
    void stockInicialCeroNoRegistra() {
        guardarDevuelveArgumento();
        service.crear(req(null, BigDecimal.ZERO, null));
        verify(movimientos, never()).registrarInicial(any(), any());
    }

    @Test @DisplayName("crear: nombre repetido lanza excepción y no guarda")
    void rechazaNombreDuplicado() {
        when(repo.existsByNombreIgnoreCase("Martillo")).thenReturn(true);
        assertThrows(NombreDuplicadoException.class, () -> service.crear(req("C-1", "Martillo")));
        verify(repo, never()).save(any());
    }

    @Test @DisplayName("crear: código repetido lanza CODIGO_DUPLICADO y no guarda")
    void rechazaCodigoDuplicado() {
        when(repo.existsByCodigoIgnoreCase("C-1")).thenReturn(true);
        NegocioException e = assertThrows(NegocioException.class, () -> service.crear(req("C-1", "Martillo")));
        assertEquals("CODIGO_DUPLICADO", e.getCodigo());
        verify(repo, never()).save(any());
    }

    @Test @DisplayName("obtener: inexistente lanza ProductoNoEncontradoException")
    void obtenerInexistente() {
        when(repo.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ProductoNoEncontradoException.class, () -> service.obtener(1L));
    }

    @Test @DisplayName("obtener: devuelve todos los campos del producto")
    void obtenerExistente() {
        when(repo.findById(5L)).thenReturn(Optional.of(Producto.builder().id(5L).codigo("LL-1").nombre("Llave")
                .categoria("Mecánica").descripcion("d").unidad(Unidad.M).stockActual(new BigDecimal("3.000"))
                .stockMinimo(new BigDecimal("3.000")).build()));
        ProductoResponse r = service.obtener(5L);
        assertEquals(5L, r.id());
        assertEquals("LL-1", r.codigo());
        assertEquals("Llave", r.nombre());
        assertEquals("d", r.descripcion());
        assertEquals(Unidad.M, r.unidad());
        assertEquals(NivelStock.BAJO, r.nivel());
    }

    @Test @DisplayName("desactivar: pasa a INACTIVO, guarda y evalúa alertas")
    void desactivaLogicamente() {
        Producto p = Producto.builder().id(3L).nombre("Sierra").categoria("C").build();
        when(repo.findById(3L)).thenReturn(Optional.of(p));
        service.desactivar(3L);
        ArgumentCaptor<Producto> cap = ArgumentCaptor.forClass(Producto.class);
        verify(repo).save(cap.capture());
        assertEquals(Estado.INACTIVO, cap.getValue().getEstado());
        verify(alertas).evaluar(p);
    }

    @Test @DisplayName("actualizar: nombre de otro producto lanza duplicado")
    void actualizarNombreDuplicado() {
        Producto p = Producto.builder().id(3L).nombre("Sierra").categoria("C").build();
        when(repo.findById(3L)).thenReturn(Optional.of(p));
        when(repo.existsByNombreIgnoreCaseAndIdNot("Pinza", 3L)).thenReturn(true);
        assertThrows(NombreDuplicadoException.class, () -> service.actualizar(3L, req("C-1", "Pinza")));
        verify(repo, never()).save(any());
    }

    @Test @DisplayName("actualizar: código de otro producto lanza CODIGO_DUPLICADO")
    void actualizarCodigoDuplicado() {
        Producto p = Producto.builder().id(3L).nombre("Sierra").categoria("C").build();
        when(repo.findById(3L)).thenReturn(Optional.of(p));
        when(repo.existsByCodigoIgnoreCaseAndIdNot("C-1", 3L)).thenReturn(true);
        assertThrows(NegocioException.class, () -> service.actualizar(3L, req("C-1", "Sierra")));
        verify(repo, never()).save(any());
    }

    @Test @DisplayName("actualizar: modifica los campos, no toca el stock y conserva el estado si no se indica")
    void actualizarModificaCampos() {
        Producto p = Producto.builder().id(3L).codigo("OLD").nombre("Sierra").categoria("C").descripcion("a")
                .stockActual(new BigDecimal("9.000")).estado(Estado.INACTIVO).build();
        when(repo.findById(3L)).thenReturn(Optional.of(p));
        when(repo.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));
        ProductoResponse r = service.actualizar(3L, new ProductoRequest(" NEW ", " Sierra Pro ", " Corte ", "b", Unidad.CAJA,
                new BigDecimal("2"), new BigDecimal("500"), null));
        assertEquals("NEW", r.codigo());
        assertEquals("Sierra Pro", r.nombre());
        assertEquals("Corte", r.categoria());
        assertEquals("b", r.descripcion());
        assertEquals(Unidad.CAJA, r.unidad());
        assertEquals(new BigDecimal("2.000"), r.stockMinimo());
        assertEquals(new BigDecimal("9.000"), r.stockActual());
        assertEquals(Estado.INACTIVO, r.estado());
        verify(alertas).evaluar(p);
    }

    @Test @DisplayName("actualizar: con estado explícito lo cambia")
    void actualizarCambiaEstado() {
        Producto p = Producto.builder().id(3L).nombre("Sierra").categoria("C").estado(Estado.INACTIVO).build();
        when(repo.findById(3L)).thenReturn(Optional.of(p));
        when(repo.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));
        assertEquals(Estado.ACTIVO, service.actualizar(3L, req(null, null, Estado.ACTIVO)).estado());
    }

    @Test @DisplayName("listar: mapea la página del repositorio")
    @SuppressWarnings("unchecked")
    void listarMapeaPagina() {
        Pageable pag = PageRequest.of(1, 2);
        var pagina = new PageImpl<>(List.of(Producto.builder().id(1L).nombre("A").categoria("C").build()), pag, 5);
        when(repo.findAll(any(Specification.class), eq(pag))).thenReturn(pagina);
        PaginaResponse<ProductoResponse> r = service.listar(Estado.ACTIVO, "a", true, pag);
        assertEquals(1, r.content().size());
        assertEquals(5, r.totalElements());
        assertEquals(3, r.totalPages());
        assertEquals(1, r.page());
        assertEquals(2, r.size());
    }

    @Test @DisplayName("nivel: reglas de AGOTADO, BAJO y OK según mínimo y stock")
    void reglaDeNivel() {
        assertEquals(NivelStock.OK, nivel("0", "0"));
        assertEquals(NivelStock.OK, nivel("6", "5"));
        assertEquals(NivelStock.BAJO, nivel("5", "5"));
        assertEquals(NivelStock.BAJO, nivel("0.001", "5"));
        assertEquals(NivelStock.AGOTADO, nivel("0", "5"));
        assertEquals(NivelStock.OK, nivel("0", "0"));
    }

    private NivelStock nivel(String stock, String minimo) {
        return Producto.builder().stockActual(new BigDecimal(stock)).stockMinimo(new BigDecimal(minimo)).build().nivel();
    }
}
