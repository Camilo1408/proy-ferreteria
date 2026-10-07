# Casos de prueba

Cada caso se traza a uno o más criterios de aceptación ([criterios-aceptacion.md](criterios-aceptacion.md)). Los resultados de la última ejecución real están en [resultados.md](resultados.md). Todas las pruebas son automáticas y se ejecutan en el pipeline.

## 1. Back-end: pruebas de integración, sistema, seguridad y límite (JUnit 5 + Spring Test/MockMvc, H2 en modo PostgreSQL)

Se ejecutan con seguridad real (JWT y permisos), validación, i18n y transacciones.

| Archivo | Caso | Criterio |
|---|---|---|
| `ProductoApiIT` | `ca01` crear válido: 201, id, ACTIVO, stock 0 | CA-01 |
| | `ca02` nombre 2/3/100/101 caracteres | CA-02 |
| | `ca03` nombre duplicado sin distinguir mayúsculas: 409 | CA-03 |
| | `ca04` DELETE desactiva; GET devuelve INACTIVO | CA-04 |
| | `ca05` sin permiso de gestión: 403 en escritura, 200 en lectura | CA-05 |
| | `ca06` sin token o token inválido: 401 | CA-06 |
| | `ca07` mensajes es/en por Accept-Language | CA-07 |
| | `ca08` login correcto/incorrecto/vacío; usuario en mayúsculas | CA-08 |
| | `ca09` categoría obligatoria; descripción 500/501 | CA-09 |
| | `ca10` actualizar, 409 al renombrar, 404 | CA-10 |
| | `ca11` paginación, filtros, búsqueda por código, página 50, size 100000 | CA-11 |
| | `ca12` JSON roto, inyección en búsqueda, enum inválido, ruta inexistente, método no permitido | CA-12 |
| | `ca16` código duplicado (crear y editar) | CA-16 |
| | `ca17` código con formato inválido, unidad ausente o inválida, mínimo negativo, 4 decimales | CA-17 |
| | `ca18` editar no cambia el stock | CA-18 |
| | `ca19` stock inicial genera entrada STOCK_INICIAL | CA-19 |
| `MovimientoApiIT` | `ca20` entrada: stock, anterior/resultante, usuario, referencia | CA-20 |
| | `ca21` cantidad 0, negativa, 4 decimales, nula; 0,001 válida | CA-21 |
| | `ca22` motivo de otro tipo o inexistente | CA-22 |
| | `ca23` salida mayor al stock: 409 y sin cambios | CA-23 |
| | `ca24` salida igual al stock deja 0 | CA-24 |
| | `ca25` 12 salidas simultáneas sobre 5 unidades: exactamente 5 exitosas | CA-25 |
| | `ca26` ajuste con variación negativa, positiva y a cero | CA-26 |
| | `ca27` ajuste sin diferencia, sin nota, nota corta, contado negativo | CA-27 |
| | `ca28` no existen rutas para editar o borrar movimientos | CA-28 |
| | `ca29` historial por producto, tipo, fechas, orden y paginación | CA-29 |
| | `ca30` producto inactivo no admite movimientos | CA-30 |
| | `permisosIndependientes` entradas/salidas, ajustes e historial son permisos distintos | CA-51 |
| `AlertaApiIT` | `ca31` salida al mínimo crea alerta BAJO | CA-31 |
| | `ca32` stock 0 pasa a AGOTADO | CA-32 |
| | `ca33` sin duplicados | CA-33 |
| | `ca34` reponer resuelve la alerta | CA-34 |
| | `ca35` desactivar resuelve; subir el mínimo crea; mínimo 0 no alerta | CA-35 |
| | `ca36` reconocer; idempotencia; 404; resuelta no reconocible | CA-36 |
| | `ca37` empeorar reabre | CA-37 |
| | `ca38` filtro `bajoMinimo` | CA-38 |
| | `ca39` resumen pendientes/bajos/agotados | CA-39 |
| | `permisos` ver y reconocer son permisos distintos | CA-51 |
| `ReporteApiIT` | `ca40` panel coincide con los datos | CA-40 |
| | `ca41` CSV: encabezado, escape, fórmulas neutralizadas, inglés, solo mínimos | CA-41 |
| | `permisos` reporte y panel exigen su permiso | CA-51 |
| `AdministracionApiIT` | `ca42` perfil nuevo asignado a un usuario; alcance real | CA-42 |
| | `ca43` perfil duplicado y validaciones | CA-43 |
| | `ca44` perfil de sistema inmutable | CA-44 |
| | `ca45` perfil en uso no se elimina | CA-45 |
| | `ca46`, `ca46Ultimo` guardián del último gestor, con rollback | CA-46 |
| | `ca47` alta de usuario, minúsculas, duplicado, validaciones, búsqueda | CA-47 |
| | `ca48` usuario desactivado: sin login y token anterior inválido | CA-48 |
| | `ca49` auto-modificación rechazada | CA-49 |
| | `ca50` contraseñas débiles | CA-50 |
| | `ca51` matriz de 403/401 por endpoint | CA-51 |
| | `ca52` cambio de permisos de un perfil rige sin reiniciar sesión | CA-52 |
| | `ca54`, `ca55`, `ca56` cambio de contraseña propio | CA-54 a CA-56 |
| | `catalogo`, `cuentaPropia` catálogo de permisos y edición de la cuenta | CA-42 |

## 2. Back-end: esquema y migración

