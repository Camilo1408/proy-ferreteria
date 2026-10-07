# Requisitos del módulo de inventario (fase 2)

Proyecto académico. Amplía el CRUD de productos hacia un inventario básico: existencias, movimientos, alertas de mínimos, usuarios, perfiles con permisos modulares y cuenta propia.

## 1. Alcance

Incluye: existencias por producto, entradas, salidas y ajustes con historial (kárdex), alertas de stock mínimo, perfiles con permisos por módulo, gestión de usuarios, gestión de la cuenta propia, panel de resumen y reporte CSV de inventario.

Se incluyen además: navegación por permisos, campana de alertas con contador, diálogos de movimiento rápido desde productos, panel y alertas, y el tema claro/oscuro con español e inglés.

Fuera de alcance (explícito): múltiples bodegas, lotes y vencimientos, costos y valorización, proveedores y clientes, órdenes de compra, envío de alertas por correo o SMS, recuperación de contraseña por correo, autenticación de dos factores. Las alertas son dentro de la aplicación (campana con contador y listado).

## 2. Roles y perfiles

Los permisos son modulares y se asignan a **perfiles**; cada usuario tiene un perfil.

| Permiso | Módulo | Permite |
|---|---|---|
| PRODUCTOS_VER | Productos | Consultar productos, existencias y el panel |
| PRODUCTOS_GESTIONAR | Productos | Crear, editar y desactivar productos |
| MOVIMIENTOS_VER | Inventario | Consultar el historial de movimientos |
| MOVIMIENTOS_REGISTRAR | Inventario | Registrar entradas y salidas |
| AJUSTES_REGISTRAR | Inventario | Registrar ajustes por conteo físico |
| ALERTAS_VER | Alertas | Ver alertas y el contador |
| ALERTAS_GESTIONAR | Alertas | Reconocer alertas |
| REPORTES_VER | Reportes | Descargar el reporte de inventario |
| USUARIOS_GESTIONAR | Administración | Crear, editar, desactivar usuarios y restablecer contraseñas |
| PERFILES_GESTIONAR | Administración | Crear, editar y eliminar perfiles |

Perfiles de partida: **ADMINISTRADOR** (todos los permisos, protegido: no se edita ni se elimina) y **CONSULTA** (ver productos, movimientos, alertas y reportes; editable). Usuarios iniciales: `admin` (ADMINISTRADOR) y `user` (CONSULTA); sus contraseñas se definen por variables de entorno. El administrador puede crear perfiles nuevos con cualquier combinación de permisos. Toda cuenta autenticada puede gestionar su propia cuenta sin permiso adicional.

## 3. Requisitos funcionales y criterios de aceptación

