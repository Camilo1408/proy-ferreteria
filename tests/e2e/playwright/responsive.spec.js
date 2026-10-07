/**
 * nombre: responsive.spec.js
 * descripcion: Pruebas de diseño responsive y accesibilidad básica (móvil y escritorio).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import { expect, test } from '@playwright/test';
import { entrar } from './helpers';

test('en móvil el listado usa tarjetas y no desborda horizontalmente', async ({ page, isMobile }) => {
  test.skip(!isMobile, 'solo aplica al proyecto móvil');
  await entrar(page);
  await expect(page.getByRole('table')).toHaveCount(0);
  const desborda = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1);
  expect(desborda).toBe(false);
});

test('el enlace de salto y los controles tienen nombre accesible', async ({ page }) => {
  await page.goto('/');
  await page.keyboard.press('Tab');
  await expect(page.getByRole('link', { name: 'Saltar al contenido' })).toBeFocused();
  await expect(page.getByRole('button', { name: 'Cambiar tema' })).toBeVisible();
  await expect(page.getByLabel('Usuario')).toBeVisible();
});
