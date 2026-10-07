-- nombre: init.sql
-- descripcion: Esquema completo e inicialización de la base de datos (PostgreSQL) con productos de ejemplo.
--              Los usuarios admin/user y los perfiles los crea la aplicación al arrancar (contraseñas por variable
--              de entorno). Una prueba automática (InitSqlTest) valida que este script coincide con las entidades.
-- fecha_creacion: 2026-10-07
-- actualizacion: 2026-10-08
-- autor: Camilo1408
-- version: 1.1.0

CREATE TABLE IF NOT EXISTS perfil (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(60)  NOT NULL UNIQUE,
    descripcion VARCHAR(200),
    sistema     BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS perfil_permiso (
    perfil_id BIGINT      NOT NULL REFERENCES perfil (id),
    permiso   VARCHAR(40) NOT NULL,
    PRIMARY KEY (perfil_id, permiso)
);

CREATE TABLE IF NOT EXISTS app_usuario (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password        VARCHAR(100) NOT NULL,
    nombre_completo VARCHAR(100) NOT NULL,
    email           VARCHAR(120),
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    perfil_id       BIGINT       NOT NULL REFERENCES perfil (id),
    creado_en       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS producto (
    id           BIGSERIAL PRIMARY KEY,
    codigo       VARCHAR(30)  NOT NULL UNIQUE,
    nombre       VARCHAR(100) NOT NULL CHECK (char_length(nombre) >= 3),
    categoria    VARCHAR(60)  NOT NULL,
    descripcion  VARCHAR(500),
    unidad       VARCHAR(10)  NOT NULL DEFAULT 'UND',
    stock_actual NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (stock_actual >= 0),
    stock_minimo NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (stock_minimo >= 0),
    estado       VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE TABLE IF NOT EXISTS movimiento (
    id               BIGSERIAL PRIMARY KEY,
    producto_id      BIGINT       NOT NULL REFERENCES producto (id),
    tipo             VARCHAR(10)  NOT NULL CHECK (tipo IN ('ENTRADA', 'SALIDA', 'AJUSTE')),
    motivo           VARCHAR(30)  NOT NULL,
    cantidad         NUMERIC(14,3) NOT NULL CHECK (cantidad > 0),
    variacion        NUMERIC(14,3) NOT NULL,
    stock_anterior   NUMERIC(14,3) NOT NULL CHECK (stock_anterior >= 0),
    stock_resultante NUMERIC(14,3) NOT NULL CHECK (stock_resultante >= 0),
    referencia       VARCHAR(40),
    nota             VARCHAR(300),
    usuario          VARCHAR(50)  NOT NULL,
    fecha            TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS alerta (
    id                BIGSERIAL PRIMARY KEY,
    producto_id       BIGINT       NOT NULL REFERENCES producto (id),
    nivel             VARCHAR(10)  NOT NULL CHECK (nivel IN ('BAJO', 'AGOTADO')),
    estado            VARCHAR(12)  NOT NULL CHECK (estado IN ('ABIERTA', 'RECONOCIDA', 'RESUELTA')),
    stock_al_detectar NUMERIC(14,3) NOT NULL,
    stock_minimo      NUMERIC(14,3) NOT NULL,
    creada_en         TIMESTAMP WITH TIME ZONE NOT NULL,
    reconocida_por    VARCHAR(50),
    reconocida_en     TIMESTAMP WITH TIME ZONE,
    resuelta_en       TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS ix_producto_estado ON producto (estado);
CREATE INDEX IF NOT EXISTS ix_movimiento_producto_fecha ON movimiento (producto_id, fecha);
CREATE INDEX IF NOT EXISTS ix_movimiento_fecha ON movimiento (fecha);
CREATE INDEX IF NOT EXISTS ix_alerta_producto_estado ON alerta (producto_id, estado);

-- Productos de ejemplo: solo si la tabla está vacía.
INSERT INTO producto (codigo, nombre, categoria, descripcion, unidad, stock_actual, stock_minimo, estado)
SELECT v.codigo, v.nombre, v.categoria, v.descripcion, v.unidad, v.stock_actual, v.stock_minimo, v.estado
FROM (VALUES
    ('MAR-016', 'Martillo de uña 16 oz', 'Herramientas manuales', 'Mango de fibra, cabeza de acero forjado', 'UND', 24, 5, 'ACTIVO'),
    ('TAL-650', 'Taladro percutor 650 W', 'Herramientas eléctricas', 'Con maletín y juego de brocas', 'UND', 4, 5, 'ACTIVO'),
    ('CIN-005', 'Cinta métrica 5 m', 'Medición', 'Carcasa antigolpes', 'UND', 40, 10, 'ACTIVO'),
    ('TOR-1PG', 'Tornillo autoperforante 1"', 'Fijación', 'Caja por 100 unidades', 'CAJA', 0, 8, 'ACTIVO'),
    ('PIN-LAT', 'Pintura látex blanca 1 gal', 'Pinturas', 'Interior, alto cubrimiento', 'UND', 12, 6, 'ACTIVO'),
    ('CAB-ELE', 'Cable eléctrico calibre 12', 'Electricidad', 'Se vende por metro', 'M', 180.5, 100, 'ACTIVO'),
    ('CAN-040', 'Candado de bronce 40 mm', 'Seguridad', 'Descontinuado por el proveedor', 'UND', 0, 0, 'INACTIVO')
) AS v (codigo, nombre, categoria, descripcion, unidad, stock_actual, stock_minimo, estado)
WHERE NOT EXISTS (SELECT 1 FROM producto);

-- Cada existencia de ejemplo queda respaldada por su movimiento de stock inicial.
INSERT INTO movimiento (producto_id, tipo, motivo, cantidad, variacion, stock_anterior, stock_resultante, usuario, fecha)
SELECT p.id, 'ENTRADA', 'STOCK_INICIAL', p.stock_actual, p.stock_actual, 0, p.stock_actual, 'sistema', CURRENT_TIMESTAMP
FROM producto p
WHERE p.stock_actual > 0 AND NOT EXISTS (SELECT 1 FROM movimiento);

-- Alertas iniciales para productos activos en o bajo su mínimo.
INSERT INTO alerta (producto_id, nivel, estado, stock_al_detectar, stock_minimo, creada_en)
SELECT p.id, CASE WHEN p.stock_actual = 0 THEN 'AGOTADO' ELSE 'BAJO' END, 'ABIERTA', p.stock_actual, p.stock_minimo, CURRENT_TIMESTAMP
FROM producto p
WHERE p.estado = 'ACTIVO' AND p.stock_minimo > 0 AND p.stock_actual <= p.stock_minimo
  AND NOT EXISTS (SELECT 1 FROM alerta);
