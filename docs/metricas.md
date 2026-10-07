# Métricas, evaluación y monitoreo

Proyecto académico. Los umbrales son objetivos del equipo, no compromisos de producción.

## 1. Métricas del producto (en ejecución)

| Métrica | Fuente | Umbral | Evaluación |
|---|---|---|---|
| Disponibilidad | `GET /actuator/health` (Render lo usa como health check) | Responde UP | Render reinicia si falla; se revisa en el panel |
| Latencia p95 | k6 (`tests/k6/estres.js`) y `/actuator/metrics/http.server.requests` | < 800 ms | En cada entrega ejecutar `scripts/test.ps1 -Estres` |
| Tasa de errores HTTP 5xx | `/actuator/metrics/http.server.requests` | < 1 % | Revisión semanal |
| Uso de memoria JVM | `/actuator/metrics/jvm.memory.used` | < 80 % del límite del plan | Revisión semanal |

Los endpoints de métricas requieren token de ADMIN.

### Métricas de negocio del inventario

| Métrica | Fuente | Umbral / uso |
|---|---|---|
| Productos con stock negativo | consulta `select count(*) from producto where stock_actual < 0` (la base lo impide con un `CHECK`) | 0 siempre |
| Productos en mínimos y agotados | `GET /api/v1/dashboard` | Revisión diaria; debe tender a 0 |
| Alertas pendientes (abiertas) | `GET /api/v1/alertas/resumen` y campana de la interfaz | Cada alerta se reconoce o se repone en el día |
| Tiempo medio de resolución de una alerta | `resuelta_en - creada_en` en la tabla `alerta` | Seguimiento semanal |
| Movimientos por tipo y por usuario | `GET /api/v1/movimientos` (kárdex) | Control de actividad y detección de ajustes frecuentes |
| Ajustes sobre el total de movimientos | tipo AJUSTE / total | Un porcentaje alto indica problemas de conteo o de proceso |
| Intentos de salida con stock insuficiente | respuestas 409 `STOCK_INSUFICIENTE` en `http.server.requests` | Seguimiento: señala desorden entre inventario físico y sistema |
| Usuarios activos por perfil | `GET /api/v1/usuarios` | Revisión mensual de permisos (principio de mínimo privilegio) |

## 2. Métricas del proceso (calidad)

| Métrica | Fuente | Umbral |
|---|---|---|
| Pruebas automáticas pasando | CI | 100 % |
| Puntaje de mutación (PIT) | `backend/target/pit-reports` | ≥ 70 % |
| Criterios de aceptación con prueba | `docs/casos-prueba.md` | 57 de 57 |
| Tiempo del pipeline | GitHub Actions | < 15 min |
| Tasa de fallos del pipeline en `main` | Issues con etiqueta `ci-fallo` / ejecuciones | < 20 % |
| Tiempo de corrección de un fallo | Issue abierto → cerrado | < 2 días |

## 3. Evaluación de integrantes

`scripts/reporte-equipo.ps1` genera `docs/reporte-equipo.md` con commits, PRs, PRs fusionados e issues asignados y cerrados por persona. Se complementa con:

- Revisión de PRs entre pares (cada PR a `main` lo revisa otro integrante).
- Cumplimiento de tareas del tablero (GitHub Projects) frente a lo asignado.
- Calidad: pruebas incluidas en cada PR y fallos de CI atribuibles.

Advertencia: el número de commits no mide esfuerzo ni calidad; el reporte es una ayuda para la conversación del equipo, no una calificación automática.

## 4. Monitoreo de errores y tareas

- Un fallo del pipeline crea automáticamente un issue (`ci-fallo`) asignado a quien hizo el push, con enlace al log y lista de tareas; GitHub notifica por correo a los asignados y a los mencionados en la variable `TEAM_MENTIONS`.
- Errores de ejecución: `render logs` o el panel de Render; los errores internos devuelven código `ERROR_INTERNO` sin exponer detalles.
- Tareas: GitHub Issues y Projects con las plantillas de `.github/ISSUE_TEMPLATE`.
