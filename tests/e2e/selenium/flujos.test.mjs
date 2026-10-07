/**
 * nombre: flujos.test.mjs
 * descripcion: Pruebas Selenium WebDriver de los flujos críticos: login, creación y permisos.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import assert from 'node:assert/strict';
import { Builder, By, until } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome.js';

const BASE = process.env.E2E_BASE_URL ?? 'http://localhost:5173';
const ADMIN = { u: 'admin', p: process.env.E2E_ADMIN_PASSWORD ?? 'Admin123*' };
const USER = { u: 'user', p: process.env.E2E_USER_PASSWORD ?? 'User123*' };
const opts = new chrome.Options().addArguments('--headless=new', '--window-size=1280,900', '--no-sandbox');

async function conductor() {
  return new Builder().forBrowser('chrome').setChromeOptions(opts).build();
}

async function entrar(d, c) {
  await d.get(BASE);
  await d.wait(until.elementLocated(By.id('username')), 10000);
  await d.findElement(By.id('username')).sendKeys(c.u);
  await d.findElement(By.id('password')).sendKeys(c.p);
  await d.findElement(By.css('button[type=submit]')).click();
  await d.wait(until.elementLocated(By.xpath("//h1[normalize-space()='Productos']")), 10000);
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
  await d.wait(until.elementLocated(By.id('username')), 10000);
  await d.findElement(By.id('username')).sendKeys('admin');
  await d.findElement(By.id('password')).sendKeys('mala');
  await d.findElement(By.css('button[type=submit]')).click();
  const alerta = await d.wait(until.elementLocated(By.css('.MuiAlert-message')), 10000);
  assert.match(await alerta.getText(), /incorrectos/);
});

await caso('SEL-02 CA-01 ADMIN crea un producto y aparece en el listado', async (d) => {
  await entrar(d, ADMIN);
  const nombre = `Selenium ${Date.now().toString(36)}`;
  await d.findElement(By.xpath("//button[contains(.,'Nuevo producto')]")).click();
  await d.wait(until.elementLocated(By.id('nombre')), 5000);
  await d.findElement(By.id('nombre')).sendKeys(nombre);
  await d.findElement(By.id('categoria')).sendKeys('Pruebas');
  await d.findElement(By.xpath("//button[normalize-space()='Guardar']")).click();
  await d.wait(until.elementLocated(By.xpath(`//*[contains(text(),'Producto creado')]`)), 10000);
  await d.findElement(By.css('input[type=search]')).sendKeys(nombre);
  await d.wait(until.elementLocated(By.xpath(`//td[normalize-space()='${nombre}']`)), 10000);
});

await caso('SEL-03 CA-05 USER no ve el botón Nuevo producto', async (d) => {
  await entrar(d, USER);
  const botones = await d.findElements(By.xpath("//button[contains(.,'Nuevo producto')]"));
  assert.equal(botones.length, 0);
});

for (const [estado, texto] of resultados) console.log(`${estado}  ${texto}`);
process.exit(resultados.some(([e]) => e !== 'OK') ? 1 : 0);
