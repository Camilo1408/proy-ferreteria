/**
 * nombre: helpers.js
 * descripcion: Utilidades compartidas de las pruebas E2E (login y datos únicos).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
export const ADMIN = { u: 'admin', p: process.env.E2E_ADMIN_PASSWORD ?? 'Admin123*' };
export const USER = { u: 'user', p: process.env.E2E_USER_PASSWORD ?? 'User123*' };

/** Inicia sesión por la interfaz. */
export async function entrar(page, cred = ADMIN) {
  await page.goto('/');
  await page.getByLabel('Usuario').fill(cred.u);
  await page.getByLabel('Contraseña').fill(cred.p);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await page.getByRole('heading', { name: 'Productos' }).waitFor();
}

/** Nombre único por ejecución para no chocar con datos previos. */
export const unico = (base) => `${base} ${Date.now().toString(36)}${Math.floor(Math.random() * 99)}`;

/** Crea un producto desde la interfaz. */
export async function crearProducto(page, nombre, categoria = 'Herramientas', descripcion = '') {
  await page.getByRole('button', { name: 'Nuevo producto' }).click();
  await page.getByLabel(/Nombre/).fill(nombre);
  await page.getByLabel(/Categoría/).fill(categoria);
  if (descripcion) await page.getByLabel('Descripción').fill(descripcion);
  await page.getByRole('button', { name: 'Guardar' }).click();
}
