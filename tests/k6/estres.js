/**
 * nombre: estres.js
 * descripcion: Prueba de estrés con k6: rampa hasta 50 usuarios virtuales sobre el flujo de inventario completo
 *              (producto, entrada, salida, ajuste, historial, alertas, reporte). Umbrales: p95 < 800 ms y errores < 1 %.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.API_URL || 'http://localhost:8080';
const ADMIN_PASS = __ENV.ADMIN_PASSWORD || 'Admin123*';

export const options = {
  stages: [
    { duration: '15s', target: 10 },
    { duration: '30s', target: 50 },
    { duration: '15s', target: 50 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  const r = http.post(`${BASE}/api/v1/auth/login`, JSON.stringify({ username: 'admin', password: ADMIN_PASS }),
    { headers: { 'Content-Type': 'application/json' } });
  return { token: r.json('token') };
}

export default function (data) {
  const h = { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${data.token}` } };
  const post = (ruta, cuerpo) => http.post(`${BASE}${ruta}`, JSON.stringify(cuerpo), h);

  check(http.get(`${BASE}/api/v1/productos?size=20`, h), { 'listar productos 200': (r) => r.status === 200 });
  check(http.get(`${BASE}/api/v1/dashboard`, h), { 'panel 200': (r) => r.status === 200 });

  const sufijo = `${__VU}-${__ITER}-${Date.now()}`;
  const crea = post('/api/v1/productos', {
    codigo: `K6-${sufijo}`.slice(0, 30), nombre: `k6 ${sufijo}`, categoria: 'Carga', unidad: 'UND', stockMinimo: 5, stockInicial: 10,
  });
  check(crea, { 'crear producto 201': (r) => r.status === 201 });

  if (crea.status === 201) {
    const idProducto = crea.json('id');
    check(post('/api/v1/movimientos/entradas', { productoId: idProducto, cantidad: 5, motivo: 'COMPRA' }), { 'entrada 201': (r) => r.status === 201 });
    check(post('/api/v1/movimientos/salidas', { productoId: idProducto, cantidad: 12, motivo: 'VENTA' }), { 'salida 201': (r) => r.status === 201 });
    // Caso límite intencional: el 409 es la respuesta correcta, por eso no cuenta como error de la prueba.
    const sinStock = http.post(`${BASE}/api/v1/movimientos/salidas`, JSON.stringify({ productoId: idProducto, cantidad: 999, motivo: 'VENTA' }),
      { ...h, responseCallback: http.expectedStatuses(409) });
    check(sinStock, { 'stock insuficiente 409': (r) => r.status === 409 });
    check(post('/api/v1/movimientos/ajustes', { productoId: idProducto, stockContado: 2, motivo: 'CONTEO_FISICO', nota: 'Conteo de carga' }), { 'ajuste 201': (r) => r.status === 201 });
    check(http.get(`${BASE}/api/v1/movimientos?productoId=${idProducto}`, h), { 'historial 200': (r) => r.status === 200 });
    check(http.del(`${BASE}/api/v1/productos/${idProducto}`, null, h), { 'desactivar 204': (r) => r.status === 204 });
  }
  check(http.get(`${BASE}/api/v1/alertas/resumen`, h), { 'alertas 200': (r) => r.status === 200 });
  check(http.get(`${BASE}/api/v1/productos?bajoMinimo=true&size=5`, h), { 'en mínimos 200': (r) => r.status === 200 });
  sleep(0.5);
}
