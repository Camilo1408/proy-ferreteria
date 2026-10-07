/**
 * nombre: inventario.js
 * descripcion: Funciones de acceso a movimientos, alertas, usuarios, perfiles y cuenta propia.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { consulta, request } from './client';

// ---- Movimientos ----
/** Historial de movimientos con filtros. */
export const listarMovimientos = ({ page = 0, size = 10, productoId = '', tipo = '', desde = '', hasta = '' } = {}) =>
  request(`/api/v1/movimientos${consulta({ page, size, productoId, tipo, desde, hasta })}`);

/** Registra una entrada, salida o ajuste según `tipo`. */
export function registrarMovimiento(tipo, cuerpo) {
  const ruta = { ENTRADA: 'entradas', SALIDA: 'salidas', AJUSTE: 'ajustes' }[tipo];
  return request(`/api/v1/movimientos/${ruta}`, { method: 'POST', body: cuerpo });
}

// ---- Alertas ----
/** Alertas vigentes por defecto; con `estado` filtra por ese estado. */
export const listarAlertas = ({ page = 0, size = 10, estado = '' } = {}) =>
  request(`/api/v1/alertas${consulta({ page, size, estado })}`);

/** Contadores de alertas. */
export const resumenAlertas = () => request('/api/v1/alertas/resumen');

/** Reconoce una alerta. */
export const reconocerAlerta = (id) => request(`/api/v1/alertas/${id}/reconocer`, { method: 'POST' });

// ---- Usuarios ----
export const listarUsuarios = ({ page = 0, size = 10, q = '' } = {}) =>
  request(`/api/v1/usuarios${consulta({ page, size, q })}`);
export const crearUsuario = (cuerpo) => request('/api/v1/usuarios', { method: 'POST', body: cuerpo });
export const actualizarUsuario = (id, cuerpo) => request(`/api/v1/usuarios/${id}`, { method: 'PUT', body: cuerpo });
export const restablecerClave = (id, nueva) =>
  request(`/api/v1/usuarios/${id}/clave`, { method: 'PUT', body: { nueva } });

// ---- Perfiles ----
export const listarPerfiles = () => request('/api/v1/perfiles');
export const catalogoPermisos = () => request('/api/v1/perfiles/permisos');
export const crearPerfil = (cuerpo) => request('/api/v1/perfiles', { method: 'POST', body: cuerpo });
export const actualizarPerfil = (id, cuerpo) => request(`/api/v1/perfiles/${id}`, { method: 'PUT', body: cuerpo });
export const eliminarPerfil = (id) => request(`/api/v1/perfiles/${id}`, { method: 'DELETE' });

// ---- Cuenta propia ----
export const obtenerCuenta = () => request('/api/v1/cuenta');
export const actualizarCuenta = (cuerpo) => request('/api/v1/cuenta', { method: 'PUT', body: cuerpo });
export const cambiarClave = (actual, nueva) =>
  request('/api/v1/cuenta/clave', { method: 'PUT', body: { actual, nueva } });
