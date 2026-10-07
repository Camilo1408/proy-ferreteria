# nombre: test.ps1
# descripcion: Ejecuta todas las pruebas: back-end (JUnit, PIT), front-end (Vitest) y E2E (Playwright, Selenium).
#              Con -Estres añade la prueba de carga k6. Requiere haber ejecutado build.ps1.
# fecha_creacion: 2026-10-07
# actualizacion: 2026-10-07
# autor: Camilo1408
# version: 1.0.0
param([switch]$Estres)
$ErrorActionPreference = 'Stop'
$raiz = Split-Path $PSScriptRoot -Parent
function Paso($dir, [scriptblock]$cmd) { Push-Location "$raiz/$dir"; try { & $cmd; if ($LASTEXITCODE) { throw "Falló en $dir" } } finally { Pop-Location } }
Paso 'backend'   { mvn -B verify }
Paso 'backend'   { mvn -B org.pitest:pitest-maven:mutationCoverage }
Paso 'frontend'  { npm test }
Paso 'tests/e2e' { npx playwright test }
Paso 'tests/e2e' { npm run test:selenium }
if ($Estres) { Paso 'tests/k6' { k6 run estres.js } }
Write-Host 'Todas las pruebas OK'
