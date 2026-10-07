/**
 * nombre: estres.js
 * descripcion: Prueba de estrés con k6: rampa hasta 50 usuarios virtuales sobre login, listado y CRUD.
 *              Umbrales: p95 < 800 ms y errores < 1 %.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
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
  },
};

export function setup() {
  const r = http.post(`${BASE}/api/v1/auth/login`, JSON.stringify({ username: 'admin', password: ADMIN_PASS }),
    { headers: { 'Content-Type': 'application/json' } });
  return { token: r.json('token') };
}

export default function (data) {
  const h = { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${data.token}` } };

  const lista = http.get(`${BASE}/api/v1/productos?size=20`, h);
  check(lista, { 'listar 200': (r) => r.status === 200 });

  const nombre = `k6-${__VU}-${__ITER}-${Date.now()}`;
  const crea = http.post(`${BASE}/api/v1/productos`, JSON.stringify({ nombre, categoria: 'Carga', descripcion: 'k6' }), h);
  check(crea, { 'crear 201': (r) => r.status === 201 });

  if (crea.status === 201) {
    const id = crea.json('id');
    check(http.get(`${BASE}/api/v1/productos/${id}`, h), { 'obtener 200': (r) => r.status === 200 });
    check(http.put(`${BASE}/api/v1/productos/${id}`, JSON.stringify({ nombre: `${nombre}-b`, categoria: 'Carga' }), h),
      { 'actualizar 200': (r) => r.status === 200 });
    check(http.del(`${BASE}/api/v1/productos/${id}`, null, h), { 'desactivar 204': (r) => r.status === 204 });
  }
  sleep(0.5);
}
