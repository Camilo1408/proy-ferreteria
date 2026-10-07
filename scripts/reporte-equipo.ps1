# nombre: reporte-equipo.ps1
# descripcion: Reporte de desempeño por integrante con datos de GitHub (commits, PRs, issues, líneas).
# fecha_creacion: 2026-10-07
# actualizacion: 2026-10-07
# autor: Camilo1408
# version: 1.0.0
param([int]$Dias = 30, [string]$Repo = 'Camilo1408/proy-ferreteria', [string]$Salida = "$PSScriptRoot/../docs/reporte-equipo.md")
$ErrorActionPreference = 'Stop'
$desde = (Get-Date).AddDays(-$Dias).ToString('yyyy-MM-ddTHH:mm:ssZ')

$commits = gh api "repos/$Repo/commits?since=$desde&per_page=100" --paginate | ConvertFrom-Json
$prs     = gh pr list --repo $Repo --state all --limit 200 --json author,state,createdAt,mergedAt | ConvertFrom-Json
$issues  = gh issue list --repo $Repo --state all --limit 200 --json author,assignees,state,labels,createdAt,closedAt | ConvertFrom-Json

$autores = @{}
function Fila($u) { if (-not $autores.ContainsKey($u)) { $autores[$u] = [ordered]@{ Commits = 0; PRs = 0; PRsFusionados = 0; IssuesAsignados = 0; IssuesCerrados = 0 } }; $autores[$u] }
foreach ($c in $commits) { $u = if ($c.author) { $c.author.login } else { $c.commit.author.name }; (Fila $u).Commits++ }
foreach ($p in $prs) { $f = Fila $p.author.login; $f.PRs++; if ($p.mergedAt) { $f.PRsFusionados++ } }
foreach ($i in $issues) { foreach ($a in $i.assignees) { $f = Fila $a.login; $f.IssuesAsignados++; if ($i.state -eq 'CLOSED') { $f.IssuesCerrados++ } } }

$fallos = ($issues | Where-Object { $_.labels.name -contains 'ci-fallo' }).Count
$md = @("# Reporte de equipo", "", "Periodo: últimos $Dias días · Repositorio: $Repo · Generado: $(Get-Date -Format 'yyyy-MM-dd HH:mm')", "",
        "| Integrante | Commits | PRs | PRs fusionados | Issues asignados | Issues cerrados |", "|---|---|---|---|---|---|")
foreach ($k in ($autores.Keys | Sort-Object)) { $a = $autores[$k]; $md += "| $k | $($a.Commits) | $($a.PRs) | $($a.PRsFusionados) | $($a.IssuesAsignados) | $($a.IssuesCerrados) |" }
$md += "", "Fallos de CI registrados como issue: **$fallos**", "",
       "> Estos conteos son indicadores de actividad, no una medida de calidad ni de esfuerzo. Interprételos junto con la revisión de código y las tareas entregadas."
$md | Set-Content -Encoding UTF8 $Salida
Write-Host "Reporte escrito en $Salida"
