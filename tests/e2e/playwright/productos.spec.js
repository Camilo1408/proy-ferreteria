/**
 * nombre: productos.spec.js
 * descripcion: Aceptación E2E del catálogo de productos con existencias (CA-01 a CA-04, CA-09 a CA-11, CA-16 a CA-19, CA-38, CA-41).
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { expect, test } from '@playwright/test';
import { codigoUnico, crearProducto, elegir, entrar, filaProducto, irA, unico } from './helpers';

test.describe('Productos (administrador)', () => {
  test.beforeEach(async ({ page }) => entrar(page));

  test('CA-01 y CA-19 crear con unidad, mínimo y stock inicial lo muestra con sus existencias y un movimiento', async ({ page }) => {
    const { codigo, nombre } = await crearProducto(page, { unidad: 'M', minimo: '5', inicial: '12,5', descripcion: 'Cable de cobre' });
    await expect(page.getByText('Producto creado')).toBeVisible();
    const fila = await filaProducto(page, nombre);
    await expect(fila).toContainText(codigo);
    await expect(fila).toContainText('12,5 M');
    await expect(fila).toContainText('Activo');
    await fila.getByRole('button', { name: `Ver historial: ${nombre}` }).click();
    await expect(page.getByRole('heading', { name: 'Movimientos' })).toBeVisible();
    const mov = page.getByRole('row', { name: /Stock inicial/ });
    await expect(mov).toContainText('Entrada');
    await expect(mov).toContainText('+12,5');
    await expect(mov).toContainText('0 → 12,5');
    await expect(mov).toContainText('admin');
  });

  test('CA-02, CA-16 y CA-17 validaciones del formulario: código, nombre, categoría y cantidades', async ({ page }) => {
    await irA(page, 'Productos');
    await page.getByRole('button', { name: 'Nuevo producto' }).click();
    const d = page.getByRole('dialog', { name: 'Crear producto' });
    await d.getByLabel(/^Código/).fill('a b');
    await d.getByLabel(/^Nombre/).fill('ab');
    await d.getByLabel(/Stock mínimo/).fill('1,2345');
    await d.getByLabel(/Stock inicial/).fill('-3');
    await d.getByRole('button', { name: 'Guardar' }).click();
    await expect(d.getByText('De 2 a 30 caracteres: letras, números, punto, guion y guion bajo')).toBeVisible();
    await expect(d.getByText('El nombre debe tener entre 3 y 100 caracteres')).toBeVisible();
    await expect(d.getByText('La categoría es obligatoria')).toBeVisible();
    await expect(d.getByText('Use un número no negativo con hasta 3 decimales')).toHaveCount(2);
    await expect(d).toBeVisible();
  });

  test('CA-03 y CA-16 nombre o código duplicados muestran el error de la API', async ({ page }) => {
    const { codigo, nombre } = await crearProducto(page);
    await expect(page.getByText('Producto creado')).toBeVisible();
    await crearProducto(page, { nombre: nombre.toUpperCase() });
    await expect(page.getByRole('alert').filter({ hasText: 'Ya existe un producto con el nombre' })).toBeVisible();
    await page.getByRole('button', { name: 'Cancelar' }).click();
    await crearProducto(page, { codigo: codigo.toLowerCase() });
    await expect(page.getByRole('alert').filter({ hasText: 'Ya existe un producto con el código' })).toBeVisible();
  });

  test('CA-10 y CA-18 editar cambia los datos y no toca el stock', async ({ page }) => {
    const { nombre } = await crearProducto(page, { inicial: '7', minimo: '2' });
    await expect(page.getByText('Producto creado')).toBeVisible();
    await (await filaProducto(page, nombre)).getByRole('button', { name: `Editar producto: ${nombre}` }).click();
    const d = page.getByRole('dialog', { name: 'Editar producto' });
    await expect(d.getByLabel(/Stock inicial/)).toHaveCount(0);
    const nuevo = `${nombre} Pro`;
    await d.getByLabel(/^Nombre/).fill(nuevo);
    await d.getByLabel(/Stock mínimo/).fill('3');
    await d.getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByText('Producto actualizado')).toBeVisible();
    const fila = await filaProducto(page, nuevo);
    await expect(fila).toContainText('7 UND');
  });

  test('CA-04 desactivar pide confirmación y deja el producto Inactivo sin permitir movimientos', async ({ page }) => {
    const { nombre } = await crearProducto(page, { inicial: '3' });
    await expect(page.getByText('Producto creado')).toBeVisible();
    await (await filaProducto(page, nombre)).getByRole('button', { name: `Desactivar producto: ${nombre}` }).click();
    await expect(page.getByRole('dialog', { name: 'Desactivar producto' })).toBeVisible();
    await page.getByRole('dialog').getByRole('button', { name: 'Desactivar', exact: true }).click();
    await expect(page.getByText('Producto desactivado')).toBeVisible();
    const fila = page.getByRole('row', { name: new RegExp(nombre) });
    await expect(fila).toContainText('Inactivo');
    await expect(fila.getByRole('button', { name: `Registrar movimiento: ${nombre}` })).toHaveCount(0);
  });

  test('CA-11 filtros: por estado, por texto y por «Solo en mínimos» (CA-38)', async ({ page }) => {
    const bajo = await crearProducto(page, { minimo: '5', inicial: '2' });
    await expect(page.getByText('Producto creado')).toBeVisible();
    const sano = await crearProducto(page, { minimo: '5', inicial: '50' });
    await expect(page.getByText('Producto creado').last()).toBeVisible();
    await page.getByLabel('Buscar por código, nombre o categoría').fill('');
    await page.getByRole('checkbox', { name: 'Solo en mínimos' }).check();
    await page.getByLabel('Buscar por código, nombre o categoría').fill(bajo.nombre);
    const fila = page.getByRole('row', { name: new RegExp(bajo.nombre) });
    await expect(fila).toBeVisible();
    await expect(fila).toContainText('Bajo');
    await page.getByLabel('Buscar por código, nombre o categoría').fill(sano.nombre);
    await expect(page.getByText('No hay productos que coincidan.')).toBeVisible();
    await page.getByRole('checkbox', { name: 'Solo en mínimos' }).uncheck();
    await expect(page.getByRole('row', { name: new RegExp(sano.nombre) })).toBeVisible();
    await page.getByLabel('Buscar por código, nombre o categoría').fill('');
    await elegir(page, 'Estado', 'Inactivo');
    for (const f of await page.getByRole('row').filter({ hasNot: page.getByRole('columnheader') }).all()) {
      await expect(f).toContainText('Inactivo');
    }
  });

  test('CA-41 exportar el inventario descarga un CSV con los productos', async ({ page }) => {
    const { codigo, nombre } = await crearProducto(page, { inicial: '4' });
    await expect(page.getByText('Producto creado')).toBeVisible();
    const [descarga] = await Promise.all([page.waitForEvent('download'), page.getByRole('button', { name: 'Exportar CSV' }).click()]);
    expect(descarga.suggestedFilename()).toBe('inventario.csv');
    const ruta = await descarga.path();
    const csv = (await import('node:fs')).readFileSync(ruta, 'utf8');
    expect(csv).toContain('Código,Nombre,Categoría,Unidad,Stock actual,Stock mínimo,Nivel,Estado');
    expect(csv).toContain(codigo);
    expect(csv).toContain(nombre);
  });

  test('CA-09 el código y la unidad se envían en mayúsculas y minúsculas tal como se escriben', async ({ page }) => {
    const codigo = codigoUnico('mx').toLowerCase();
    const nombre = unico('Minúscula');
    await crearProducto(page, { codigo, nombre });
    await expect(page.getByText('Producto creado')).toBeVisible();
    await expect(await filaProducto(page, nombre)).toContainText(codigo);
  });
});