| ID | Requisito | Criterios de aceptación |
|---|---|---|
| RF-01 | Producto con código (único, 2-30, letras, números, `.`, `_`, `-`), unidad, stock actual y stock mínimo, además de lo ya existente | **CA-16** Crear con código duplicado (sin distinguir mayúsculas) devuelve 409. **CA-17** Código con formato inválido, unidad ausente o mínimo negativo devuelven 400. **CA-18** El stock actual no se puede fijar por la edición del producto. |
| RF-02 | Stock inicial opcional al crear el producto, registrado como entrada | **CA-19** Crear con stock inicial 25 deja stock 25 y un movimiento ENTRADA con motivo STOCK_INICIAL. |
| RF-03 | Registrar entradas de stock (cantidad > 0, motivo, referencia y nota opcionales) | **CA-20** Una entrada de 10 sobre stock 5 deja stock 15 y un movimiento con stock anterior 5 y resultante 15. **CA-21** Cantidad 0, negativa o con más de 3 decimales devuelve 400. **CA-22** Un motivo que no corresponde al tipo devuelve 400. |
| RF-04 | Registrar salidas de stock; nunca se permite stock negativo | **CA-23** Salida mayor que el stock devuelve 409 `STOCK_INSUFICIENTE` y no cambia nada. **CA-24** Salida igual al stock deja 0. **CA-25** Dos salidas simultáneas de la última unidad: solo una se registra. |
| RF-05 | Registrar ajustes indicando el stock contado y una nota obligatoria | **CA-26** Contado 8 con stock 10 registra AJUSTE con variación −2 y stock 8. **CA-27** Contado igual al stock devuelve 409; sin nota (mínimo 5 caracteres) devuelve 400. |
| RF-06 | Los movimientos son inmutables y se consultan con filtros (producto, tipo, fechas) y paginación | **CA-28** No existe operación para editar ni borrar movimientos. **CA-29** El historial se filtra por producto, tipo y rango de fechas y muestra usuario, fecha, stock anterior y resultante. |
| RF-07 | No se registran movimientos sobre productos inactivos | **CA-30** Entrada, salida o ajuste sobre un producto inactivo devuelve 409. |
| RF-08 | Alertas de mínimos: un producto activo con mínimo > 0 y stock ≤ mínimo genera una alerta (BAJO, o AGOTADO si el stock es 0) | **CA-31** Una salida que lleva el stock al mínimo crea una alerta ABIERTA. **CA-32** Si el stock llega a 0 el nivel pasa a AGOTADO. **CA-33** No se duplican alertas por producto. **CA-34** Al reponer el stock por encima del mínimo, la alerta pasa a RESUELTA sola. **CA-35** Desactivar el producto resuelve su alerta. |
| RF-09 | Reconocer una alerta (queda registrado quién y cuándo); si el nivel empeora, vuelve a abrirse | **CA-36** Reconocer pasa a RECONOCIDA y deja de contar como pendiente. **CA-37** Si luego el stock llega a 0, la alerta vuelve a ABIERTA. |
| RF-10 | Listado de productos en mínimos y contador de alertas pendientes | **CA-38** `bajoMinimo=true` lista solo activos con stock ≤ mínimo. **CA-39** El resumen de alertas devuelve pendientes, bajos y agotados. |
| RF-11 | Panel con productos activos, en mínimos, agotados, alertas pendientes y movimientos del día | **CA-40** Los números coinciden con los datos. |
| RF-12 | Reporte CSV del inventario (opcionalmente solo productos en mínimos) | **CA-41** El CSV tiene encabezado traducido, escapa comillas y comas y neutraliza fórmulas (`=`, `+`, `-`, `@`). |
| RF-13 | Perfiles con permisos modulares: crear, editar, eliminar | **CA-42** Un perfil nuevo con permisos elegidos se puede asignar a un usuario. **CA-43** Nombre de perfil duplicado devuelve 409. **CA-44** El perfil ADMINISTRADOR no se edita ni se elimina. **CA-45** Un perfil con usuarios no se elimina (409). **CA-46** Un cambio que deje al sistema sin ningún usuario activo capaz de gestionar usuarios y perfiles se rechaza. |
| RF-14 | Usuarios: crear, editar, activar/desactivar y restablecer contraseña (nunca se borran) | **CA-47** Crear usuario con perfil; usuario duplicado devuelve 409. **CA-48** Un usuario desactivado no puede iniciar sesión ni usar un token anterior. **CA-49** Nadie puede desactivarse ni cambiarse el perfil a sí mismo. **CA-50** Contraseña de menos de 8 caracteres o sin letra y número devuelve 400. |
| RF-15 | Permisos aplicados en el servidor y en la interfaz | **CA-51** Cada endpoint exige su permiso: sin él devuelve 403 y sin sesión 401. **CA-52** Los cambios de permisos de un perfil rigen de inmediato en el siguiente request. **CA-53** La interfaz solo muestra las secciones y acciones permitidas. |
| RF-16 | Cuenta propia: ver y editar nombre y correo, cambiar la contraseña con la actual | **CA-54** Cambiar contraseña con la actual incorrecta devuelve 400 y no cambia nada. **CA-55** Tras cambiarla, la anterior ya no sirve y la nueva sí. **CA-56** La nueva contraseña no puede ser igual a la actual. |
| RF-17 | Migración de la base de la versión anterior | **CA-57** Una base creada con la primera versión arranca con el sistema nuevo: conserva sus productos (código `LEG-n`, unidad UND, stock 0 y mínimo 0), elimina los usuarios antiguos (con rol) y los rehace con perfiles, y permite registrar movimientos sobre los productos heredados. |

