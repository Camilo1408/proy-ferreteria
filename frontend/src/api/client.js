/**
 * nombre: client.js
 * descripcion: Cliente HTTP JSON para la API (token Bearer, Accept-Language, errores uniformes, descargas).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
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

function cabeceras(extra = {}) {
  return {
    'Accept-Language': i18n.language,
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...extra,
  };
}

async function enviar(path, init) {
  try {
    return await fetch(`${BASE}${path}`, init);
  } catch {
    throw new ApiError(0, { mensaje: i18n.t('errores.red') });
  }
}

async function validar(res) {
  if (res.ok) return;
  const data = await res.json().catch(() => null);
  if (res.status === 401 && token) onUnauthorized();
  throw new ApiError(res.status, data);
}

/**
 * Ejecuta una petición JSON.
 * @param {string} path ruta relativa, por ejemplo `/api/v1/productos`
 * @param {{method?: string, body?: object}} [opts] opciones
 * @returns {Promise<any>} cuerpo JSON o null si no hay contenido
 */
export async function request(path, { method = 'GET', body } = {}) {
  const res = await enviar(path, {
    method,
    headers: cabeceras({ 'Content-Type': 'application/json' }),
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.status === 204) return null;
  if (!res.ok) await validar(res);
  return res.json();
}

/**
 * Descarga un archivo autenticado y lo entrega al navegador.
 * @param {string} path ruta relativa
 * @param {string} nombre nombre del archivo a guardar
 */
export async function descargar(path, nombre) {
  const res = await enviar(path, { headers: cabeceras() });
  await validar(res);
  const url = URL.createObjectURL(await res.blob());
  const a = document.createElement('a');
  a.href = url;
  a.download = nombre;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

/** Arma una cadena de consulta omitiendo valores vacíos. */
export function consulta(params) {
  const p = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== '' && v !== null && v !== undefined && v !== false) p.set(k, v);
  });
  const s = p.toString();
  return s ? `?${s}` : '';
}
