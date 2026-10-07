# Diseño técnico del módulo de inventario

Complementa [requisitos-inventario.md](requisitos-inventario.md). Describe el modelo de datos, las decisiones clave y cómo se verifica cada una.

## 1. Arquitectura

Aplicación Spring Boot en capas por módulo: `producto`, `inventario` (movimientos), `alerta`, `security` (usuarios, perfiles, cuenta), `reporte`, `error` y `config`. Cada módulo tiene controlador (HTTP y permisos), servicio (reglas y transacciones), repositorio (JPA) y DTO. El front-end React consume la API JSON y decide qué mostrar según los permisos de la sesión; el servidor los exige de todos modos.

## 2. Modelo de datos

```mermaid
erDiagram
    PERFIL ||--o{ PERFIL_PERMISO : tiene
    PERFIL ||--o{ APP_USUARIO : "asignado a"
    PRODUCTO ||--o{ MOVIMIENTO : registra
    PRODUCTO ||--o{ ALERTA : genera
    PERFIL { bigint id PK
      varchar nombre UK
      boolean sistema }
    PERFIL_PERMISO { bigint perfil_id FK
      varchar permiso }
    APP_USUARIO { bigint id PK
      varchar username UK
      varchar password
      boolean activo
      bigint perfil_id FK }
    PRODUCTO { bigint id PK
      varchar codigo UK
      varchar nombre
      varchar unidad
      numeric stock_actual
      numeric stock_minimo
      varchar estado }
    MOVIMIENTO { bigint id PK
      bigint producto_id FK
      varchar tipo
      varchar motivo
      numeric cantidad
      numeric variacion
      numeric stock_anterior
      numeric stock_resultante
      varchar usuario
      timestamptz fecha }
    ALERTA { bigint id PK
      bigint producto_id FK
      varchar nivel
      varchar estado
      numeric stock_al_detectar
      numeric stock_minimo }
```

Restricciones en la base: `stock_actual >= 0`, `stock_minimo >= 0`, `cantidad > 0`, `stock_resultante >= 0`, valores válidos de estado, tipo y nivel, claves únicas de código, usuario y nombre de perfil. El script completo es [db/init.sql](../db/init.sql).

## 3. Decisiones de diseño y su verificación

| Decisión | Motivo | Verificación |
|---|---|---|
| Bloqueo pesimista de la fila del producto (`SELECT … FOR UPDATE`) en cada movimiento | Dos salidas simultáneas no pueden dejar stock negativo ni perder una actualización | CA-25 (12 hilos sobre 5 unidades: exactamente 5 salidas) |
| `@DynamicUpdate` en `Producto` y el servicio nunca asigna el stock al editar | Una edición del producto no pisa un stock cambiado en paralelo | CA-18 |
| Movimientos sin setters ni endpoints de edición o borrado | Trazabilidad: se corrige con un ajuste, nunca reescribiendo | CA-28 |
| Una sola alerta vigente por producto, evaluada tras cada cambio de stock, mínimo o estado | Sin duplicados ni alertas obsoletas | CA-31 a CA-37 |
| Permisos releídos de la base en cada petición (el JWT solo identifica) | Desactivar un usuario o editar un perfil rige al instante | CA-48, CA-52 |
| Permiso exigido por ruta y por método | Sin permiso se responde 403 antes de validar el cuerpo | CA-51 |
| Guardián de «último administrador» dentro de la transacción | Nunca se pierde la capacidad de administrar usuarios y perfiles | CA-46 |
| Perfil de sistema inmutable; un usuario no puede desactivarse ni cambiarse el perfil a sí mismo | Evita bloquear la administración por error | CA-44, CA-49 |
| Cantidades `NUMERIC(14,3)`; nunca coma flotante en el servidor | Exactitud con metros, kilos y litros | CA-21, CA-26 |
| Migración idempotente previa a Hibernate (`migracion-previa.sql`) más `MigracionDatos` | Actualizar la base ya desplegada sin perder productos | CA-57 |
| CSV con BOM y neutralización de fórmulas | Excel abre bien las tildes y no ejecuta contenido malicioso | CA-41 |

## 4. Ciclo de vida de una alerta

```mermaid
stateDiagram-v2
    [*] --> ABIERTA: stock ≤ mínimo (mínimo > 0)
    ABIERTA --> RECONOCIDA: usuario con ALERTAS_GESTIONAR
    RECONOCIDA --> ABIERTA: el nivel empeora a AGOTADO
    ABIERTA --> RESUELTA: stock > mínimo, mínimo = 0 o producto inactivo
    RECONOCIDA --> RESUELTA: stock > mínimo, mínimo = 0 o producto inactivo
    RESUELTA --> [*]
```

## 5. Registro de un movimiento

```mermaid
sequenceDiagram
    participant U as Usuario
    participant C as MovimientoController
    participant S as MovimientoService
    participant P as ProductoRepository
    participant A as AlertaService
    U->>C: POST /movimientos/salidas
    C->>C: permiso por ruta y @PreAuthorize
    C->>S: salida(solicitud validada)
    S->>P: findByIdForUpdate (bloquea la fila)
    S->>S: valida motivo, estado y stock suficiente
    S->>P: guarda el nuevo stock
    S->>S: guarda el movimiento (anterior, resultante, usuario)
    S->>A: evaluar(producto)
    S-->>U: 201 con el movimiento
```

## 6. Interfaz

- Cabecera con navegación según permisos, campana de alertas (se actualiza cada minuto y tras cada cambio de stock), idioma y tema.
- Pantallas: Panel, Productos, Movimientos, Alertas, Usuarios, Perfiles y Mi cuenta. Cada una funciona como tabla en escritorio y como tarjetas en móvil.
- Los formularios usan Formik y Yup con las mismas reglas que el servidor; los errores de la API se muestran en el mismo formulario.
- Un diálogo se reinicia al cerrarse para no conservar datos de la apertura anterior (defecto detectado por las pruebas E2E y corregido).
- Accesibilidad: enlace de salto, `nav` con nombre, etiquetas en todos los campos y botones de icono, `aria-current`, foco visible y avisos con `role="status"`.

## 7. Seguridad

Contraseñas con BCrypt; política de 8 a 72 caracteres con letra y número, sin espacios; CORS por variable de entorno; errores sin detalles internos; el reporte CSV neutraliza fórmulas; los datos de sesión viven en `sessionStorage`. No hay bloqueo por intentos fallidos ni autenticación de dos factores (fuera de alcance).
