# nombre: build.ps1
# descripcion: Compila back-end y front-end.
# fecha_creacion: 2026-10-07
# actualizacion: 2026-10-07
# autor: Camilo1408
# version: 1.0.0
$ErrorActionPreference = 'Stop'
$raiz = Split-Path $PSScriptRoot -Parent
Push-Location "$raiz/backend"; mvn -B -DskipTests package; if ($LASTEXITCODE) { exit $LASTEXITCODE }; Pop-Location
Push-Location "$raiz/frontend"; npm ci; npm run build; if ($LASTEXITCODE) { exit $LASTEXITCODE }; Pop-Location
Write-Host 'Compilación OK'
