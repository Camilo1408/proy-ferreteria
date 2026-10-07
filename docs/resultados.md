# Resultados de ejecución

Fecha: 2026-10-07. Resultados reales de las ejecuciones, sin ajustes. Detalle de cada caso en [casos-prueba.md](casos-prueba.md).

| Suite | Herramienta | Resultado | Dónde se ejecutó |
|---|---|---|---|
| Back-end integración, sistema, seguridad y límite | JUnit 5 + Spring Test/MockMvc (H2 modo PostgreSQL) | 59 de 59 | local y CI |
| Back-end unitarias | JUnit 5 + Mockito | 47 de 47 | local y CI |
| Esquema (`db/init.sql` valida contra las entidades) y migración desde la versión anterior | Spring Boot Test | 2 de 2 | local y CI |
| Mutación | PIT sobre los 5 servicios de negocio | 139 de 140 mutantes eliminados (99 %), cobertura de líneas 100 %; umbral 70 % | local y CI |
| Front-end unitarias y de componentes | Vitest + Testing Library | 93 de 93 | local y CI |
| Aceptación y sistema E2E | Playwright, escritorio y móvil Pixel 7 | 35 pasan, 2 omitidas por diseño (cada una aplica solo a escritorio o solo a móvil) | local y CI |
| Selenium | WebDriver + Chrome | 5 de 5 | local y CI |
| Estrés | k6, rampa a 50 usuarios virtuales, 70 s, flujo completo de inventario | 40.052 peticiones, 0 % de errores, p95 = 13,9 ms (umbral 800 ms), promedio 3,8 ms, 3.641 iteraciones | local |
| Humo en producción (PostgreSQL de Render) | API con Python y Playwright sobre el sitio desplegado | Todo correcto (ver abajo) | Render |

Total de pruebas automáticas ejecutadas: 108 (back-end) + 93 (front-end) + 37 (Playwright, 2 omitidas) + 5 (Selenium) + k6.

## Verificación en producción

Tras desplegar sobre la base que ya tenía datos de la primera versión:

- La migración convirtió los 6 productos existentes en `LEG-1…LEG-6` (unidad UND, stock 0) y rehízo los usuarios con perfiles: `admin` con ADMINISTRADOR (10 permisos) y `user` con CONSULTA (4 permisos).
- `user` recibe 403 al crear productos y al ver usuarios; sin sesión se recibe 401.
- Flujos de inventario sobre PostgreSQL: ajustes de carga inicial, entrada de stock inicial, salida sin stock (`STOCK_INSUFICIENTE`, también en inglés), motivo inválido (`MOTIVO_INVALIDO`), panel (6 activos, 2 en mínimos, 1 agotado), alertas (`TOR-1PG` agotado y `TAL-650` bajo), historial con usuario y CSV de productos en mínimos.
- Las 9 pruebas E2E de sesión y permisos de la interfaz pasan contra el sitio desplegado.

## Defectos encontrados y corregidos durante las pruebas

- Sin permiso, una petición con cuerpo inválido recibía 400 en lugar de 403 (la validación corría antes del permiso). Se añadió el permiso por ruta.
- Rutas inexistentes y métodos no permitidos devolvían 500; ahora 404 y 405.
- Los diálogos de creación conservaban los valores de la apertura anterior; ahora se reinician al cerrar.
- Un `role="alert"` anidado duplicaba el aviso a lectores de pantalla (corregido en la primera fase).

## Limitaciones declaradas

- Las pruebas automáticas usan H2 en modo PostgreSQL. La compatibilidad con PostgreSQL real se comprobó en Render (migración, CRUD, movimientos, alertas, reporte), no en CI.
- **`docker-compose.yml` no se ejecutó**: Docker Desktop no arrancó en la máquina de desarrollo. `db/init.sql` sí está verificado (se ejecuta sobre H2 y Hibernate valida el esquema), pero no sobre un PostgreSQL local.
- La prueba de estrés se corrió contra la API local con H2; los tiempos no representan al servicio gratuito de Render.
- Render **no** se redespliega solo con cada push (el servicio se creó desde una URL pública, sin webhook); lo hace el job de despliegue del pipeline con el secreto `RENDER_API_KEY`. Verificado: tras las pruebas en verde el job creó un deploy (disparador `api`) y la API quedó en Live con `/actuator/health` en `UP`. Sin el secreto, el job avisa y el despliegue se hace con `render deploys create <id>`.
- El plan gratuito duerme el servicio tras inactividad (la primera petición puede tardar cerca de un minuto) y la base gratuita expira el 2026-11-06.
- Fuera de alcance, por decisión: múltiples bodegas, lotes y vencimientos, costos, proveedores y clientes, alertas por correo, recuperación de contraseña por correo y autenticación de dos factores.