## 4. Requisitos no funcionales

| ID | Requisito | Verificación |
|---|---|---|
| RNF-01 | Integridad: el stock nunca es negativo y no hay pérdida de actualizaciones con movimientos concurrentes (bloqueo pesimista por producto) | CA-25, prueba de concurrencia |
| RNF-02 | Trazabilidad: cada movimiento guarda usuario, fecha, stock anterior y resultante | CA-29 |
| RNF-03 | Seguridad: contraseñas con BCrypt, permisos por endpoint, token revalidado contra la base en cada petición, CSV sin inyección de fórmulas | CA-48, CA-51, CA-41 |
| RNF-04 | Internacionalización es/en en mensajes de la API, interfaz y reporte | CA-07 |
| RNF-05 | Rendimiento: p95 < 800 ms con 50 usuarios virtuales sobre los flujos nuevos | CA-14 |
| RNF-06 | Accesibilidad y diseño responsive en las pantallas nuevas | CA-13 |
| RNF-07 | Compatibilidad de datos: el esquema anterior se migra al arrancar sin pérdida de productos | CA-57 |

## 5. Reglas de negocio

1. Cantidades con hasta 3 decimales (unidades fraccionables como metros o kilos).
2. Nivel de stock: **AGOTADO** si el mínimo es > 0 y el stock es 0; **BAJO** si el mínimo es > 0 y el stock ≤ mínimo; **OK** en los demás casos. Con mínimo 0 no hay alertas.
3. Motivos por tipo. ENTRADA: COMPRA, DEVOLUCION_CLIENTE, STOCK_INICIAL, OTRA_ENTRADA. SALIDA: VENTA, CONSUMO_INTERNO, MERMA, DEVOLUCION_PROVEEDOR, OTRA_SALIDA. AJUSTE: CONTEO_FISICO, DANO, CORRECCION.
4. Unidades: UND, KG, G, M, CM, L, ML, CAJA, PAQUETE, ROLLO.
5. Contraseña: 8 a 72 caracteres, sin espacios, al menos una letra y un número.
6. Los nombres de usuario se guardan en minúscula.
7. Un movimiento nunca se corrige: se compensa con otro (ajuste).

## 6. Matriz de endpoints

Cada permiso se exige dos veces: por ruta en `SecurityConfig` (para que quien no lo tiene reciba 403 antes de que se valide el cuerpo) y con `@PreAuthorize` en el controlador. Los permisos se releen de la base en cada petición, de modo que un cambio de perfil o una desactivación rigen de inmediato.

| Endpoint | Permiso |
|---|---|
| `POST /auth/login` | público |
| `GET/PUT /cuenta`, `PUT /cuenta/clave` | sesión iniciada |
| `GET /productos`, `GET /productos/{id}`, `GET /dashboard` | PRODUCTOS_VER |
| `POST/PUT/DELETE /productos` | PRODUCTOS_GESTIONAR |
| `GET /movimientos` | MOVIMIENTOS_VER |
| `POST /movimientos/entradas`, `/salidas` | MOVIMIENTOS_REGISTRAR |
| `POST /movimientos/ajustes` | AJUSTES_REGISTRAR |
| `GET /alertas`, `GET /alertas/resumen` | ALERTAS_VER |
| `POST /alertas/{id}/reconocer` | ALERTAS_GESTIONAR |
| `GET /reportes/inventario.csv` | REPORTES_VER |
| `GET /perfiles`, `GET /perfiles/permisos` | PERFILES_GESTIONAR o USUARIOS_GESTIONAR |
| `POST/PUT/DELETE /perfiles` | PERFILES_GESTIONAR |
| `/usuarios` (todas) | USUARIOS_GESTIONAR |
| `/actuator/**` (salvo salud) | USUARIOS_GESTIONAR |

Todas las rutas cuelgan de `/api/v1`.
