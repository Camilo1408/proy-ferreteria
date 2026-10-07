# Resultados de ejecución

Fecha: 2026-10-07. Resultados reales de las ejecuciones, sin ajustes.

| Suite | Herramienta | Resultado | Dónde se ejecutó |
|---|---|---|---|
| Back-end unitarias | JUnit 5 + Mockito | 10 de 10 | local y CI |
| Back-end integración, sistema, seguridad y límite | SpringBootTest + MockMvc (H2 modo PostgreSQL) | 12 de 12 | local y CI |
| Mutación | PIT sobre `ProductoService` | 19 de 19 mutantes eliminados (100 %), umbral 70 % | local y CI |
| Front-end | Vitest + Testing Library | 13 de 13 | local y CI |
| Aceptación E2E | Playwright (escritorio y móvil Pixel 7) | 16 pasan, 1 omitida por diseño (solo móvil) | local y CI |
| Selenium | WebDriver + Chrome | 3 de 3 | local y CI |
| Estrés | k6, rampa a 50 usuarios virtuales, 70 s | 19.306 peticiones, 0 % de errores, p95 = 4,3 ms (umbral 800 ms) | local |
| Humo en producción | curl + Playwright (9 pruebas que no escriben datos) | Todas pasan | Render |

## Limitaciones declaradas

- Las pruebas automáticas usan H2 en modo PostgreSQL; la validación contra PostgreSQL real se hizo en Render (login, CRUD, roles, CORS, Swagger). **`db/init.sql` y `docker-compose.yml` no se ejecutaron**: Docker Desktop no arrancó en la máquina y no hay `psql` local. En Render el esquema lo crea Hibernate (`DDL_AUTO=update`).
- La prueba de estrés se corrió contra la API local con H2, no contra Render (el plan gratuito se duerme y tiene recursos limitados), por lo que los 4,3 ms no son representativos de producción.
- No se ejecutó ninguna prueba de estrés ni de mutación contra PostgreSQL.
- La base de datos gratuita de Render expira el 2026-11-06.
