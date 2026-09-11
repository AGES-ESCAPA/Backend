# Sobe o docker compose escolhendo a primeira porta livre para o backend.
#
# O Docker Compose nao tem fallback de porta: se a 8080 estiver ocupada, a subida
# falha. Este script testa 8080, 8081, ... e define SERVER_PORT antes de
# chamar o compose. Argumentos extras sao repassados (ex.: .\scripts\dev-up.ps1 --build).
#
# Uso (PowerShell):
#   .\scripts\dev-up.ps1            # sobe tudo
#   .\scripts\dev-up.ps1 --build    # reconstroi a imagem antes

$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

$firstPort = if ($env:SERVER_PORT) { [int]$env:SERVER_PORT } else { 8080 }
$lastPort = $firstPort + 10

function Test-PortBusy([int]$port) {
    $listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    return ($null -ne $listener)
}

$chosen = $null
for ($p = $firstPort; $p -le $lastPort; $p++) {
    if (-not (Test-PortBusy $p)) { $chosen = $p; break }
}

if ($null -eq $chosen) {
    Write-Error "Nenhuma porta livre entre $firstPort e $lastPort."
    exit 1
}

if ($chosen -ne $firstPort) {
    Write-Host "Porta $firstPort ocupada. Subindo o backend na $chosen. Aponte o frontend para ela."
}

$env:SERVER_PORT = "$chosen"
docker compose up -d @args
Write-Host ""
Write-Host "Backend:  http://localhost:$chosen"
Write-Host "Swagger:  http://localhost:$chosen/swagger-ui/index.html"
