-- nombre: init.sql
-- descripcion: Creación e inicialización de la base de datos PostgreSQL (esquema y productos de ejemplo).
--              Los usuarios admin/user los crea la aplicación al arrancar (contraseñas por variable de entorno).
-- fecha_creacion: 2026-10-07
-- actualizacion: 2026-10-07
-- autor: Camilo1408
-- version: 1.0.0

CREATE TABLE IF NOT EXISTS usuario (
    id        BIGSERIAL PRIMARY KEY,
    username  VARCHAR(50)  NOT NULL UNIQUE,
    password  VARCHAR(100) NOT NULL,
    rol       VARCHAR(10)  NOT NULL CHECK (rol IN ('ADMIN', 'USER'))
);

CREATE TABLE IF NOT EXISTS producto (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL CHECK (char_length(nombre) >= 3),
    categoria   VARCHAR(60)  NOT NULL,
    descripcion VARCHAR(500),
    estado      VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

-- Nombre único sin distinguir mayúsculas (refuerza la regla del servicio).
CREATE UNIQUE INDEX IF NOT EXISTS ux_producto_nombre_ci ON producto (lower(nombre));
CREATE INDEX IF NOT EXISTS ix_producto_estado ON producto (estado);

INSERT INTO producto (nombre, categoria, descripcion, estado) VALUES
    ('Martillo de uña 16 oz', 'Herramientas manuales', 'Mango de fibra, cabeza de acero forjado', 'ACTIVO'),
    ('Taladro percutor 650 W', 'Herramientas eléctricas', 'Con maletín y juego de brocas', 'ACTIVO'),
    ('Cinta métrica 5 m', 'Medición', 'Carcasa antigolpes', 'ACTIVO'),
    ('Tornillo autoperforante 1"', 'Fijación', 'Caja por 100 unidades', 'ACTIVO'),
    ('Pintura látex blanca 1 gal', 'Pinturas', 'Interior, alto cubrimiento', 'ACTIVO'),
    ('Candado de bronce 40 mm', 'Seguridad', 'Descontinuado por el proveedor', 'INACTIVO')
ON CONFLICT DO NOTHING;
