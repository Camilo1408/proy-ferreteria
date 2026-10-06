# CRUD de Productos (back-end, front-end, pruebas, despliegue, monitoreo) — Diseño

Proyecto académico. Prioridad: que funcione y cumpla lo pedido, con la mínima complejidad.

## 1. Decisiones

| Tema | Decisión |
|---|---|
| Repositorio | Monorepo `proy-ferreteria` (GitHub, rama `main`) |
| CI/CD | GitHub Actions |
| Producción | Render (back-end como Web Service, PostgreSQL gestionado, front-end como Static Site) |
| Seguridad | Spring Security + JWT; roles `ADMIN` y `USER`; BCrypt |
| Idiomas | Español (por defecto) e inglés, back-end y front-end |
| Pruebas front | Vitest (unitarias), Playwright (aceptación), Selenium (2-3 flujos) |
| Pruebas back | JUnit 5, Mockito, Spring Test/MockMvc, Testcontainers (PostgreSQL), PIT (mutación) |
| Estrés | k6 |
| Monitoreo | Spring Actuator + `docs/metricas.md`; reporte de equipo con `gh` |

## 2. Estructura

```
backend/    Spring Boot 3, Java 21, Maven
frontend/   React + Vite, Material UI
db/         init.sql (esquema + datos de ejemplo)
tests/      e2e (Playwright, Selenium) y k6
scripts/    build, test, git, reporte de equipo
docs/       criterios de aceptación, casos de prueba, métricas, specs
.github/    workflows e issue templates
```

Todo archivo de código lleva un encabezado con: nombre, descripción, fecha_creacion, actualizacion, autor, version.

## 3. Entidad Producto

| Campo | Regla |
|---|---|
| id | Long, generado |
| nombre | obligatorio, 3-100 caracteres, único (sin distinguir mayúsculas) |
| categoria | obligatoria, 1-60 caracteres |
| descripcion | opcional, hasta 500 caracteres |
| estado | `ACTIVO` / `INACTIVO`; por defecto `ACTIVO` |

`DELETE` desactiva (borrado lógico): el producto pasa a `INACTIVO`, no se elimina.

## 4. API (JSON)

| Método y ruta | Rol | Resultado |
|---|---|---|
| POST `/api/v1/auth/login` | público | token JWT |
| GET `/api/v1/productos?page&size&estado&q` | USER, ADMIN | página de productos |
| GET `/api/v1/productos/{id}` | USER, ADMIN | producto o 404 |
| POST `/api/v1/productos` | ADMIN | 201 creado |
| PUT `/api/v1/productos/{id}` | ADMIN | 200 actualizado |
| DELETE `/api/v1/productos/{id}` | ADMIN | 204, producto queda INACTIVO |

Errores con formato uniforme `{codigo, mensaje, detalles[], fecha}`. Códigos HTTP: 400 validación, 401 sin token, 403 sin permiso, 404 no existe, 409 nombre duplicado. Mensajes traducidos por `Accept-Language` (`es`, `en`). Documentación OpenAPI en `/swagger-ui.html`; JavaDoc en las clases públicas.

## 5. Front-end

- Pantallas: login, listado paginado con búsqueda y filtro por estado, formulario crear/editar, diálogo de confirmación para desactivar.
- Formik + Yup con las mismas reglas del back-end.
- Tokens de color y estilo en un archivo único; tema claro y oscuro; diseño responsive; accesibilidad (etiquetas, foco visible, ARIA, contraste).
- i18next (es/en) con selector de idioma. JSDoc y comentarios.
- Rol USER solo ve el listado; los controles de escritura se ocultan (el back-end lo impone igualmente).

## 6. Criterios de aceptación

Se redactan en `docs/criterios-aceptacion.md` con IDs `CA-01…`. Cada caso de prueba (`docs/casos-prueba.md`) referencia uno o más CA, y la matriz CA → prueba se verifica al final. Ejemplos:

- CA-01 Crear producto válido devuelve 201 con id y estado ACTIVO.
- CA-02 Nombre menor de 3 o mayor de 100 caracteres devuelve 400.
- CA-03 Nombre duplicado devuelve 409.
- CA-04 Eliminar desactiva el producto; el GET lo sigue devolviendo como INACTIVO.
- CA-05 USER no puede crear, editar ni desactivar (403).
- CA-06 Sin token se recibe 401.
- CA-07 Los mensajes salen en el idioma de `Accept-Language`.

## 7. Pruebas

| Tipo | Herramienta |
|---|---|
| Unitarias back | JUnit 5 + Mockito (servicio, mapeos, validaciones) |
| Integración | SpringBootTest + MockMvc + Testcontainers |
| Sistema | Flujo completo login → CRUD contra la aplicación levantada |
| Seguridad | Acceso sin token, rol insuficiente, token inválido/expirado, inyección en `q` |
| Límite | nombre de 2/3/100/101 caracteres, descripción 500/501, página vacía, `size` extremo |
| Mutación | PIT sobre servicio y validaciones (meta ≥ 70 %) |
| Estrés | k6: rampa hasta 50 usuarios virtuales; umbral p95 < 800 ms y errores < 1 % |
| Front unitarias | Vitest + Testing Library |
| Front aceptación | Playwright; Selenium para 2-3 flujos |

Se listan todos los casos en `docs/casos-prueba.md` y se ejecutan localmente; los resultados reales se reportan sin retocarlos.

## 8. Despliegue y CI/CD

- `scripts/`: crear/inicializar BD, compilar y probar back y front, comandos de Git.
- `.github/workflows/ci.yml`: en cada push o PR a `main` compila y prueba back y front. Si pasa y es `main`, dispara el despliegue en Render por deploy hook. Si falla, abre un Issue con el log y menciona a los integrantes.
- BD: `db/init.sql` (esquema, usuarios `admin`/`user` de ejemplo con contraseñas BCrypt de desarrollo, productos de ejemplo). En Render se aplica al crear la BD.
- Los secretos (JWT secret, hooks de Render) van en GitHub Secrets/variables de Render; nunca en el repositorio.

## 9. Administración y monitoreo

- Actuator expone salud y métricas (latencia, errores HTTP).
- `docs/metricas.md`: disponibilidad, p95, tasa de errores, cobertura, puntaje de mutación, tiempo de pipeline, tasa de fallos del pipeline; con umbral y forma de medición.
- `scripts/reporte-equipo`: con `gh` lista commits, PRs e issues por integrante en un periodo.
- Errores y tareas se gestionan con GitHub Issues y Projects.

## 10. Fuera de alcance y limitaciones

- No incluye el módulo completo de inventario del documento de requisitos; solo la entidad Producto.
- No se implementan Prometheus/Grafana.
- El despliegue real en Render y los secretos requieren cuenta y acciones del usuario.
- Credenciales de ejemplo solo para desarrollo.
