param(
    [string]$Database = "paper_processing_prod_smoke_test",
    [int]$Port = 18081,
    [string]$JarPath = ""
)

$ErrorActionPreference = "Stop"
if ($Database -notmatch '^paper_processing_prod_smoke_test(?:_[0-9]+)?$') {
    throw "Unsafe production smoke database name: $Database"
}
if ($Port -lt 1024 -or $Port -gt 65535) { throw "Unsafe production smoke port: $Port" }
if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) {
    throw "Production smoke port is already in use: $Port"
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$jar = if ($JarPath) {
    (Resolve-Path -LiteralPath $JarPath).Path
} else {
    Join-Path $repoRoot "target\paper-mes-0.0.1-SNAPSHOT.jar"
}
$schema = Join-Path $repoRoot "sql\01_schema_v4.1.sql"
$migrationDirectory = Join-Path $repoRoot "sql"
$prodConfig = Join-Path $repoRoot "src\main\resources\application-prod.example.yml"
$stdoutLog = Join-Path $repoRoot "target\prod-smoke.stdout.log"
$stderrLog = Join-Path $repoRoot "target\prod-smoke.stderr.log"
$appLog = Join-Path $repoRoot "target\prod-smoke.app.log"
foreach ($required in @($jar, $schema, $prodConfig)) {
    if (-not (Test-Path -LiteralPath $required)) { throw "Required file not found: $required" }
}
foreach ($log in @($stdoutLog, $stderrLog, $appLog)) {
    Remove-Item -LiteralPath $log -Force -ErrorAction SilentlyContinue
}

$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
$java = (Get-Command java.exe -ErrorAction Stop).Source
$dbHost = if ($env:PAPER_MES_IT_DB_HOST) { $env:PAPER_MES_IT_DB_HOST } else { "127.0.0.1" }
$dbPort = if ($env:PAPER_MES_IT_DB_PORT) { $env:PAPER_MES_IT_DB_PORT } else { "3306" }
$dbUser = if ($env:PAPER_MES_IT_DB_USERNAME) { $env:PAPER_MES_IT_DB_USERNAME } else { "root" }
$dbPassword = $env:PAPER_MES_IT_DB_PASSWORD
if (-not $dbPassword) { throw "Set PAPER_MES_IT_DB_PASSWORD before running the production smoke test" }

$process = $null
$databaseCreated = $false
$testPassed = $false
$failureMessage = $null
$testAdminPassword = "Aa9!" + [Guid]::NewGuid().ToString("N").Substring(0, 16)
$testOperatorPassword = "Bb8!" + [Guid]::NewGuid().ToString("N").Substring(0, 16)
$environmentNames = @(
    "SPRING_PROFILES_ACTIVE", "SPRING_CONFIG_ADDITIONAL_LOCATION", "SERVER_PORT",
    "PAPER_MES_DB_URL", "PAPER_MES_DB_USER", "PAPER_MES_DB_PASSWORD",
    "PAPER_MES_EXPECTED_SCHEMA_VERSION", "PAPER_MES_BACKEND_VERSION",
    "PAPER_MES_FRONTEND_VERSION", "PAPER_MES_GIT_SHA", "PAPER_MES_BUILD_TIME",
    "PAPER_MES_BACKUP_ENABLED", "PAPER_MES_DATA_HEALTH_INITIAL_DELAY_MS",
    "APP_INITIAL_ADMIN_PASSWORD", "APP_INITIAL_OPERATOR_PASSWORD", "LOGGING_FILE_NAME"
)
$previousEnvironment = @{}
foreach ($name in $environmentNames) { $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name) }
$previousMysqlPassword = $env:MYSQL_PWD
$env:MYSQL_PWD = $dbPassword

function Invoke-MySql {
    param([string]$Sql, [string]$TargetDatabase = "")
    $arguments = @("--host=$dbHost", "--port=$dbPort", "--user=$dbUser", "--batch", "--skip-column-names")
    if ($TargetDatabase) { $arguments += $TargetDatabase }
    $preference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $output = $Sql | & $mysql @arguments 2>&1
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $preference
    if ($exitCode -ne 0) { throw ($output -join "`n") }
    return $output
}

function Apply-Schema {
    $arguments = @("--host=$dbHost", "--port=$dbPort", "--user=$dbUser", "--batch", $Database)
    $preference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $output = Get-Content -Raw -Encoding UTF8 $schema | & $mysql @arguments 2>&1
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $preference
    if ($exitCode -ne 0) { throw ($output -join "`n") }
}

