param(
    [string]$Database = "paper_mes_desktop_test",
    [int]$BackendPort = 18085,
    [int]$FrontendPort = 5177
)

$ErrorActionPreference = "Stop"
if ($Database -notmatch '^paper_mes_desktop_test(?:_[0-9]+)?$') {
    throw "Unsafe desktop acceptance database name: $Database"
}
foreach ($port in @($BackendPort, $FrontendPort)) {
    if ($port -lt 1024 -or $port -gt 65535) { throw "Unsafe acceptance port: $port" }
    if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) {
        throw "Desktop acceptance port is already in use: $port"
    }
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$frontendRoot = Join-Path $repoRoot "frontend"
$jar = Join-Path $repoRoot "target\paper-mes-0.0.1-SNAPSHOT.jar"
$schema = Join-Path $repoRoot "sql\01_schema_v4.1.sql"
$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
$java = (Get-Command java.exe -ErrorAction Stop).Source
$npm = (Get-Command npm.cmd -ErrorAction Stop).Source
foreach ($required in @($jar, $schema)) {
    if (-not (Test-Path -LiteralPath $required)) { throw "Required file not found: $required" }
}

$dbHost = if ($env:PAPER_MES_IT_DB_HOST) { $env:PAPER_MES_IT_DB_HOST } else { "127.0.0.1" }
$dbPort = if ($env:PAPER_MES_IT_DB_PORT) { $env:PAPER_MES_IT_DB_PORT } else { "3306" }
$dbUser = if ($env:PAPER_MES_IT_DB_USERNAME) { $env:PAPER_MES_IT_DB_USERNAME } else { "root" }
$dbPassword = $env:PAPER_MES_IT_DB_PASSWORD
if (-not $dbPassword) { throw "Set PAPER_MES_IT_DB_PASSWORD before desktop acceptance" }

$adminPassword = "Aa9!" + [Guid]::NewGuid().ToString("N").Substring(0, 16)
$operatorPassword = "Bb8!" + [Guid]::NewGuid().ToString("N").Substring(0, 16)
$environmentNames = @(
    "SPRING_PROFILES_ACTIVE", "SERVER_PORT", "SPRING_DATASOURCE_URL",
    "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD",
    "APP_SCHEMA_BOOTSTRAP_ENABLED", "APP_INITIAL_ADMIN_PASSWORD",
    "APP_INITIAL_OPERATOR_PASSWORD", "PAPER_MES_BACKUP_ENABLED",
    "PAPER_MES_DATA_HEALTH_INITIAL_DELAY_MS", "VITE_API_PROXY_TARGET",
    "PAPER_MES_E2E_BASE_URL", "PAPER_MES_E2E_USERNAME", "PAPER_MES_E2E_PASSWORD",
    "PAPER_MES_E2E_LIMITED_USERNAME", "PAPER_MES_E2E_LIMITED_PASSWORD", "MYSQL_PWD"
)
$previousEnvironment = @{}
foreach ($name in $environmentNames) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, "Process")
}

$backend = $null
$vite = $null
$databaseCreated = $false
$testPassed = $false
$logs = @(
    (Join-Path $repoRoot "target\desktop-e2e-backend.stdout.log"),
    (Join-Path $repoRoot "target\desktop-e2e-backend.stderr.log"),
    (Join-Path $repoRoot "target\desktop-e2e-vite.stdout.log"),
    (Join-Path $repoRoot "target\desktop-e2e-vite.stderr.log")
)

function Invoke-MySql([string]$Sql, [string]$TargetDatabase = "") {
    $arguments = @("--host=$dbHost", "--port=$dbPort", "--user=$dbUser", "--batch", "--skip-column-names")
    if ($TargetDatabase) { $arguments += $TargetDatabase }
    $output = $Sql | & $mysql @arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($output -join "`n") }
    return $output
}

function Wait-ForUrl([string]$Url, [System.Diagnostics.Process]$Process, [int]$Attempts) {
    for ($attempt = 0; $attempt -lt $Attempts; $attempt++) {
        if ($Process.HasExited) { throw "Acceptance service exited before becoming ready: $Url" }
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -eq 200) { return }
        } catch { }
        Start-Sleep -Seconds 1
    }
    throw "Acceptance service readiness timed out: $Url"
}

