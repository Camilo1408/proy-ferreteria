# Ferretería · CRUD de Productos

Proyecto académico: API REST (Spring Boot, JPA, PostgreSQL) y front-end React (Material UI) con seguridad JWT, validación, internacionalización (es/en), pruebas automáticas y CI/CD hacia Render.

| | |
|---|---|
| Web | https://proy-ferreteria-web.onrender.com |
| API | https://proy-ferreteria-api.onrender.com (Swagger: `/swagger-ui.html`) |
| Usuarios | `admin` (lee y escribe) y `user` (solo lectura). Las contraseñas desplegadas están en variables de entorno de Render. En desarrollo local: `Admin123*` / `User123*`. |

> El plan gratuito de Render duerme el servicio tras inactividad: la primera petición puede tardar ~1 minuto. La base gratuita expira el 2026-11-06.

## Estructura

```
backend/   Spring Boot 3, Java 21      db/       init.sql
frontend/  React + Vite + MUI          tests/    e2e (Playwright, Selenium) y k6
scripts/   build, test, git, reporte   docs/      criterios, casos de prueba, métricas, resultados
```

## Ejecutar en local (sin Docker)

```bash
cd backend && mvn -DskipTests package
java -jar target/inventario-backend-1.0.0.jar --spring.profiles.active=h2   # API en :8080
cd ../frontend && npm ci && npm run build && npx vite preview --port 5173   # Web en :5173
```

Con Docker (PostgreSQL real): `docker compose up --build` → web en `:8081`, API en `:8080`.

## Pruebas

```powershell
./scripts/build.ps1
./scripts/test.ps1            # JUnit, PIT, Vitest, Playwright, Selenium
./scripts/test.ps1 -Estres    # además k6
```

Detalle: [criterios de aceptación](docs/criterios-aceptacion.md) · [casos de prueba](docs/casos-prueba.md) · [resultados](docs/resultados.md) · [métricas](docs/metricas.md).

## CI/CD

`.github/workflows/ci.yml`: en cada push/PR compila y prueba; en `main`, si todo pasa, Render despliega (auto-deploy por commit). Si algo falla se abre un issue `ci-fallo` asignado a quien hizo el push, con el log y tareas.

Configuración opcional: variable `TEAM_MENTIONS` (por ejemplo `@usuario1 @usuario2`) para notificar a más integrantes; secretos `RENDER_BACKEND_DEPLOY_HOOK` y `RENDER_FRONTEND_DEPLOY_HOOK` si se desactiva el auto-deploy.

## Documentación del diseño

[Diseño](docs/superpowers/specs/2026-10-06-crud-productos-design.md) · [Plan](docs/superpowers/plans/2026-10-07-crud-productos.md) · [Comandos de Git](scripts/git-comandos.md)