| Archivo | Caso | Criterio |
|---|---|---|
| `InitSqlTest` | `db/init.sql` se ejecuta sobre una base vacía y Hibernate (`ddl-auto=validate`) confirma que coincide con las entidades; los datos de ejemplo son coherentes | CA-57 |
| `MigracionTest` | Una base de la versión anterior (tablas y filas heredadas) arranca con el sistema nuevo, conserva sus productos y permite movimientos | CA-57 |

## 3. Back-end: pruebas unitarias (JUnit 5 + Mockito) y mutación (PIT)

| Archivo | Cobertura | Criterio |
|---|---|---|
| `ProductoServiceTest` | valores por defecto, stock inicial, duplicados de nombre y código, edición sin tocar el stock, desactivación, listado y regla de nivel (OK/BAJO/AGOTADO) | CA-01 a CA-04, CA-16 a CA-19 |
| `MovimientoServiceTest` | suma y resta, límites del stock, motivos, producto inexistente o inactivo, ajuste con y sin diferencia, stock inicial | CA-20 a CA-30 |
| `AlertaServiceTest` | crear, no crear, resolver, reabrir al empeorar, mejorar sin reabrir, reconocer, resumen y faltante | CA-31 a CA-39 |
| `AdminServicesTest` | usuarios (alta, edición, auto-modificación, guardián, contraseñas, cuenta) y perfiles (alta, sistema, edición, eliminación, catálogo) | CA-42 a CA-56 |
| PIT | `mvn org.pitest:pitest-maven:mutationCoverage` sobre los cinco servicios de negocio, con umbral de 70 % | CA-15 |

## 4. Front-end: pruebas unitarias y de componentes (Vitest + Testing Library)

| Archivo | Cobertura | Criterio |
|---|---|---|
| `schemas.test.js` | producto (código, nombre, categoría, unidad, cantidades con coma/punto y límites), movimiento (entrada/salida/ajuste, motivo por tipo, nota), usuario, contraseñas (límites 8/72), perfil, cuenta, cambio de contraseña, login | CA-02, CA-09, CA-17, CA-21, CA-27, CA-50, CA-54 a CA-56 |
| `format.test.js` | formato de cantidades, variaciones con signo, fechas y números escritos por el usuario | CA-07 |
| `ProductoForm.test.jsx` | errores en español, cuerpo enviado, edición sin stock inicial, error de la API, nombre accesible | CA-01, CA-02, CA-03, CA-13, CA-17 |
| `MovimientoDialog.test.jsx` | tipos según permiso, motivos según tipo, validación, envío, ajuste con nota, error de stock insuficiente | CA-20 a CA-23, CA-26, CA-27, CA-53 |
| `AppHeader.test.jsx` | navegación por permisos, campana de alertas y su actualización, `aria-current`, traducción | CA-07, CA-13, CA-39, CA-53 |
| `Perfiles.test.jsx` | agrupación por módulo, perfil de sistema de solo lectura, permiso obligatorio, «todo el módulo», confirmación al eliminar | CA-42, CA-44, CA-53 |

## 5. Sistema y aceptación E2E (Playwright, escritorio y móvil Pixel 7)

| Archivo | Cobertura | Criterio |
|---|---|---|
| `sesion.spec.js` | login correcto/incorrecto/vacío, salir, secciones del administrador, consulta sin controles de escritura, «Sin acceso» por URL, token inválido, inglés (login y sesión), tema oscuro persistente | CA-05 a CA-08, CA-13, CA-53 |
| `productos.spec.js` | crear con stock inicial, validaciones, duplicados de nombre y código, editar sin tocar el stock, desactivar, filtros (estado, texto, solo en mínimos), exportar CSV | CA-01 a CA-04, CA-09 a CA-11, CA-16 a CA-19, CA-38, CA-41 |
| `inventario.spec.js` | entrada con referencia, salida mayor al stock, validación de cantidad, ajuste con nota, filtro del historial, ciclo completo de una alerta (aparece, se reconoce, empeora, se repone, se resuelve), panel | CA-20 a CA-29, CA-31 a CA-37, CA-39, CA-40 |
| `administracion.spec.js` | perfil modular asignado a un usuario y verificado en la interfaz; ampliación de permisos; perfil duplicado; perfil de sistema y perfil en uso; alta de usuario con validaciones; usuario desactivado; auto-modificación; restablecer contraseña; cuenta propia | CA-42 a CA-56 |
| `responsive.spec.js` | móvil: tarjetas y sin desborde en las siete pantallas; enlace de salto y etiquetas accesibles | CA-13 |

## 6. Selenium WebDriver (Chrome)

| ID | Prueba | Criterio |
|---|---|---|
| SEL-01 | Login incorrecto muestra error | CA-08 |
| SEL-02 | ADMIN crea un producto con stock inicial y lo ve con sus existencias | CA-01, CA-19 |
| SEL-03 | CONSULTA no ve «Nuevo producto» ni Usuarios/Perfiles | CA-05, CA-53 |
| SEL-04 | Entrada y salida actualizan el stock; la salida bajo el mínimo marca «Bajo» y genera la alerta | CA-20, CA-31 |
| SEL-05 | El administrador abre el formulario de perfil con los 10 permisos agrupados por módulo | CA-42 |

## 7. Estrés (k6)

| ID | Prueba | Criterio |
|---|---|---|
| K6-01 | Rampa a 50 usuarios virtuales durante 70 s sobre el flujo completo: panel, listado, crear producto, entrada, salida, salida sin stock (409 esperado), ajuste, historial, desactivar, alertas y mínimos. Umbrales: p95 < 800 ms, errores < 1 %, verificaciones > 99 % | CA-14, CA-25 |
