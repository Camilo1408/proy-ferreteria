# CRUD de Productos Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans (ejecución en línea, autónoma). Cada tarea termina con verificación real y commit.

**Goal:** Sistema funcional CRUD de Producto (Spring Boot + React) con seguridad, i18n, pruebas, CI/CD y despliegue en Render.

**Architecture:** Monorepo. Back-end Spring Boot 3 (capas controller/service/repository), JWT stateless. Front-end React+Vite con Material UI consumiendo la API JSON. CI en GitHub Actions; despliegue por Render.

**Tech Stack:** Java 21, Maven, Spring Boot 3.3, JPA, Lombok, PostgreSQL, springdoc-openapi, jjwt; React 18, Vite, MUI 5, Formik, Yup, i18next, Vitest; Playwright, Selenium; k6; PIT.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-10-06-crud-productos-design.md` (reglas de Producto y API exactas).
- Todo archivo de código lleva header: nombre, descripcion, fecha_creacion, actualizacion, autor, version. Autor: `Camilo1408`.
- Idiomas es (defecto) y en. Sin secretos en el repo.
- Resultados de pruebas se reportan tal como salen.

---

### Task 1: Back-end núcleo (entidad, repositorio, servicio, controlador, validación, errores)
**Files:** `backend/pom.xml`, `backend/src/main/java/com/ferreteria/**` (`Producto`, `Estado`, `ProductoRepository`, `ProductoService`, `ProductoController`, DTOs, `GlobalExceptionHandler`), `backend/src/main/resources/application*.yml`, `messages*.properties`.
**Produces:** `/api/v1/productos` CRUD; errores `{codigo,mensaje,detalles,fecha}`.
- [ ] Tests unitarios (Mockito) y de integración (MockMvc + H2/Testcontainers) primero; ver fallar.
- [ ] Implementar; `mvn test` verde; commit.

### Task 2: Seguridad JWT, i18n y OpenAPI
**Files:** `security/*` (JwtService, filtro, SecurityConfig, AuthController, Usuario), `messages_en.properties`, `OpenApiConfig`.
- [ ] Tests de seguridad (401/403/rol/token inválido) y de idioma; implementar; `mvn test` verde; commit.

### Task 3: Base de datos
**Files:** `db/init.sql`, `scripts/db-*.ps1|sh`, `docker-compose.yml` (postgres + backend + frontend).
- [ ] Esquema + usuarios y productos de ejemplo; verificar con docker compose.

### Task 4: Front-end
**Files:** `frontend/**` (tokens, tema claro/oscuro, i18n, páginas Login/Productos, formulario Formik+Yup, servicio API).
- [ ] Vitest unitarias; `npm run build` verde; commit.

### Task 5: Pruebas avanzadas
**Files:** `docs/criterios-aceptacion.md`, `docs/casos-prueba.md`, límite/sistema en JUnit, PIT en `pom.xml`, `tests/k6/*.js`, `tests/e2e/playwright/*`, `tests/e2e/selenium/*`.
- [ ] Ejecutar y reportar resultados reales (incluida mutación y k6 si es viable local).

### Task 6: CI/CD y scripts
**Files:** `.github/workflows/ci.yml`, `.github/ISSUE_TEMPLATE/*`, `scripts/*`.
- [ ] Workflow compila/prueba; falla → crea issue; pasa en main → deploy hook Render.

### Task 7: Monitoreo y administración
**Files:** `docs/metricas.md`, `scripts/reporte-equipo.ps1`, Actuator configurado.

### Task 8: Despliegue en Render
- [ ] Requiere `render login` del usuario; crear Postgres, web service y static site; verificar salud; documentar URLs.
