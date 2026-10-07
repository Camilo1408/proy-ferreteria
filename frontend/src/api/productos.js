/**
 * nombre: productos.js
 * descripcion: Funciones de acceso a la API de productos y autenticación.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { consulta, descargar, request } from './client';

/** Inicia sesión y devuelve `{token, usuario}`. */
export const login = (username, password) =>
  request('/api/v1/auth/login', { method: 'POST', body: { username, password } });

/** Lista productos paginados con filtros opcionales (`bajoMinimo` limita a productos en mínimos). */
export const listar = ({ page = 0, size = 10, estado = '', q = '', bajoMinimo = false } = {}) =>
  request(`/api/v1/productos${consulta({ page, size, estado, q, bajoMinimo })}`);

/** Obtiene un producto por id. */
export const obtener = (id) => request(`/api/v1/productos/${id}`);

/** Crea un producto. */
export const crear = (producto) => request('/api/v1/productos', { method: 'POST', body: producto });

/** Actualiza un producto. */
export const actualizar = (id, producto) =>
  request(`/api/v1/productos/${id}`, { method: 'PUT', body: producto });

/** Desactiva un producto (borrado lógico). */
export const desactivar = (id) => request(`/api/v1/productos/${id}`, { method: 'DELETE' });

/** Descarga el inventario en CSV. */
export const descargarInventario = (bajoMinimo = false) =>
  descargar(`/api/v1/reportes/inventario.csv${consulta({ bajoMinimo })}`, 'inventario.csv');

/** Números del panel de inicio. */
export const dashboard = () => request('/api/v1/dashboard');
