/**
 * nombre: responsive.spec.js
 * descripcion: Pruebas de diseño responsive y accesibilidad básica (móvil y escritorio).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { expect, test } from '@playwright/test';
import { crearProducto, entrar, irA } from './helpers';

test('CA-13 en móvil las listas usan tarjetas y ninguna pantalla desborda horizontalmente', async ({ page, isMobile }) => {
  test.skip(!isMobile, 'solo aplica al proyecto móvil');
  await entrar(page);
  await crearProducto(page, { inicial: '3', minimo: '5' });
  await expect(page.getByText('Producto creado')).toBeVisible();
  await expect(page.getByRole('table')).toHaveCount(0);
  for (const seccion of ['Panel', 'Productos', 'Movimientos', 'Alertas', 'Usuarios', 'Perfiles', 'Mi cuenta']) {
    await irA(page, seccion);
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    const desborda = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1);
    expect(desborda, `desborde horizontal en ${seccion}`).toBe(false);
  }
});

test('CA-13 el enlace de salto y los controles principales tienen nombre accesible', async ({ page }) => {
  await page.goto('/');
  await page.keyboard.press('Tab');
  await expect(page.getByRole('link', { name: 'Saltar al contenido' })).toBeFocused();
  await expect(page.getByRole('button', { name: 'Cambiar tema' })).toBeVisible();
  await expect(page.getByLabel('Usuario')).toBeVisible();
});

test('CA-13 la campana de alertas y las acciones de la tabla tienen etiquetas accesibles', async ({ page, isMobile }) => {
  test.skip(isMobile, 'se valida en escritorio');
  await entrar(page);
  await expect(page.getByRole('button', { name: /Alertas pendientes: \d+|Sin alertas pendientes/ })).toBeVisible();
  const { nombre } = await crearProducto(page, { inicial: '2' });
  await expect(page.getByText('Producto creado')).toBeVisible();
  await page.getByLabel('Buscar por código, nombre o categoría').fill(nombre);
  for (const accion of ['Registrar movimiento', 'Ver historial', 'Editar producto', 'Desactivar producto']) {
    await expect(page.getByRole('button', { name: `${accion}: ${nombre}` })).toBeVisible();
  }
});