function Apply-PendingMigrations {
    # The current canonical baseline is 3.62. The smoke database replays the last
    # upgrade window (3.49 -> current) so migrations are executed instead of
    # being registered as a fabricated baseline.  Update this fixture when a
    # release introduces a new canonical baseline.
    $baselineVersion = if ($env:PAPER_MES_SMOKE_BASELINE_VERSION) { $env:PAPER_MES_SMOKE_BASELINE_VERSION } else { "3.49" }
    if ($baselineVersion -notmatch '^[0-9]+(?:\.[0-9]+)*$') { throw "Unsafe smoke baseline version: $baselineVersion" }
    $migrationFiles = Get-ChildItem -LiteralPath $migrationDirectory -Filter "V*.sql" -File |
        Sort-Object { [version]([regex]::Match($_.Name, '^V([0-9]+(?:\.[0-9]+)*)__').Groups[1].Value) } |
        Where-Object {
            $version = [version]([regex]::Match($_.Name, '^V([0-9]+(?:\.[0-9]+)*)__').Groups[1].Value)
            $version -gt [version]$baselineVersion
        }
    if (-not $migrationFiles) { throw "No pending migration files found after baseline $baselineVersion" }
    Invoke-MySql @'
CREATE TABLE IF NOT EXISTS sys_schema_migration (
  version VARCHAR(50) NOT NULL,
  script_name VARCHAR(255) NOT NULL,
  checksum CHAR(64) NOT NULL,
  execution_type VARCHAR(20) NOT NULL DEFAULT 'applied',
  status VARCHAR(20) NOT NULL DEFAULT 'applied',
  failure_message TEXT NULL,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
'@ $Database | Out-Null
    foreach ($migration in $migrationFiles) {
        $match = [regex]::Match($migration.Name, '^V([0-9]+(?:\.[0-9]+)*)__')
        $version = $match.Groups[1].Value
        $checksum = (Get-FileHash -LiteralPath $migration.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        $escapedName = $migration.Name.Replace("'", "''")
        Invoke-MySql "INSERT INTO sys_schema_migration (version,script_name,checksum,execution_type,status,started_at) VALUES ('$version','$escapedName','$checksum','applied','running',NOW()) ON DUPLICATE KEY UPDATE checksum=VALUES(checksum),status='running',started_at=NOW(),failure_message=NULL;" $Database | Out-Null
        try {
            $sql = Get-Content -Raw -Encoding UTF8 $migration.FullName
            Invoke-MySql $sql $Database | Out-Null
            Invoke-MySql "UPDATE sys_schema_migration SET status='applied',finished_at=NOW(),executed_at=NOW() WHERE version='$version';" $Database | Out-Null
        } catch {
            $message = $_.Exception.Message.Replace("'", "''")
            Invoke-MySql "UPDATE sys_schema_migration SET status='failed',failure_message='$message',finished_at=NOW() WHERE version='$version';" $Database | Out-Null
            throw
        }
    }
}

function Wait-ForHealth {
    $healthUrl = "http://127.0.0.1:$Port/actuator/health"
    for ($attempt = 0; $attempt -lt 45; $attempt++) {
        if ($process.HasExited) { throw "Production smoke backend exited before becoming healthy" }
        try {
            $health = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 2
            if ($health.status -eq "UP") { return $health }
        } catch { }
        Start-Sleep -Seconds 1
    }
    throw "Production smoke backend health check timed out"
}

function Assert-HttpStatus {
    param([string]$Url, [int]$Expected)
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        $actual = [int]$response.StatusCode
    } catch {
        $actual = [int]$_.Exception.Response.StatusCode
    }
    if ($actual -ne $Expected) { throw "Expected HTTP $Expected from $Url, got $actual" }
}

try {
    $exists = Invoke-MySql "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$Database';"
    if ([int]$exists -ne 0) { throw "Production smoke database already exists; refusing to overwrite it" }
    Invoke-MySql "CREATE DATABASE ``$Database`` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
    $databaseCreated = $true
    Apply-Schema
    Apply-PendingMigrations
    $migrationCount = Invoke-MySql "SELECT COUNT(*) FROM sys_schema_migration WHERE status='applied';" $Database
    if ([int]$migrationCount -lt 1) { throw "Production smoke did not execute a pending migration" }
    $pendingSchema = Invoke-MySql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$Database' AND table_name='rpt_report_query_snapshot';"
    if ([int]$pendingSchema -ne 1) { throw "Production smoke migration did not create rpt_report_query_snapshot" }

    $configUri = ([Uri]$prodConfig).AbsoluteUri
    $env:SPRING_PROFILES_ACTIVE = "prod"
    $env:SPRING_CONFIG_ADDITIONAL_LOCATION = $configUri
    $env:SERVER_PORT = "$Port"
    $env:PAPER_MES_DB_URL = "jdbc:mysql://${dbHost}:${dbPort}/${Database}?useSSL=false&useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true"
    $env:PAPER_MES_DB_USER = $dbUser
    $env:PAPER_MES_DB_PASSWORD = $dbPassword
    $env:PAPER_MES_EXPECTED_SCHEMA_VERSION = "3.62"
    $env:PAPER_MES_BACKEND_VERSION = "prod-smoke-backend"
    $env:PAPER_MES_FRONTEND_VERSION = "prod-smoke-frontend"
    $env:PAPER_MES_GIT_SHA = "0000000000000000000000000000000000000000"
    $env:PAPER_MES_BUILD_TIME = "2026-08-07T00:00:00Z"
    $env:PAPER_MES_BACKUP_ENABLED = "false"
    $env:PAPER_MES_DATA_HEALTH_INITIAL_DELAY_MS = "3600000"
    $env:APP_INITIAL_ADMIN_PASSWORD = $testAdminPassword
    $env:APP_INITIAL_OPERATOR_PASSWORD = $testOperatorPassword
    $env:LOGGING_FILE_NAME = $appLog

    $process = Start-Process -FilePath $java -ArgumentList @("-jar", $jar) -WorkingDirectory $repoRoot `
        -RedirectStandardOutput $stdoutLog -RedirectStandardError $stderrLog -WindowStyle Hidden -PassThru
    $health = Wait-ForHealth
    if ($health.PSObject.Properties.Name -contains "components") { throw "Health endpoint exposed component details" }

    $listener = Get-NetTCPConnection -State Listen -LocalPort $Port | Select-Object -First 1
    if ($listener.LocalAddress -ne "127.0.0.1") { throw "Production backend is not bound to loopback" }
    Assert-HttpStatus "http://127.0.0.1:$Port/actuator/env" 404
    Assert-HttpStatus "http://127.0.0.1:$Port/api/auth/me" 401

    $loginBody = @{ username = "admin"; password = $testAdminPassword } | ConvertTo-Json
    $login = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/api/auth/login" -Method Post `
        -ContentType "application/json" -Body $loginBody -UseBasicParsing -TimeoutSec 5
    $cookie = [string]$login.Headers["Set-Cookie"]
    foreach ($flag in @("Secure", "HttpOnly", "SameSite=Strict")) {
        if ($cookie -notmatch $flag) { throw "Production login cookie is missing $flag" }
    }

    $users = Invoke-MySql "SELECT COUNT(*) FROM sys_user WHERE is_deleted=0;" $Database
    if ([int]$users -ne 2) { throw "Production empty-database bootstrap did not create two initial users" }
    $testPassed = $true
    Write-Output "production profile startup smoke test passed"
}
catch {
    $failureMessage = $_.Exception.Message
    Write-Output "production profile startup smoke test failed: $failureMessage"
}
finally {
    $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    if ($listener) {
        $smokePid = $listener.OwningProcess
        $smokeProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$smokePid"
        $smokeJarName = [IO.Path]::GetFileName($jar)
        if ($smokeProcess.CommandLine -like "*$smokeJarName*") {
            Stop-Process -Id $smokePid -Force -ErrorAction SilentlyContinue
            Wait-Process -Id $smokePid -Timeout 5 -ErrorAction SilentlyContinue
        }
    }
    if ($databaseCreated) { Invoke-MySql "DROP DATABASE ``$Database``;" | Out-Null }
    foreach ($name in $environmentNames) {
        if ($null -eq $previousEnvironment[$name]) {
            Remove-Item "Env:$name" -ErrorAction SilentlyContinue
        } else {
            Set-Item "Env:$name" $previousEnvironment[$name]
        }
    }
    $env:MYSQL_PWD = $previousMysqlPassword
    if ($testPassed) {
        foreach ($log in @($stdoutLog, $stderrLog, $appLog)) {
            Remove-Item -LiteralPath $log -Force -ErrorAction SilentlyContinue
        }
    }
}
if ($failureMessage) { throw $failureMessage }
