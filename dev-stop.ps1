$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

$devDir = Join-Path $PSScriptRoot ".dev"

function Stop-ProcessFromPidFile {
    param(
        [string]$Name,
        [string]$PidFile
    )

    if (-not (Test-Path $PidFile)) {
        return
    }

    $pidValue = (Get-Content $PidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
    if (-not $pidValue) {
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
        return
    }

    try {
        Stop-Process -Id ([int]$pidValue) -Force -ErrorAction Stop
        Write-Host "Stopped $Name (PID $pidValue)." -ForegroundColor Green
    } catch {
        Write-Host "$Name was not running." -ForegroundColor Yellow
    }

    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
}

Stop-ProcessFromPidFile -Name "backend" -PidFile (Join-Path $devDir "backend.pid")
Stop-ProcessFromPidFile -Name "frontend" -PidFile (Join-Path $devDir "frontend.pid")

Write-Host "Stopping Docker database services..." -ForegroundColor Cyan
docker compose stop db pgadmin | Out-Host
