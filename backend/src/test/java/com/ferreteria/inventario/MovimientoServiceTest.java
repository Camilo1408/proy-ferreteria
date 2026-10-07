/*
 * nombre: MovimientoServiceTest.java
 * descripcion: Pruebas unitarias de las reglas de movimientos de inventario con Mockito.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.inventario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ferreteria.alerta.AlertaService;
import com.ferreteria.error.NegocioException;
import com.ferreteria.inventario.MovimientoDtos.AjusteRequest;
import com.ferreteria.inventario.MovimientoDtos.MovimientoRequest;
import com.ferreteria.inventario.MovimientoDtos.MovimientoResponse;
import com.ferreteria.producto.Estado;
import com.ferreteria.producto.Producto;
import com.ferreteria.producto.ProductoNoEncontradoException;
import com.ferreteria.producto.ProductoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovimientoServiceTest {

    @Mock MovimientoRepository movimientos;
    @Mock ProductoRepository productos;
    @Mock AlertaService alertas;
    @InjectMocks MovimientoService service;

    private Producto producto;

    @BeforeEach
    void preparar() {
        producto = Producto.builder().id(1L).codigo("P-1").nombre("Prod").categoria("C")
                .stockActual(new BigDecimal("10.000")).stockMinimo(new BigDecimal("2.000")).build();
    }

    private void productoExiste() {
        when(productos.findByIdForUpdate(1L)).thenReturn(Optional.of(producto));
        when(movimientos.save(any(Movimiento.class))).thenAnswer(i -> i.getArgument(0));
    }

    private MovimientoRequest req(String cantidad, Motivo motivo) {
        return new MovimientoRequest(1L, new BigDecimal(cantidad), motivo, "  F-1 ", "  nota ");
    }

    @Test @DisplayName("entrada: suma al stock, guarda anterior/resultante/variación, limpia textos y evalúa alertas")
    void entradaSuma() {
        productoExiste();
        MovimientoResponse r = service.entrada(req("5", Motivo.COMPRA));
        assertEquals(new BigDecimal("15.000"), producto.getStockActual());
        assertEquals(TipoMovimiento.ENTRADA, r.tipo());
        assertEquals(new BigDecimal("10.000"), r.stockAnterior());
        assertEquals(new BigDecimal("15.000"), r.stockResultante());
        assertEquals(new BigDecimal("5.000"), r.variacion());
        assertEquals(new BigDecimal("5.000"), r.cantidad());
        assertEquals("F-1", r.referencia());
        assertEquals("nota", r.nota());
        assertEquals("sistema", r.usuario());
        verify(productos).save(producto);
        verify(alertas).evaluar(producto);
    }

    @Test @DisplayName("salida: resta, variación negativa y deja nulos los textos vacíos")
    void salidaResta() {
        productoExiste();
        MovimientoResponse r = service.salida(new MovimientoRequest(1L, new BigDecimal("4"), Motivo.VENTA, " ", ""));
        assertEquals(new BigDecimal("6.000"), producto.getStockActual());
        assertEquals(new BigDecimal("-4.000"), r.variacion());
        assertEquals(new BigDecimal("4.000"), r.cantidad());
        assertEquals(null, r.referencia());
        assertEquals(null, r.nota());
    }

    @Test @DisplayName("salida: exactamente el stock deja 0; un poco más lanza STOCK_INSUFICIENTE sin guardar")
    void salidaLimite() {
        productoExiste();
        service.salida(req("10", Motivo.VENTA));
        assertEquals(0, producto.getStockActual().signum());
        producto.setStockActual(new BigDecimal("10.000"));
        NegocioException e = assertThrows(NegocioException.class, () -> service.salida(req("10.001", Motivo.VENTA)));
        assertEquals("STOCK_INSUFICIENTE", e.getCodigo());
        assertEquals(new BigDecimal("10.000"), producto.getStockActual());
    }

    @Test @DisplayName("motivo de otro tipo lanza MOTIVO_INVALIDO antes de tocar el producto")
    void motivoInvalido() {
        NegocioException e = assertThrows(NegocioException.class, () -> service.entrada(req("1", Motivo.VENTA)));
        assertEquals("MOTIVO_INVALIDO", e.getCodigo());
        assertThrows(NegocioException.class, () -> service.salida(req("1", Motivo.COMPRA)));
        assertThrows(NegocioException.class, () -> service.ajuste(new AjusteRequest(1L, BigDecimal.ONE, Motivo.MERMA, "nota larga")));
        verify(productos, never()).findByIdForUpdate(any());
    }

    @Test @DisplayName("producto inexistente lanza ProductoNoEncontradoException; inactivo lanza PRODUCTO_INACTIVO")
    void productoNoApto() {
        when(productos.findByIdForUpdate(1L)).thenReturn(Optional.empty());
        assertThrows(ProductoNoEncontradoException.class, () -> service.entrada(req("1", Motivo.COMPRA)));
        producto.setEstado(Estado.INACTIVO);
        when(productos.findByIdForUpdate(1L)).thenReturn(Optional.of(producto));
        NegocioException e = assertThrows(NegocioException.class, () -> service.entrada(req("1", Motivo.COMPRA)));
        assertEquals("PRODUCTO_INACTIVO", e.getCodigo());
        verify(movimientos, never()).save(any());
    }

    @Test @DisplayName("ajuste: lleva el stock al contado con variación con signo y cantidad absoluta")
    void ajusteConDiferencia() {
        productoExiste();
        MovimientoResponse baja = service.ajuste(new AjusteRequest(1L, new BigDecimal("7.5"), Motivo.CONTEO_FISICO, "Conteo"));
        assertEquals(new BigDecimal("-2.500"), baja.variacion());
        assertEquals(new BigDecimal("2.500"), baja.cantidad());
        assertEquals(new BigDecimal("7.500"), producto.getStockActual());
        MovimientoResponse sube = service.ajuste(new AjusteRequest(1L, new BigDecimal("8"), Motivo.CORRECCION, "Corrección"));
        assertEquals(new BigDecimal("0.500"), sube.variacion());
        assertEquals(TipoMovimiento.AJUSTE, sube.tipo());
        assertEquals(null, sube.referencia());
    }

    @Test @DisplayName("ajuste: contado igual al stock lanza AJUSTE_SIN_DIFERENCIA")
    void ajusteSinDiferencia() {
        when(productos.findByIdForUpdate(1L)).thenReturn(Optional.of(producto));
        NegocioException e = assertThrows(NegocioException.class,
                () -> service.ajuste(new AjusteRequest(1L, new BigDecimal("10"), Motivo.CONTEO_FISICO, "Sin cambios")));
        assertEquals("AJUSTE_SIN_DIFERENCIA", e.getCodigo());
        verify(movimientos, never()).save(any());
    }

    @Test @DisplayName("stock inicial: registra una entrada STOCK_INICIAL con anterior 0")
    void registraInicial() {
        Producto nuevo = Producto.builder().id(2L).codigo("N-1").nombre("Nuevo").categoria("C").build();
        when(movimientos.save(any(Movimiento.class))).thenAnswer(i -> i.getArgument(0));
        service.registrarInicial(nuevo, new BigDecimal("25"));
        ArgumentCaptor<Movimiento> cap = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientos).save(cap.capture());
        assertEquals(Motivo.STOCK_INICIAL, cap.getValue().getMotivo());
        assertEquals(TipoMovimiento.ENTRADA, cap.getValue().getTipo());
        assertEquals(0, cap.getValue().getStockAnterior().signum());
        assertEquals(new BigDecimal("25.000"), cap.getValue().getStockResultante());
        assertEquals(new BigDecimal("25.000"), nuevo.getStockActual());
    }

    @Test @DisplayName("cada motivo pertenece a un único tipo y hay motivos para los tres tipos")
    void motivosPorTipo() {
        for (TipoMovimiento t : TipoMovimiento.values()) {
            assertEquals(true, java.util.Arrays.stream(Motivo.values()).anyMatch(m -> m.tipo() == t), t.name());
        }
        assertEquals(TipoMovimiento.ENTRADA, Motivo.COMPRA.tipo());
        assertEquals(TipoMovimiento.SALIDA, Motivo.MERMA.tipo());
        assertEquals(TipoMovimiento.AJUSTE, Motivo.DANO.tipo());
    }
}