function Stop-AcceptanceProcess([System.Diagnostics.Process]$Process, [int]$Port) {
    $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($listener) {
        Stop-Process -Id $listener.OwningProcess -Force -ErrorAction SilentlyContinue
        Wait-Process -Id $listener.OwningProcess -Timeout 5 -ErrorAction SilentlyContinue
    }
    if ($Process -and -not $Process.HasExited) {
        Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
    }
}

try {
    $env:MYSQL_PWD = $dbPassword
    $exists = Invoke-MySql "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$Database';"
    if ([int]$exists -ne 0) { throw "Desktop acceptance database already exists; refusing overwrite" }
    Invoke-MySql "CREATE DATABASE ``$Database`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" | Out-Null
    $databaseCreated = $true
    $schemaSql = Get-Content -LiteralPath $schema -Raw -Encoding UTF8
    Invoke-MySql $schemaSql $Database | Out-Null

    $env:SPRING_PROFILES_ACTIVE = "dev"
    $env:SERVER_PORT = "$BackendPort"
    $env:SPRING_DATASOURCE_URL = "jdbc:mysql://${dbHost}:${dbPort}/${Database}?useSSL=false&useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true"
    $env:SPRING_DATASOURCE_USERNAME = $dbUser
    $env:SPRING_DATASOURCE_PASSWORD = $dbPassword
    $env:APP_SCHEMA_BOOTSTRAP_ENABLED = "true"
    $env:APP_INITIAL_ADMIN_PASSWORD = $adminPassword
    $env:APP_INITIAL_OPERATOR_PASSWORD = $operatorPassword
    $env:PAPER_MES_BACKUP_ENABLED = "false"
    $env:PAPER_MES_DATA_HEALTH_INITIAL_DELAY_MS = "3600000"
    $backend = Start-Process -FilePath $java -ArgumentList @("-jar", $jar) -WorkingDirectory $repoRoot `
        -RedirectStandardOutput $logs[0] -RedirectStandardError $logs[1] -WindowStyle Hidden -PassThru
    Wait-ForUrl "http://127.0.0.1:$BackendPort/actuator/health" $backend 60

    $env:VITE_API_PROXY_TARGET = "http://127.0.0.1:$BackendPort"
    $vite = Start-Process -FilePath $npm -ArgumentList @(
        "run", "dev", "--", "--host", "127.0.0.1", "--port", "$FrontendPort", "--strictPort"
    ) -WorkingDirectory $frontendRoot -RedirectStandardOutput $logs[2] `
        -RedirectStandardError $logs[3] -WindowStyle Hidden -PassThru
    Wait-ForUrl "http://127.0.0.1:$FrontendPort/login" $vite 45

    $env:PAPER_MES_E2E_BASE_URL = "http://127.0.0.1:$FrontendPort"
    $env:PAPER_MES_E2E_USERNAME = "admin"
    $env:PAPER_MES_E2E_PASSWORD = $adminPassword
    $env:PAPER_MES_E2E_LIMITED_USERNAME = "operator"
    $env:PAPER_MES_E2E_LIMITED_PASSWORD = $operatorPassword
    Push-Location $frontendRoot
    try { & $npm run test:e2e -- --workers=1 } finally { Pop-Location }
    if ($LASTEXITCODE -ne 0) { throw "Desktop acceptance E2E failed: $LASTEXITCODE" }
    $testPassed = $true
    Write-Output "desktop acceptance passed at 1366, 1440 and 1920"
} finally {
    Stop-AcceptanceProcess $vite $FrontendPort
    Stop-AcceptanceProcess $backend $BackendPort
    if ($databaseCreated) {
        $env:MYSQL_PWD = $dbPassword
        Invoke-MySql "DROP DATABASE ``$Database``;" | Out-Null
    }
    foreach ($name in $environmentNames) {
        if ($null -eq $previousEnvironment[$name]) {
            Remove-Item "Env:$name" -ErrorAction SilentlyContinue
        } else {
            Set-Item "Env:$name" $previousEnvironment[$name]
        }
    }
    if ($testPassed) { Remove-Item -LiteralPath $logs -Force -ErrorAction SilentlyContinue }
}
