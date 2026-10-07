# Casos de prueba

Estado = resultado de la última ejecución local registrada en [resultados.md](resultados.md).

## Back-end (JUnit 5, Mockito, Spring Test/MockMvc)

| ID | Tipo | Archivo / método | Criterio |
|---|---|---|---|
| BE-U-01 | Unitaria | `ProductoServiceTest.creaConValoresPorDefecto` | CA-01 |
| BE-U-02 | Unitaria | `ProductoServiceTest.creaGuardaTodosLosCampos` | CA-01 |
| BE-U-03 | Unitaria | `ProductoServiceTest.rechazaDuplicado` | CA-03 |
| BE-U-04 | Unitaria | `ProductoServiceTest.obtenerInexistente` | CA-10 |
| BE-U-05 | Unitaria | `ProductoServiceTest.obtenerExistente` | CA-10 |
| BE-U-06 | Unitaria | `ProductoServiceTest.desactivaLogicamente` | CA-04 |
| BE-U-07 | Unitaria | `ProductoServiceTest.actualizarDuplicado` | CA-10 |
| BE-U-08 | Unitaria | `ProductoServiceTest.actualizarConservaEstado` | CA-10 |
| BE-U-09 | Unitaria | `ProductoServiceTest.actualizarModificaCampos` | CA-10 |
| BE-U-10 | Unitaria | `ProductoServiceTest.listarMapeaPagina` | CA-11 |
| BE-I-01 | Integración | `ProductoApiIT.ca01` crear válido → 201, ACTIVO | CA-01 |
| BE-I-02 | Integración/Límite | `ProductoApiIT.ca02` nombre 2/3/100/101 | CA-02 |
| BE-I-03 | Integración | `ProductoApiIT.ca03` duplicado sin distinguir mayúsculas → 409 | CA-03 |
| BE-I-04 | Integración | `ProductoApiIT.ca04` DELETE desactiva, GET devuelve INACTIVO | CA-04 |
| BE-S-01 | Seguridad | `ProductoApiIT.ca05` USER recibe 403 en escritura y 200 en lectura | CA-05 |
| BE-S-02 | Seguridad | `ProductoApiIT.ca06` sin token y token inválido → 401 | CA-06 |
| BE-I-05 | Integración | `ProductoApiIT.ca07` mensajes es/en por Accept-Language | CA-07 |
| BE-S-03 | Seguridad | `ProductoApiIT.ca08` login malo → 401, vacío → 400 | CA-08 |
| BE-I-06 | Límite | `ProductoApiIT.ca09` categoría vacía, descripción 500/501 | CA-09 |
| BE-I-07 | Integración | `ProductoApiIT.ca10` actualizar, 409 al renombrar, 404 | CA-10 |
| BE-I-08 | Límite | `ProductoApiIT.ca11` paginación, filtro, búsqueda, página 50, size 100000 | CA-11 |
| BE-S-04 | Seguridad | `ProductoApiIT.ca12` JSON roto → 400; búsqueda con comilla no inyectable | CA-12 |
| BE-M-01 | Mutación | PIT sobre `ProductoService` (`mvn org.pitest:pitest-maven:mutationCoverage`) | CA-15 |

## Front-end (Vitest + Testing Library)

| ID | Tipo | Archivo | Criterio |
|---|---|---|---|
| FE-U-01..08 | Unitaria/Límite | `schemas.test.js`: nombre 2/3/100/101, vacío, categoría 60/61, descripción 500/501 | CA-02, CA-09 |
| FE-U-09 | Unitaria | `schemas.test.js`: login exige usuario y contraseña | CA-08 |
| FE-U-10 | Unitaria | `ProductoForm.test.jsx`: errores en español, no envía inválido | CA-02 |
| FE-U-11 | Unitaria | `ProductoForm.test.jsx`: envía datos recortados | CA-01 |
| FE-U-12 | Unitaria | `ProductoForm.test.jsx`: muestra error de API | CA-03 |
| FE-U-13 | Accesibilidad | `ProductoForm.test.jsx`: diálogo con nombre accesible | CA-13 |

## Sistema / Aceptación E2E (Playwright)

| ID | Prueba | Criterio |
|---|---|---|
| PW-01 | Credenciales incorrectas muestran error | CA-08 |
| PW-02 | Campos vacíos se validan | CA-08 |
| PW-03 | Salir vuelve al login | CA-06 |
| PW-04 | Crear producto válido aparece ACTIVO | CA-01 |
| PW-05 | Nombre corto y categoría vacía no envían | CA-02, CA-09 |
| PW-06 | Nombre duplicado muestra error de API | CA-03 |
| PW-07 | Editar cambia el nombre | CA-10 |
| PW-08 | Desactivar con confirmación deja Inactivo | CA-04 |
| PW-09 | Filtro por estado | CA-11 |
| PW-10 | USER sin controles de escritura | CA-05 |
| PW-11 | Token inválido regresa al login | CA-06 |
| PW-12 | Cambio a inglés traduce interfaz y errores | CA-07 |
| PW-13 | Tema oscuro persiste | CA-13 |
| PW-14 | Móvil: tarjetas sin desbordamiento (Pixel 7) | CA-13 |
| PW-15 | Enlace de salto y nombres accesibles | CA-13 |

## Selenium (WebDriver, Chrome)

| ID | Prueba | Criterio |
|---|---|---|
| SEL-01 | Login incorrecto muestra error | CA-08 |
| SEL-02 | ADMIN crea producto y aparece en el listado | CA-01 |
| SEL-03 | USER no ve «Nuevo producto» | CA-05 |

## Estrés y límite (k6)

| ID | Prueba | Criterio |
|---|---|---|
| K6-01 | Rampa a 50 VU sobre listar, crear, obtener, actualizar y desactivar; umbrales p95 < 800 ms y errores < 1 % | CA-14 |
