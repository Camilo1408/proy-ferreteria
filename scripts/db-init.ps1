# nombre: db-init.ps1
# descripcion: Crea e inicializa la base de datos PostgreSQL local (contenedor Docker) con db/init.sql.
#              Con -Render aplica el script a la base de datos de Render usando la CLI (render psql).
# fecha_creacion: 2026-10-07
# actualizacion: 2026-10-07
# autor: Camilo1408
# version: 1.0.0
param([switch]$Render, [string]$RenderDbId = 'dpg-db2ueocs728c73anbqh0-a')
$ErrorActionPreference = 'Stop'
$raiz = Split-Path $PSScriptRoot -Parent
if ($Render) {
  render psql $RenderDbId --confirm -c (Get-Content "$raiz/db/init.sql" -Raw)
} else {
  docker compose -f "$raiz/docker-compose.yml" up -d db
  Write-Host 'PostgreSQL arriba en localhost:5432 (db/init.sql se aplica al crear el volumen).'
}
