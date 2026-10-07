# Ferretería · Inventario

Sistema de inventario sencillo para una ferretería: productos con existencias, entradas, salidas y ajustes con historial, alertas de stock mínimo, usuarios y perfiles con permisos modulares, cuenta propia, panel y reporte CSV. Proyecto académico: API REST (Spring Boot, JPA, PostgreSQL) y front-end React (Material UI) con seguridad JWT, validación, internacionalización (es/en), pruebas automáticas y CI/CD.

| | |
|---|---|
| Web | https://proy-ferreteria-web.onrender.com |
| API | https://proy-ferreteria-api.onrender.com (Swagger: `/swagger-ui.html`) |
| Usuarios | `admin` (perfil ADMINISTRADOR, todos los permisos) y `user` (perfil CONSULTA, solo lectura). Las contraseñas desplegadas están en variables de entorno de Render. En desarrollo local: `Admin123*` y `User123*`. |

> El plan gratuito de Render duerme el servicio tras inactividad (la primera petición puede tardar ~1 minuto) y su base de datos expira el 2026-11-06.

## Qué incluye

- **Productos** con código único, unidad (unidad, kg, m, litro, caja…), stock actual, stock mínimo y borrado lógico.
- **Movimientos**: entradas, salidas (nunca dejan el stock negativo, ni con peticiones simultáneas) y ajustes por conteo físico. El historial (kárdex) es inmutable y guarda usuario, fecha, stock anterior y resultante.
- **Alertas de mínimos**: se crean solas cuando el stock llega al mínimo, pasan a «agotado» en cero, se reconocen, y se resuelven al reponer. Campana con contador en la cabecera.
- **Perfiles con permisos modulares** (10 permisos en 5 módulos) y **usuarios**: el administrador crea perfiles a la medida; los cambios rigen de inmediato.
- **Mi cuenta**: cada usuario edita sus datos y cambia su contraseña.
- **Panel** de inicio y **reporte CSV** del inventario.
- Español e inglés, tema claro y oscuro, diseño responsive y accesible.

## Documentación

| Documento | Contenido |
|---|---|
| [Requisitos del inventario](docs/requisitos-inventario.md) | Alcance, permisos, 17 requisitos funcionales, no funcionales, reglas de negocio y matriz de endpoints |
| [Criterios de aceptación](docs/criterios-aceptacion.md) | CA-01 a CA-57 |
| [Diseño técnico](docs/diseno-inventario.md) | Modelo de datos, decisiones, ciclo de vida de alertas y secuencia de un movimiento |
| [Casos de prueba](docs/casos-prueba.md) | Todas las pruebas trazadas a los criterios |
| [Resultados](docs/resultados.md) | Resultados reales, verificación en producción, defectos corregidos y limitaciones |
| [Métricas](docs/metricas.md) | Métricas del producto, del negocio y del proceso; evaluación del equipo |
| [Comandos de Git](scripts/git-comandos.md) | Flujo de trabajo del repositorio |

## Estructura

```
backend/   Spring Boot 3, Java 21      db/       init.sql (esquema completo y datos de ejemplo)
frontend/  React + Vite + MUI          tests/    e2e (Playwright, Selenium) y k6
scripts/   build, test, git, reporte   docs/      requisitos, diseño, pruebas, métricas
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

## CI/CD

`.github/workflows/ci.yml`: en cada push o PR compila, prueba (incluida la mutación) y corre los E2E. Si algo falla en `main` abre un issue `ci-fallo` asignado a quien hizo el push, con el log y tareas.

Despliegue en Render: el servicio **no** se redespliega solo con cada push. Para automatizarlo, cree una API key en Render (Account Settings → API Keys) y guárdela como secreto `RENDER_API_KEY` del repositorio; las variables `RENDER_API_SERVICE_ID` y `RENDER_WEB_SERVICE_ID` ya están definidas. También funcionan los secretos `RENDER_BACKEND_DEPLOY_HOOK` y `RENDER_FRONTEND_DEPLOY_HOOK`. Despliegue manual:

```bash
render deploys create srv-db2ufke7bikc73b28gsg   # API
render deploys create srv-db2ufkss728c73anf1mg   # sitio web
```

Opcional: variable `TEAM_MENTIONS` (por ejemplo `@usuario1 @usuario2`) para notificar a más integrantes en los issues de fallo.

## Diseño del primer entregable

[Diseño](docs/superpowers/specs/2026-10-06-crud-productos-design.md) · [Plan](docs/superpowers/plans/2026-10-07-crud-productos.md)
