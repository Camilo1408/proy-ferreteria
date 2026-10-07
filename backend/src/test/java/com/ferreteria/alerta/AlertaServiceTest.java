/*
 * nombre: AlertaServiceTest.java
 * descripcion: Pruebas unitarias del ciclo de vida de las alertas de stock mínimo con Mockito.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.alerta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ferreteria.alerta.AlertaDtos.AlertaResponse;
import com.ferreteria.alerta.AlertaDtos.ResumenAlertas;
import com.ferreteria.error.NegocioException;
import com.ferreteria.producto.Estado;
import com.ferreteria.producto.Producto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

    @Mock AlertaRepository repo;
    @InjectMocks AlertaService service;

    private Producto producto(String stock, String minimo, Estado estado) {
        return Producto.builder().id(1L).codigo("P-1").nombre("Prod").categoria("C").estado(estado)
                .stockActual(new BigDecimal(stock)).stockMinimo(new BigDecimal(minimo)).build();
    }

    private Alerta activa(NivelAlerta nivel, EstadoAlerta estado) {
        return Alerta.builder().id(9L).producto(producto("1", "5", Estado.ACTIVO)).nivel(nivel).estado(estado)
                .stockAlDetectar(BigDecimal.ONE).stockMinimo(new BigDecimal("5")).creadaEn(Instant.now()).build();
    }

    @Test @DisplayName("evaluar: en el mínimo crea una alerta ABIERTA BAJO con los datos del producto")
    void creaAlertaBaja() {
        when(repo.findByProductoIdAndEstadoIn(1L, AlertaService.ACTIVAS)).thenReturn(List.of());
        service.evaluar(producto("5", "5", Estado.ACTIVO));
        ArgumentCaptor<Alerta> cap = ArgumentCaptor.forClass(Alerta.class);
        verify(repo).save(cap.capture());
        assertEquals(NivelAlerta.BAJO, cap.getValue().getNivel());
        assertEquals(EstadoAlerta.ABIERTA, cap.getValue().getEstado());
        assertEquals(new BigDecimal("5"), cap.getValue().getStockAlDetectar());
        assertEquals(new BigDecimal("5"), cap.getValue().getStockMinimo());
        assertNotNull(cap.getValue().getCreadaEn());
    }

    @Test @DisplayName("evaluar: con stock 0 y mínimo definido crea una alerta AGOTADO")
    void creaAlertaAgotada() {
        when(repo.findByProductoIdAndEstadoIn(1L, AlertaService.ACTIVAS)).thenReturn(List.of());
        service.evaluar(producto("0", "5", Estado.ACTIVO));
        ArgumentCaptor<Alerta> cap = ArgumentCaptor.forClass(Alerta.class);
        verify(repo).save(cap.capture());
        assertEquals(NivelAlerta.AGOTADO, cap.getValue().getNivel());
    }

    @Test @DisplayName("evaluar: sobre el mínimo, sin mínimo o inactivo no crea alerta")
    void noCreaAlerta() {
        when(repo.findByProductoIdAndEstadoIn(1L, AlertaService.ACTIVAS)).thenReturn(List.of());
        service.evaluar(producto("6", "5", Estado.ACTIVO));
        service.evaluar(producto("0", "0", Estado.ACTIVO));
        service.evaluar(producto("0", "5", Estado.INACTIVO));
        verify(repo, never()).save(any());
    }

    @Test @DisplayName("evaluar: al reponer resuelve la alerta vigente con fecha")
    void resuelveAlRecuperar() {
        Alerta a = activa(NivelAlerta.BAJO, EstadoAlerta.RECONOCIDA);
        when(repo.findByProductoIdAndEstadoIn(1L, AlertaService.ACTIVAS)).thenReturn(List.of(a));
        service.evaluar(producto("6", "5", Estado.ACTIVO));
        assertEquals(EstadoAlerta.RESUELTA, a.getEstado());
        assertNotNull(a.getResueltaEn());
        verify(repo, never()).save(any());
    }

    @Test @DisplayName("evaluar: un producto inactivo con alerta vigente la resuelve")
    void resuelveAlDesactivar() {
        Alerta a = activa(NivelAlerta.BAJO, EstadoAlerta.ABIERTA);
        when(repo.findByProductoIdAndEstadoIn(1L, AlertaService.ACTIVAS)).thenReturn(List.of(a));
        service.evaluar(producto("1", "5", Estado.INACTIVO));
        assertEquals(EstadoAlerta.RESUELTA, a.getEstado());
    }

    @Test @DisplayName("evaluar: de BAJO a AGOTADO reabre una alerta reconocida y borra el reconocimiento")
    void reabreAlEmpeorar() {
        Alerta a = activa(NivelAlerta.BAJO, EstadoAlerta.RECONOCIDA);
        a.setReconocidaPor("ana");
        a.setReconocidaEn(Instant.now());
        when(repo.findByProductoIdAndEstadoIn(1L, AlertaService.ACTIVAS)).thenReturn(List.of(a));
        service.evaluar(producto("0", "7", Estado.ACTIVO));
        assertEquals(NivelAlerta.AGOTADO, a.getNivel());
        assertEquals(EstadoAlerta.ABIERTA, a.getEstado());
        assertNull(a.getReconocidaPor());
        assertNull(a.getReconocidaEn());
        assertEquals(new BigDecimal("7"), a.getStockMinimo());
    }

    @Test @DisplayName("evaluar: de AGOTADO a BAJO actualiza el nivel sin cambiar el estado reconocido")
    void mejoraSinReabrir() {
        Alerta a = activa(NivelAlerta.AGOTADO, EstadoAlerta.RECONOCIDA);
        a.setReconocidaPor("ana");
        when(repo.findByProductoIdAndEstadoIn(1L, AlertaService.ACTIVAS)).thenReturn(List.of(a));
        service.evaluar(producto("2", "5", Estado.ACTIVO));
        assertEquals(NivelAlerta.BAJO, a.getNivel());
        assertEquals(EstadoAlerta.RECONOCIDA, a.getEstado());
        assertEquals("ana", a.getReconocidaPor());
    }

    @Test @DisplayName("evaluar: mismo nivel solo actualiza el mínimo y no duplica")
    void mismoNivelActualizaMinimo() {
        Alerta a = activa(NivelAlerta.BAJO, EstadoAlerta.ABIERTA);
        when(repo.findByProductoIdAndEstadoIn(1L, AlertaService.ACTIVAS)).thenReturn(List.of(a));
        service.evaluar(producto("3", "8", Estado.ACTIVO));
        assertEquals(new BigDecimal("8"), a.getStockMinimo());
        assertEquals(EstadoAlerta.ABIERTA, a.getEstado());
        verify(repo, never()).save(any());
    }

    @Test @DisplayName("reconocer: ABIERTA pasa a RECONOCIDA con usuario y fecha; RECONOCIDA es idempotente")
    void reconoce() {
        Alerta a = activa(NivelAlerta.BAJO, EstadoAlerta.ABIERTA);
        when(repo.findById(9L)).thenReturn(Optional.of(a));
        AlertaResponse r = service.reconocer(9L, "ana");
        assertEquals(EstadoAlerta.RECONOCIDA, r.estado());
        assertEquals("ana", r.reconocidaPor());
        assertNotNull(r.reconocidaEn());
        Instant primera = a.getReconocidaEn();
        service.reconocer(9L, "luis");
        assertEquals("ana", a.getReconocidaPor());
        assertEquals(primera, a.getReconocidaEn());
    }

    @Test @DisplayName("reconocer: resuelta lanza ALERTA_NO_RECONOCIBLE; inexistente lanza ALERTA_NO_ENCONTRADA")
    void reconocerInvalida() {
        when(repo.findById(9L)).thenReturn(Optional.of(activa(NivelAlerta.BAJO, EstadoAlerta.RESUELTA)));
        assertEquals("ALERTA_NO_RECONOCIBLE", assertThrows(NegocioException.class, () -> service.reconocer(9L, "x")).getCodigo());
        when(repo.findById(8L)).thenReturn(Optional.empty());
        assertEquals("ALERTA_NO_ENCONTRADA", assertThrows(NegocioException.class, () -> service.reconocer(8L, "x")).getCodigo());
    }

    @Test @DisplayName("resumen: arma los contadores desde el repositorio")
    void resumen() {
        when(repo.countByEstado(EstadoAlerta.ABIERTA)).thenReturn(3L);
        when(repo.countByEstadoInAndNivel(AlertaService.ACTIVAS, NivelAlerta.BAJO)).thenReturn(4L);
        when(repo.countByEstadoInAndNivel(AlertaService.ACTIVAS, NivelAlerta.AGOTADO)).thenReturn(2L);
        assertEquals(new ResumenAlertas(3, 4, 2), service.resumen());
    }

    @Test @DisplayName("respuesta: el faltante es el mínimo menos el stock y nunca negativo")
    void faltante() {
        Alerta a = activa(NivelAlerta.BAJO, EstadoAlerta.ABIERTA);
        assertEquals(new BigDecimal("4"), AlertaResponse.de(a).faltante());
        a.getProducto().setStockActual(new BigDecimal("9"));
        assertEquals(0, AlertaResponse.de(a).faltante().signum());
    }
}
