/**
 * nombre: helpers.js
 * descripcion: Utilidades compartidas de las pruebas E2E (sesión, navegación, datos únicos y formularios).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { expect } from '@playwright/test';

export const ADMIN = { u: 'admin', p: process.env.E2E_ADMIN_PASSWORD ?? 'Admin123*' };
export const USER = { u: 'user', p: process.env.E2E_USER_PASSWORD ?? 'User123*' };
export const CLAVE = 'Clave12345';

let contador = 0;
/** Sufijo único por ejecución y por llamada para no chocar con datos previos. */
export const sufijo = () => `${Date.now().toString(36)}${(contador++).toString(36)}${Math.floor(Math.random() * 99)}`.toUpperCase();
/** Nombre único legible. */
export const unico = (base) => `${base} ${sufijo()}`;
/** Código único válido (2 a 30 caracteres). */
export const codigoUnico = (prefijo = 'T') => `${prefijo}-${sufijo()}`.slice(0, 30);

/** Inicia sesión por la interfaz y espera la navegación principal. */
export async function entrar(page, cred = ADMIN) {
  await page.goto('/');
  await page.getByLabel('Usuario').fill(cred.u);
  await page.getByLabel('Contraseña').fill(cred.p);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByRole('navigation', { name: 'Navegación principal' })).toBeVisible();
}

/** Va a una sección con el enlace de la navegación. */
export async function irA(page, seccion) {
  await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('link', { name: seccion, exact: true }).click();
}

/**
 * Abre un campo de selección de MUI por su etiqueta y elige una opción.
 * Las opciones se montan fuera del diálogo, por eso se buscan desde la página.
 * @param raiz página o localizador donde está el campo
 * @param etiqueta nombre accesible del campo (texto o expresión regular)
 * @param opcion texto exacto de la opción, o expresión regular
 */
export async function elegir(raiz, etiqueta, opcion) {
  const pagina = typeof raiz.page === 'function' ? raiz.page() : raiz;
  await raiz.getByRole('combobox', { name: etiqueta }).click();
  const nombre = typeof opcion === 'string' ? { name: opcion, exact: true } : { name: opcion };
  await pagina.getByRole('option', nombre).click();
}

/** Crea un producto desde la pantalla de productos. Devuelve sus datos. */
export async function crearProducto(page, { codigo = codigoUnico('P'), nombre = unico('Producto'), categoria = 'Herramientas', unidad, minimo, inicial, descripcion } = {}) {
  await irA(page, 'Productos');
  await page.getByRole('button', { name: 'Nuevo producto' }).click();
  const dialogo = page.getByRole('dialog', { name: 'Crear producto' });
  await dialogo.getByLabel(/^Código/).fill(codigo);
  await dialogo.getByLabel(/^Nombre/).fill(nombre);
  await dialogo.getByLabel(/^Categoría/).fill(categoria);
  if (descripcion) await dialogo.getByLabel('Descripción').fill(descripcion);
  if (unidad) await elegir(dialogo, /Unidad/, new RegExp(`^${unidad} ·`));
  if (minimo !== undefined) await dialogo.getByLabel(/Stock mínimo/).fill(String(minimo));
  if (inicial !== undefined) await dialogo.getByLabel(/Stock inicial/).fill(String(inicial));
  await dialogo.getByRole('button', { name: 'Guardar' }).click();
  return { codigo, nombre };
}

/** Busca un producto por texto en la pantalla de productos y devuelve su fila. */
export async function filaProducto(page, texto) {
  await page.getByLabel('Buscar por código, nombre o categoría').fill(texto);
  const fila = page.getByRole('row', { name: new RegExp(texto) });
  await expect(fila).toBeVisible();
  return fila;
}

/** Registra un movimiento desde la fila del producto (entrada, salida o ajuste). */
export async function registrarMovimiento(page, nombre, { tipo, cantidad, motivo, nota, referencia }) {
  await page.getByRole('button', { name: `Registrar movimiento: ${nombre}` }).click();
  const dialogo = page.getByRole('dialog', { name: 'Registrar movimiento' });
  const nombreTipo = { ENTRADA: 'Entrada', SALIDA: 'Salida', AJUSTE: 'Ajuste' }[tipo];
  await dialogo.getByRole('button', { name: nombreTipo, exact: true }).click();
  await dialogo.getByLabel(tipo === 'AJUSTE' ? /Stock contado/ : /^Cantidad/).fill(String(cantidad));
  await elegir(dialogo, /Motivo/, motivo);
  if (referencia) await dialogo.getByLabel(/Referencia/).fill(referencia);
  if (nota) await dialogo.getByLabel(/^Nota/).fill(nota);
  await dialogo.getByRole('button', { name: 'Guardar' }).click();
  return dialogo;
}

/** Crea un perfil desde la pantalla de perfiles con los permisos (textos visibles) indicados. */
export async function crearPerfil(page, nombre, permisos) {
  await irA(page, 'Perfiles');
  await page.getByRole('button', { name: 'Nuevo perfil' }).click();
  const dialogo = page.getByRole('dialog', { name: 'Crear perfil' });
  await dialogo.getByLabel(/Nombre del perfil/).fill(nombre);
  for (const p of permisos) await dialogo.getByLabel(p, { exact: true }).check();
  await dialogo.getByRole('button', { name: 'Guardar' }).click();
  await expect(page.getByText('Perfil creado')).toBeVisible();
}

/** Crea un usuario desde la pantalla de usuarios. */
export async function crearUsuario(page, { username, nombre, clave = CLAVE, perfil }) {
  await irA(page, 'Usuarios');
  await page.getByRole('button', { name: 'Nuevo usuario' }).click();
  const dialogo = page.getByRole('dialog', { name: 'Crear usuario' });
  await dialogo.getByLabel(/Nombre de usuario/).fill(username);
  await dialogo.getByLabel(/^Contraseña/).fill(clave);
  await dialogo.getByLabel(/Nombre completo/).fill(nombre);
  await elegir(dialogo, /Perfil/, perfil);
  await dialogo.getByRole('button', { name: 'Guardar' }).click();
  await expect(page.getByText('Usuario creado')).toBeVisible();
}

/** Cierra la sesión actual. */
export async function salir(page) {
  await page.getByRole('button', { name: 'Salir' }).click();
  await expect(page.getByRole('heading', { name: 'Ingreso al inventario' })).toBeVisible();
}
