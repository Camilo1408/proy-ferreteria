/**
 * nombre: flujos.test.mjs
 * descripcion: Pruebas Selenium WebDriver de los flujos críticos: login, producto, entrada de stock y permisos.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import assert from 'node:assert/strict';
import { Builder, By, until } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome.js';

const BASE = process.env.E2E_BASE_URL ?? 'http://localhost:5173';
const ADMIN = { u: 'admin', p: process.env.E2E_ADMIN_PASSWORD ?? 'Admin123*' };
const USER = { u: 'user', p: process.env.E2E_USER_PASSWORD ?? 'User123*' };
const opts = new chrome.Options().addArguments('--headless=new', '--window-size=1366,900', '--no-sandbox');
const ESPERA = 15000;

async function conductor() {
  return new Builder().forBrowser('chrome').setChromeOptions(opts).build();
}

const xp = (d, ruta) => d.wait(until.elementLocated(By.xpath(ruta)), ESPERA);
const id = (d, i) => d.wait(until.elementLocated(By.id(i)), ESPERA);

async function entrar(d, c) {
  await d.get(BASE);
  await (await id(d, 'username')).sendKeys(c.u);
  await (await id(d, 'password')).sendKeys(c.p);
  await d.findElement(By.css('button[type=submit]')).click();
  await xp(d, "//nav[@aria-label='Navegación principal']");
}

async function irA(d, seccion) {
  await (await xp(d, `//nav[@aria-label='Navegación principal']//a[normalize-space()='${seccion}']`)).click();
}

async function elegirOpcion(d, campo, texto) {
  await (await id(d, campo)).click();
  await (await xp(d, `//li[@role='option' and normalize-space()='${texto}']`)).click();
}

const resultados = [];
async function caso(nombre, fn) {
  const d = await conductor();
  try {
    await fn(d);
    resultados.push(['OK', nombre]);
  } catch (e) {
    resultados.push(['FALLA', `${nombre}: ${e.message}`]);
  } finally {
    await d.quit();
  }
}

await caso('SEL-01 CA-08 login incorrecto muestra error', async (d) => {
  await d.get(BASE);
  await (await id(d, 'username')).sendKeys('admin');
  await (await id(d, 'password')).sendKeys('mala');
  await d.findElement(By.css('button[type=submit]')).click();
  const alerta = await d.wait(until.elementLocated(By.css('.MuiAlert-message')), ESPERA);
  assert.match(await alerta.getText(), /incorrectos/);
});

await caso('SEL-02 CA-01 ADMIN crea un producto con stock inicial y aparece con sus existencias', async (d) => {
  await entrar(d, ADMIN);
  await irA(d, 'Productos');
  const sufijo = Date.now().toString(36).toUpperCase();
  const nombre = `Selenium ${sufijo}`;
  await (await xp(d, "//button[contains(.,'Nuevo producto')]")).click();
  await (await id(d, 'codigo')).sendKeys(`SEL-${sufijo}`);
  await d.findElement(By.id('nombre')).sendKeys(nombre);
  await d.findElement(By.id('categoria')).sendKeys('Pruebas');
  await d.findElement(By.id('stockMinimo')).sendKeys('3');
  await d.findElement(By.id('stockInicial')).sendKeys('8');
  await d.findElement(By.xpath("//button[normalize-space()='Guardar']")).click();
  await xp(d, "//*[contains(text(),'Producto creado')]");
  await d.findElement(By.css('input[type=search]')).sendKeys(nombre);
  const fila = await xp(d, `//tr[.//td[normalize-space()='${nombre}']]`);
  assert.match(await fila.getText(), /8 UND/);
});

await caso('SEL-03 CA-05 USER (consulta) no ve «Nuevo producto» ni las secciones de administración', async (d) => {
  await entrar(d, USER);
  await irA(d, 'Productos');
  await xp(d, "//h1[normalize-space()='Productos']");
  assert.equal((await d.findElements(By.xpath("//button[contains(.,'Nuevo producto')]"))).length, 0);
  assert.equal((await d.findElements(By.xpath("//nav//a[normalize-space()='Usuarios' or normalize-space()='Perfiles']"))).length, 0);
});

await caso('SEL-04 CA-20 y CA-31 una entrada y una salida actualizan el stock y la salida al mínimo genera alerta', async (d) => {
  await entrar(d, ADMIN);
  await irA(d, 'Productos');
  const sufijo = Date.now().toString(36).toUpperCase();
  const nombre = `Selenium Mov ${sufijo}`;
  await (await xp(d, "//button[contains(.,'Nuevo producto')]")).click();
  await (await id(d, 'codigo')).sendKeys(`SM-${sufijo}`);
  await d.findElement(By.id('nombre')).sendKeys(nombre);
  await d.findElement(By.id('categoria')).sendKeys('Pruebas');
  await d.findElement(By.id('stockMinimo')).sendKeys('5');
  await d.findElement(By.id('stockInicial')).sendKeys('6');
  await d.findElement(By.xpath("//button[normalize-space()='Guardar']")).click();
  await xp(d, "//*[contains(text(),'Producto creado')]");
  await d.findElement(By.css('input[type=search]')).sendKeys(nombre);

  // Entrada de 4 -> stock 10.
  await (await xp(d, `//button[@aria-label='Registrar movimiento: ${nombre}']`)).click();
  await (await xp(d, "//div[@role='dialog']//button[normalize-space()='Entrada']")).click();
  await (await id(d, 'cantidad')).sendKeys('4');
  await elegirOpcion(d, 'motivo', 'Compra');
  await d.findElement(By.xpath("//div[@role='dialog']//button[normalize-space()='Guardar']")).click();
  await xp(d, "//*[contains(text(),'Movimiento registrado')]");
  await d.wait(async () => /10 UND/.test(await (await xp(d, `//tr[.//td[normalize-space()='${nombre}']]`)).getText()), ESPERA);

  // Salida de 6 -> stock 4 (bajo el mínimo de 5): la fila pasa a «Bajo».
  await d.wait(async () => (await d.findElements(By.css('[role=dialog]'))).length === 0, ESPERA);
  await (await xp(d, `//button[@aria-label='Registrar movimiento: ${nombre}']`)).click();
  await (await xp(d, "//div[@role='dialog']//button[normalize-space()='Salida']")).click();
  await (await id(d, 'cantidad')).sendKeys('6');
  await elegirOpcion(d, 'motivo', 'Venta');
  await d.findElement(By.xpath("//div[@role='dialog']//button[normalize-space()='Guardar']")).click();
  await d.wait(async () => /4 UND/.test(await (await xp(d, `//tr[.//td[normalize-space()='${nombre}']]`)).getText()), ESPERA);
  assert.match(await (await xp(d, `//tr[.//td[normalize-space()='${nombre}']]`)).getText(), /Bajo/);

  // La alerta aparece en la pantalla de alertas.
  await irA(d, 'Alertas');
  await xp(d, `//tr[.//*[contains(text(),'${nombre}')]]`);
});

await caso('SEL-05 CA-42 el administrador ve Usuarios y Perfiles y puede abrir el formulario de perfil con permisos por módulo', async (d) => {
  await entrar(d, ADMIN);
  await irA(d, 'Perfiles');
  await (await xp(d, "//button[contains(.,'Nuevo perfil')]")).click();
  await xp(d, "//div[@role='dialog']//legend[normalize-space()='Inventario']");
  const permisos = await d.findElements(By.css("input[data-permiso]"));
  assert.equal(permisos.length, 10);
  await d.findElement(By.xpath("//div[@role='dialog']//button[normalize-space()='Cancelar']")).click();
  await d.wait(async () => (await d.findElements(By.css('[role=dialog]'))).length === 0, ESPERA);
  await irA(d, 'Usuarios');
  await xp(d, "//h1[normalize-space()='Usuarios']");
});

for (const [estado, texto] of resultados) console.log(`${estado}  ${texto}`);
process.exit(resultados.some(([e]) => e !== 'OK') ? 1 : 0);
