param(
    [switch]$WithPgAdmin
)

$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

$devDir = Join-Path $PSScriptRoot ".dev"

function Get-EnvFileValues {
    param([string]$Path)

    $values = @{}

    if (-not (Test-Path $Path)) {
        return $values
    }

    foreach ($line in Get-Content $Path) {
        $trimmed = $line.Trim()

        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
            continue
        }

        $separatorIndex = $trimmed.IndexOf("=")
        if ($separatorIndex -lt 1) {
            continue
        }

        $key = $trimmed.Substring(0, $separatorIndex).Trim()
        $value = $trimmed.Substring($separatorIndex + 1).Trim()
        $values[$key] = $value
    }

    return $values
}

function Get-ConfigValue {
    param(
        [hashtable]$Values,
        [string]$Key,
        [string]$Default
    )

    if ($Values.ContainsKey($Key) -and -not [string]::IsNullOrWhiteSpace($Values[$Key])) {
        return $Values[$Key]
    }

    return $Default
}

function Get-RunningProcessFromPidFile {
    param([string]$PidFile)

    if (-not (Test-Path $PidFile)) {
        return $null
    }

    $pidValue = (Get-Content $PidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
    if (-not $pidValue) {
        return $null
    }

    try {
        return Get-Process -Id ([int]$pidValue) -ErrorAction Stop
    } catch {
        return $null
    }
}

function Assert-PortAvailableForDockerDb {
    param([int]$Port)

    $listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique

    if (-not $listeners) {
        return
    }

    $blockingProcesses = @()

    foreach ($processId in $listeners) {
        try {
            $process = Get-Process -Id $processId -ErrorAction Stop
        } catch {
            continue
        }

        if ($process.ProcessName -in @("com.docker.backend", "docker-proxy")) {
            continue
        }

        $blockingProcesses += $process
    }

    if (-not $blockingProcesses) {
        return
    }

    $details = $blockingProcesses |
        Sort-Object Id -Unique |
        ForEach-Object { "$($_.ProcessName) (PID $($_.Id))" }

    throw "Configured DB_PORT $Port is already being used by $($details -join ', '). Stop that process or change DB_PORT in .env to an unused port such as 55432, then retry."
}

function Start-DevWindow {
    param(
        [string]$Name,
        [string]$Workdir,
        [hashtable]$EnvVars,
        [string]$Command,
        [string]$PidFile
    )

    $runningProcess = Get-RunningProcessFromPidFile -PidFile $PidFile
    if ($runningProcess) {
        Write-Host "$Name is already running in PID $($runningProcess.Id)." -ForegroundColor Yellow
        return
    }

    $envAssignments = @()
    foreach ($entry in $EnvVars.GetEnumerator()) {
        $escaped = ($entry.Value -replace "'", "''")
        $envAssignments += "`$env:$($entry.Key)='$escaped'"
    }

    $fullCommand = @(
        "Set-Location '$Workdir'"
        $envAssignments
        $Command
    ) -join "; "

    $process = Start-Process powershell `
        -ArgumentList @("-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $fullCommand) `
        -PassThru

    Set-Content -Path $PidFile -Value $process.Id
    Write-Host "Started $Name in a new PowerShell window (PID $($process.Id))." -ForegroundColor Green
}

$envPath = if (Test-Path ".env") { ".env" } else { ".env.example" }
$envValues = Get-EnvFileValues -Path $envPath

$dbName = Get-ConfigValue -Values $envValues -Key "DB_NAME" -Default "activitiesclub"
$dbUser = Get-ConfigValue -Values $envValues -Key "DB_USERNAME" -Default "app"
$dbPassword = Get-ConfigValue -Values $envValues -Key "DB_PASSWORD" -Default "app_password"
$dbPort = Get-ConfigValue -Values $envValues -Key "DB_PORT" -Default "5433"
$backendPort = Get-ConfigValue -Values $envValues -Key "BACKEND_PORT" -Default "8080"
$frontendPort = Get-ConfigValue -Values $envValues -Key "FRONTEND_PORT" -Default "5173"
$jwtSecret = Get-ConfigValue -Values $envValues -Key "JWT_SECRET" -Default ""
$jwtExpirationMs = Get-ConfigValue -Values $envValues -Key "JWT_EXPIRATION_MS" -Default "900000"
$uploadsDir = Get-ConfigValue `
    -Values $envValues `
    -Key "UPLOADS_DIR" `
    -Default (Join-Path $PSScriptRoot "public/uploads")

if (-not [System.IO.Path]::IsPathRooted($uploadsDir)) {
    $uploadsDir = Join-Path $PSScriptRoot $uploadsDir
}

$uploadsDir = [System.IO.Path]::GetFullPath($uploadsDir)

if ([string]::IsNullOrWhiteSpace($jwtSecret) -or $jwtSecret -eq "replace-with-a-local-secret") {
    $jwtSecret = "local-dev-jwt-secret-please-change-123456"
    Write-Host "Using fallback local JWT secret for dev. Set JWT_SECRET in .env to override it." -ForegroundColor Yellow
}

New-Item -ItemType Directory -Force -Path $devDir | Out-Null
New-Item -ItemType Directory -Force -Path $uploadsDir | Out-Null

Assert-PortAvailableForDockerDb -Port ([int]$dbPort)

Write-Host "Starting Postgres in Docker..." -ForegroundColor Cyan
docker compose up -d db | Out-Host

$dbReady = $false
for ($attempt = 0; $attempt -lt 90; $attempt++) {
    docker compose exec -T db pg_isready -U $dbUser -d $dbName *> $null
    if ($LASTEXITCODE -eq 0) {
        $dbReady = $true
        break
    }

    Start-Sleep -Seconds 2
}

if (-not $dbReady) {
    throw "Database did not become ready in time after 180 seconds."
}

if ($WithPgAdmin) {
    Write-Host "Starting pgAdmin in Docker..." -ForegroundColor Cyan
    docker compose up -d pgadmin | Out-Host
}

Start-DevWindow `
    -Name "backend" `
    -Workdir (Join-Path $PSScriptRoot "backend") `
    -EnvVars @{
        "SERVER_PORT" = $backendPort
        "DB_HOST" = "localhost"
        "DB_PORT" = $dbPort
        "DB_NAME" = $dbName
        "DB_USERNAME" = $dbUser
        "DB_PASSWORD" = $dbPassword
        "JWT_SECRET" = $jwtSecret
        "JWT_EXPIRATION_MS" = $jwtExpirationMs
        "UPLOADS_DIR" = $uploadsDir
    } `
    -Command ".\mvnw.cmd spring-boot:run" `
    -PidFile (Join-Path $devDir "backend.pid")

Start-DevWindow `
    -Name "frontend" `
    -Workdir (Join-Path $PSScriptRoot "frontend") `
    -EnvVars @{
        "VITE_API_BASE_URL" = "http://localhost:$backendPort"
    } `
    -Command "npm run dev" `
    -PidFile (Join-Path $devDir "frontend.pid")

Write-Host ""
Write-Host "Dev environment is starting:" -ForegroundColor Cyan
Write-Host "  Frontend: http://localhost:$frontendPort"
Write-Host "  Backend:  http://localhost:$backendPort"
Write-Host "  Database: localhost:$dbPort"
Write-Host "  Uploads:  $uploadsDir"

if ($WithPgAdmin) {
    $pgAdminPort = Get-ConfigValue -Values $envValues -Key "PGADMIN_PORT" -Default "5050"
    Write-Host "  pgAdmin:  http://localhost:$pgAdminPort"
}

Write-Host ""
Write-Host "Use .\dev-stop.ps1 to stop the local app windows and the database container." -ForegroundColor Cyan
