/**
 * nombre: sesion.spec.js
 * descripcion: Aceptación E2E de la sesión: login, idioma, tema, token inválido y permisos de solo lectura.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { expect, test } from '@playwright/test';
import { USER, entrar, irA, salir } from './helpers';

test.describe('Sesión', () => {
  test('CA-08 credenciales incorrectas muestran error y no dan acceso', async ({ page }) => {
    await page.goto('/');
    await page.getByLabel('Usuario').fill('admin');
    await page.getByLabel('Contraseña').fill('mala');
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page.getByRole('alert')).toContainText('Usuario o contraseña incorrectos');
    await expect(page.getByRole('navigation', { name: 'Navegación principal' })).toHaveCount(0);
  });

  test('CA-08 campos vacíos se validan en el formulario', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page.getByText('El usuario es obligatorio')).toBeVisible();
    await expect(page.getByText('La contraseña es obligatoria')).toBeVisible();
  });

  test('el administrador entra al panel con todas las secciones y puede salir', async ({ page }) => {
    await entrar(page);
    await expect(page.getByRole('heading', { name: 'Panel' })).toBeVisible();
    const nav = page.getByRole('navigation', { name: 'Navegación principal' });
    for (const s of ['Panel', 'Productos', 'Movimientos', 'Alertas', 'Usuarios', 'Perfiles', 'Mi cuenta']) {
      await expect(nav.getByRole('link', { name: s, exact: true })).toBeVisible();
    }
    await salir(page);
  });

  test('CA-05 y CA-53 el perfil de consulta no ve controles de escritura ni administración', async ({ page }) => {
    await entrar(page, USER);
    const nav = page.getByRole('navigation', { name: 'Navegación principal' });
    await expect(nav.getByRole('link', { name: 'Usuarios', exact: true })).toHaveCount(0);
    await expect(nav.getByRole('link', { name: 'Perfiles', exact: true })).toHaveCount(0);
    await irA(page, 'Productos');
    await expect(page.getByRole('heading', { name: 'Productos' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Nuevo producto' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: /Editar producto/ })).toHaveCount(0);
    await expect(page.getByRole('button', { name: /Desactivar producto/ })).toHaveCount(0);
    await expect(page.getByRole('button', { name: /Registrar movimiento/ })).toHaveCount(0);
    await irA(page, 'Movimientos');
    await expect(page.getByRole('button', { name: 'Registrar movimiento' })).toHaveCount(0);
  });

  test('CA-53 abrir por URL una sección sin permiso muestra «Sin acceso»', async ({ page }) => {
    await entrar(page, USER);
    await page.goto('/#/usuarios');
    await expect(page.getByRole('heading', { name: 'Sin acceso' })).toBeVisible();
    await page.goto('/#/perfiles');
    await expect(page.getByRole('heading', { name: 'Sin acceso' })).toBeVisible();
  });

  test('CA-06 un token inválido en sesión devuelve al login', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => sessionStorage.setItem('sesion', JSON.stringify({ token: 'x', usuario: { username: 'a', nombreCompleto: 'A', perfil: 'P', permisos: ['PRODUCTOS_VER'] } })));
    await page.reload();
    await expect(page.getByRole('heading', { name: 'Ingreso al inventario' })).toBeVisible();
  });

  test('CA-07 cambiar a inglés traduce la interfaz y los errores de la API', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'English' }).click();
    await expect(page.getByRole('heading', { name: 'Inventory sign in' })).toBeVisible();
    await page.getByLabel('Username').fill('admin');
    await page.getByLabel('Password').fill('mala');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page.getByRole('alert')).toContainText('Wrong username or password');
    await expect(page.locator('html')).toHaveAttribute('lang', 'en');
  });

  test('CA-07 la navegación y las pantallas se traducen al inglés tras iniciar sesión', async ({ page }) => {
    await entrar(page);
    await page.getByRole('button', { name: 'English' }).click();
    const nav = page.getByRole('navigation', { name: 'Main navigation' });
    await expect(nav.getByRole('link', { name: 'Dashboard', exact: true })).toBeVisible();
    await expect(nav.getByRole('link', { name: 'Profiles', exact: true })).toBeVisible();
    await nav.getByRole('link', { name: 'Products', exact: true }).click();
    await expect(page.getByRole('button', { name: 'New product' })).toBeVisible();
  });

  test('tema oscuro se activa y persiste al recargar', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Cambiar tema' }).click();
    await page.reload();
    const fondo = await page.evaluate(() => getComputedStyle(document.body).backgroundColor);
    expect(fondo).toBe('rgb(20, 23, 26)');
  });
});
