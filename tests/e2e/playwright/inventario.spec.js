/**
 * nombre: inventario.spec.js
 * descripcion: Aceptación E2E de entradas, salidas, ajustes, historial y alertas de mínimos (CA-20 a CA-39).
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { expect, test } from '@playwright/test';
import { crearProducto, entrar, filaProducto, irA, registrarMovimiento } from './helpers';

const campana = (page, n) => page.getByRole('button', { name: `Alertas pendientes: ${n}` });

test.describe('Movimientos y alertas', () => {
  test.beforeEach(async ({ page }) => entrar(page));

  test('CA-20 una entrada suma al stock y queda en el historial con referencia', async ({ page }) => {
    const { nombre } = await crearProducto(page, { inicial: '5' });
    await expect(page.getByText('Producto creado')).toBeVisible();
    await filaProducto(page, nombre);
    await registrarMovimiento(page, nombre, { tipo: 'ENTRADA', cantidad: '10', motivo: 'Compra', referencia: 'FAC-100', nota: 'Pedido semanal' });
    await expect(page.getByText('Movimiento registrado')).toBeVisible();
    await expect(page.getByRole('row', { name: new RegExp(nombre) })).toContainText('15 UND');
    await page.getByRole('button', { name: `Ver historial: ${nombre}` }).click();
    const fila = page.getByRole('row', { name: /Compra/ });
    await expect(fila).toContainText('+10');
    await expect(fila).toContainText('5 → 15');
    await expect(fila).toContainText('FAC-100 · Pedido semanal');
  });

  test('CA-23 una salida mayor al stock muestra el error y no cambia nada; igual al stock deja 0 (CA-24)', async ({ page }) => {
    const { nombre } = await crearProducto(page, { inicial: '4' });
    await expect(page.getByText('Producto creado')).toBeVisible();
    await filaProducto(page, nombre);
    const d = await registrarMovimiento(page, nombre, { tipo: 'SALIDA', cantidad: '4,001', motivo: 'Venta' });
    await expect(d.getByRole('alert')).toContainText('Stock insuficiente: hay 4 disponibles');
    await d.getByLabel(/^Cantidad/).fill('4');
    await d.getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByText('Movimiento registrado')).toBeVisible();
    await expect(page.getByRole('row', { name: new RegExp(nombre) })).toContainText('0 UND');
  });

  test('CA-21 la cantidad se valida en el formulario antes de enviar', async ({ page }) => {
    const { nombre } = await crearProducto(page, { inicial: '4' });
    await expect(page.getByText('Producto creado')).toBeVisible();
    await filaProducto(page, nombre);
    const d = await registrarMovimiento(page, nombre, { tipo: 'ENTRADA', cantidad: '0', motivo: 'Compra' });
    await expect(d.getByText('La cantidad debe ser mayor que cero')).toBeVisible();
    await d.getByLabel(/^Cantidad/).fill('1,2345');
    await d.getByRole('button', { name: 'Guardar' }).click();
    await expect(d.getByText('Use un número no negativo con hasta 3 decimales')).toBeVisible();
  });

  test('CA-26 y CA-27 el ajuste lleva el stock al valor contado y exige nota', async ({ page }) => {
    const { nombre } = await crearProducto(page, { inicial: '10' });
    await expect(page.getByText('Producto creado')).toBeVisible();
    await filaProducto(page, nombre);
    const d = await registrarMovimiento(page, nombre, { tipo: 'AJUSTE', cantidad: '8', motivo: 'Conteo físico', nota: 'abc' });
    await expect(d.getByText('La nota es obligatoria (mínimo 5 caracteres)')).toBeVisible();
    await d.getByLabel(/^Nota/).fill('Conteo del mes');
    await d.getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByText('Movimiento registrado')).toBeVisible();
    await expect(page.getByRole('row', { name: new RegExp(nombre) })).toContainText('8 UND');
    await page.getByRole('button', { name: `Ver historial: ${nombre}` }).click();
    const fila = page.getByRole('row', { name: /Conteo físico/ });
    await expect(fila).toContainText('−2');
    await expect(fila).toContainText('10 → 8');
  });

  test('CA-29 el historial se filtra por tipo', async ({ page }) => {
    const { nombre } = await crearProducto(page, { inicial: '10' });
    await expect(page.getByText('Producto creado')).toBeVisible();
    await filaProducto(page, nombre);
    await registrarMovimiento(page, nombre, { tipo: 'SALIDA', cantidad: '2', motivo: 'Merma' });
    await expect(page.getByText('Movimiento registrado')).toBeVisible();
    await page.getByRole('button', { name: `Ver historial: ${nombre}` }).click();
    await expect(page.getByRole('row').filter({ hasText: /Stock inicial|Merma/ })).toHaveCount(2);
    await page.getByRole('combobox', { name: 'Tipo' }).click();
    await page.getByRole('option', { name: 'Salida', exact: true }).click();
    await expect(page.getByRole('row').filter({ hasText: /Stock inicial|Merma/ })).toHaveCount(1);
    await expect(page.getByRole('row', { name: /Merma/ })).toBeVisible();
  });

  test('CA-31 a CA-37 ciclo completo de una alerta: aparece, se reconoce, empeora, se repone y se resuelve', async ({ page }) => {
    const { nombre } = await crearProducto(page, { inicial: '6', minimo: '5' });
    await expect(page.getByText('Producto creado')).toBeVisible();
    const antes = await page.getByRole('button', { name: /^Alertas pendientes: \d+$|^Sin alertas pendientes$/ }).getAttribute('aria-label');
    const n0 = Number((antes.match(/\d+/) ?? ['0'])[0]);

    await filaProducto(page, nombre);
    await registrarMovimiento(page, nombre, { tipo: 'SALIDA', cantidad: '1', motivo: 'Venta' });
    await expect(page.getByText('Movimiento registrado')).toBeVisible();
    await expect(campana(page, n0 + 1)).toBeVisible();
    await expect(page.getByRole('row', { name: new RegExp(nombre) })).toContainText('Bajo');

    await irA(page, 'Alertas');
    const fila = page.getByRole('row', { name: new RegExp(nombre) });
    await expect(fila).toContainText('Bajo');
    await expect(fila).toContainText('Pendiente');
    await fila.getByRole('button', { name: `Reconocer: ${nombre}` }).click();
    await expect(page.getByText('Alerta reconocida')).toBeVisible();
    await expect(fila).toContainText('Reconocida');
    await expect(fila).toContainText('por admin');
    await expect(campana(page, n0)).toHaveCount(n0 === 0 ? 0 : 1);

    await irA(page, 'Productos');
    await filaProducto(page, nombre);
    await registrarMovimiento(page, nombre, { tipo: 'SALIDA', cantidad: '5', motivo: 'Venta' });
    await expect(page.getByText('Movimiento registrado')).toBeVisible();
    await expect(page.getByRole('row', { name: new RegExp(nombre) })).toContainText('Agotado');
    await irA(page, 'Alertas');
    await expect(page.getByRole('row', { name: new RegExp(nombre) })).toContainText('Agotado');
    await expect(page.getByRole('row', { name: new RegExp(nombre) })).toContainText('Pendiente');

    await page.getByRole('button', { name: `Reponer: ${nombre}` }).click();
    const d = page.getByRole('dialog', { name: 'Registrar movimiento' });
    await d.getByLabel(/^Cantidad/).fill('20');
    await d.getByRole('combobox', { name: /Motivo/ }).click();
    await page.getByRole('option', { name: 'Compra', exact: true }).click();
    await d.getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByText('Movimiento registrado')).toBeVisible();
    await expect(page.getByRole('row', { name: new RegExp(nombre) })).toHaveCount(0);

    await page.getByRole('tab', { name: 'Resueltas' }).click();
    await expect(page.getByRole('row', { name: new RegExp(nombre) })).toContainText('Resuelta');
  });

  test('CA-39 y CA-40 el panel muestra los indicadores y los productos en mínimos con reposición', async ({ page }) => {
    const { nombre } = await crearProducto(page, { inicial: '1', minimo: '10' });
    await expect(page.getByText('Producto creado')).toBeVisible();
    await irA(page, 'Panel');
    await expect(page.getByText('Productos activos')).toBeVisible();
    await expect(page.getByText('Alertas pendientes').first()).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Productos en mínimos' })).toBeVisible();
    await page.getByRole('link', { name: /En mínimos/ }).click();
    await expect(page.getByRole('checkbox', { name: 'Solo en mínimos' })).toBeChecked();
    await page.getByLabel('Buscar por código, nombre o categoría').fill(nombre);
    await expect(page.getByRole('row', { name: new RegExp(nombre) })).toContainText('Bajo');
  });
});
