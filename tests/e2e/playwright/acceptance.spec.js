/**
 * nombre: acceptance.spec.js
 * descripcion: Pruebas de aceptación E2E con Playwright, trazadas a los criterios CA-xx.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import { expect, test } from '@playwright/test';
import { ADMIN, USER, crearProducto, entrar, unico } from './helpers';

test.describe('Autenticación', () => {
  test('CA-08 credenciales incorrectas muestran error y no dan acceso', async ({ page }) => {
    await page.goto('/');
    await page.getByLabel('Usuario').fill('admin');
    await page.getByLabel('Contraseña').fill('mala');
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page.getByRole('alert')).toContainText('Usuario o contraseña incorrectos');
    await expect(page.getByRole('heading', { name: 'Productos' })).toHaveCount(0);
  });

  test('CA-08 campos vacíos se validan en el formulario', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page.getByText('El usuario es obligatorio')).toBeVisible();
    await expect(page.getByText('La contraseña es obligatoria')).toBeVisible();
  });

  test('salir devuelve al login', async ({ page }) => {
    await entrar(page);
    await page.getByRole('button', { name: 'Salir' }).click();
    await expect(page.getByRole('heading', { name: 'Ingreso al inventario' })).toBeVisible();
  });
});

test.describe('CRUD como ADMIN', () => {
  test.beforeEach(async ({ page }) => entrar(page, ADMIN));

  test('CA-01 crear un producto válido lo muestra ACTIVO en el listado', async ({ page }) => {
    const nombre = unico('Martillo');
    await crearProducto(page, nombre, 'Herramientas', 'Cabeza de acero');
    await expect(page.getByText('Producto creado')).toBeVisible();
    await page.getByLabel('Buscar por nombre o categoría').fill(nombre);
    const fila = page.getByRole('row', { name: new RegExp(nombre) });
    await expect(fila).toBeVisible();
    await expect(fila).toContainText('Activo');
  });

  test('CA-02 nombre corto y categoría vacía muestran errores y no envían', async ({ page }) => {
    await page.getByRole('button', { name: 'Nuevo producto' }).click();
    await page.getByLabel(/Nombre/).fill('ab');
    await page.getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByText('El nombre debe tener entre 3 y 100 caracteres')).toBeVisible();
    await expect(page.getByText('La categoría es obligatoria')).toBeVisible();
    await expect(page.getByRole('dialog')).toBeVisible();
  });

  test('CA-03 nombre duplicado muestra el error de la API', async ({ page }) => {
    const nombre = unico('Taladro');
    await crearProducto(page, nombre);
    await expect(page.getByText('Producto creado')).toBeVisible();
    await crearProducto(page, nombre.toUpperCase());
    await expect(page.getByRole('alert').filter({ hasText: 'Ya existe un producto' })).toBeVisible();
  });

  test('CA-10 editar cambia el nombre en el listado', async ({ page }) => {
    const nombre = unico('Alicate');
    await crearProducto(page, nombre);
    await page.getByLabel('Buscar por nombre o categoría').fill(nombre);
    await page.getByRole('button', { name: new RegExp(`Editar producto: ${nombre}`) }).click();
    const nuevo = `${nombre} Pro`;
    await page.getByLabel(/Nombre/).fill(nuevo);
    await page.getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByText('Producto actualizado')).toBeVisible();
    await page.getByLabel('Buscar por nombre o categoría').fill(nuevo);
    await expect(page.getByRole('row', { name: new RegExp(nuevo) })).toBeVisible();
  });

  test('CA-04 desactivar pide confirmación y deja el producto Inactivo', async ({ page }) => {
    const nombre = unico('Sierra');
    await crearProducto(page, nombre);
    await page.getByLabel('Buscar por nombre o categoría').fill(nombre);
    await page.getByRole('button', { name: new RegExp(`Desactivar producto: ${nombre}`) }).click();
    await expect(page.getByRole('dialog', { name: 'Desactivar producto' })).toBeVisible();
    await page.getByRole('button', { name: 'Desactivar', exact: true }).click();
    await expect(page.getByText('Producto desactivado')).toBeVisible();
    await expect(page.getByRole('row', { name: new RegExp(nombre) })).toContainText('Inactivo');
  });

  test('CA-11 el filtro por estado muestra solo inactivos', async ({ page }) => {
    await page.getByLabel('Estado').click();
    await page.getByRole('option', { name: 'Inactivo' }).click();
    const filas = page.getByRole('row').filter({ hasNot: page.getByRole('columnheader') });
    for (const fila of await filas.all()) await expect(fila).toContainText('Inactivo');
  });
});

test.describe('Permisos y gestos', () => {
  test('CA-05 USER ve el listado pero no tiene controles de escritura', async ({ page }) => {
    await entrar(page, USER);
    await expect(page.getByRole('button', { name: 'Nuevo producto' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: /Editar producto/ })).toHaveCount(0);
    await expect(page.getByRole('button', { name: /Desactivar producto/ })).toHaveCount(0);
  });

  test('CA-06 un token inválido en sesión devuelve al login', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => sessionStorage.setItem('sesion', JSON.stringify({ token: 'x', username: 'a', rol: 'ADMIN' })));
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

  test('tema oscuro se activa y persiste al recargar', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Cambiar tema' }).click();
    await page.reload();
    const fondo = await page.evaluate(() => getComputedStyle(document.body).backgroundColor);
    expect(fondo).toBe('rgb(20, 23, 26)');
  });
});
