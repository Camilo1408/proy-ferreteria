/**
 * nombre: client.js
 * descripcion: Cliente HTTP JSON para la API (token Bearer, Accept-Language, errores uniformes).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import i18n from '../i18n';

const BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';
let token = null;
let onUnauthorized = () => {};

/** Define el token JWT usado en las peticiones. */
export function setToken(t) {
  token = t;
}

/** Registra la función a ejecutar cuando la API responde 401 con sesión activa. */
export function setOnUnauthorized(fn) {
  onUnauthorized = fn;
}

/** Error de API con el cuerpo `{codigo, mensaje, detalles}` y el estado HTTP. */
export class ApiError extends Error {
  constructor(status, body) {
    super(body?.mensaje ?? i18n.t('errores.generico'));
    this.status = status;
    this.codigo = body?.codigo;
    this.detalles = body?.detalles ?? [];
  }
}

/**
 * Ejecuta una petición JSON.
 * @param {string} path ruta relativa, por ejemplo `/api/v1/productos`
 * @param {{method?: string, body?: object}} [opts] opciones
 * @returns {Promise<any>} cuerpo JSON o null si no hay contenido
 */
export async function request(path, { method = 'GET', body } = {}) {
  let res;
  try {
    res = await fetch(`${BASE}${path}`, {
      method,
      headers: {
        'Content-Type': 'application/json',
        'Accept-Language': i18n.language,
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, { mensaje: i18n.t('errores.red') });
  }
  if (res.status === 204) return null;
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    if (res.status === 401 && token) onUnauthorized();
    throw new ApiError(res.status, data);
  }
  return data;
}
