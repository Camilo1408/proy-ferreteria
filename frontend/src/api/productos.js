/**
 * nombre: productos.js
 * descripcion: Funciones de acceso a la API de productos y autenticación.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import { request } from './client';

/** Inicia sesión y devuelve `{token, username, rol}`. */
export const login = (username, password) =>
  request('/api/v1/auth/login', { method: 'POST', body: { username, password } });

/** Lista productos paginados con filtros opcionales. */
export function listar({ page = 0, size = 10, estado = '', q = '' } = {}) {
  const p = new URLSearchParams({ page, size });
  if (estado) p.set('estado', estado);
  if (q) p.set('q', q);
  return request(`/api/v1/productos?${p}`);
}

/** Crea un producto. */
export const crear = (producto) => request('/api/v1/productos', { method: 'POST', body: producto });

/** Actualiza un producto. */
export const actualizar = (id, producto) =>
  request(`/api/v1/productos/${id}`, { method: 'PUT', body: producto });

/** Desactiva un producto (borrado lógico). */
export const desactivar = (id) => request(`/api/v1/productos/${id}`, { method: 'DELETE' });
