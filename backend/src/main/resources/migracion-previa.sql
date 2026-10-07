-- nombre: migracion-previa.sql
-- descripcion: Migración idempotente que se ejecuta ANTES de que Hibernate actualice el esquema.
--              Lleva una base creada con la primera versión (productos sin código ni existencias, usuarios con rol)
--              al esquema actual sin perder productos. En una base nueva no hace nada.
-- fecha_creacion: 2026-10-08
-- actualizacion: 2026-10-08
-- autor: Camilo1408
-- version: 1.1.0

-- La tabla de usuarios de la versión anterior (con rol) la elimina MigracionDatos solo si realmente es esa tabla;
-- los usuarios se recrean con perfiles al arrancar (DataSeeder).

-- Productos: nuevas columnas con valores por defecto para las filas existentes.
ALTER TABLE IF EXISTS producto ADD COLUMN IF NOT EXISTS codigo VARCHAR(30);
ALTER TABLE IF EXISTS producto ADD COLUMN IF NOT EXISTS unidad VARCHAR(10) NOT NULL DEFAULT 'UND';
ALTER TABLE IF EXISTS producto ADD COLUMN IF NOT EXISTS stock_actual NUMERIC(14,3) NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS producto ADD COLUMN IF NOT EXISTS stock_minimo NUMERIC(14,3) NOT NULL DEFAULT 0;
-- El código de los productos existentes se completa después, en MigracionDatos (la tabla puede no existir aún).
