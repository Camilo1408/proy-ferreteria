/**
 * nombre: format.js
 * descripcion: Formato de cantidades y fechas según el idioma activo.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */

/**
 * Formatea una cantidad con hasta 3 decimales y separadores del idioma.
 * @param {number|string|null} valor cantidad
 * @param {string} [idioma] código de idioma, por ejemplo `es`
 * @returns {string} texto formateado; vacío si no hay valor
 */
export function formatoCantidad(valor, idioma = 'es') {
  if (valor === null || valor === undefined || valor === '') return '';
  return new Intl.NumberFormat(idioma, { maximumFractionDigits: 3 }).format(Number(valor));
}

/**
 * Formatea una cantidad con signo explícito (+ o −) para variaciones de stock.
 * @param {number|string} valor variación
 * @param {string} [idioma] código de idioma
 * @returns {string} por ejemplo `+5` o `−2,5`
 */
export function formatoVariacion(valor, idioma = 'es') {
  const n = Number(valor);
  const texto = formatoCantidad(Math.abs(n), idioma);
  if (n > 0) return `+${texto}`;
  if (n < 0) return `−${texto}`;
  return texto;
}

/**
 * Formatea un instante ISO como fecha y hora cortas.
 * @param {string|null} iso instante en ISO 8601
 * @param {string} [idioma] código de idioma
 * @returns {string} texto o cadena vacía
 */
export function formatoFecha(iso, idioma = 'es') {
  if (!iso) return '';
  return new Intl.DateTimeFormat(idioma, { dateStyle: 'short', timeStyle: 'short' }).format(new Date(iso));
}

/**
 * Convierte un texto numérico con coma o punto decimal en número.
 * @param {string} texto texto escrito por el usuario
 * @returns {number} número, o NaN si no es válido
 */
export function aNumero(texto) {
  return Number(String(texto).trim().replace(',', '.'));
}
