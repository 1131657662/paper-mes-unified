param(
    [string]$Database = "paper_mes_migration_recovery_test_20260802"
)

$ErrorActionPreference = "Stop"
if ($Database -notmatch '^paper_mes_migration_recovery_test(?:_[0-9]+)?$') {
    throw "Unsafe migration recovery test database name: $Database"
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$runner = Join-Path $repoRoot "deploy\apply-paper-mes-migrations.example.sh"
$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
$gitBash = Join-Path ${env:ProgramFiles} "Git\bin\bash.exe"
if (-not (Test-Path -LiteralPath $gitBash)) {
    throw "Git Bash is required for the migration recovery test: $gitBash"
}
$bash = $gitBash
$dbHost = if ($env:PAPER_MES_IT_DB_HOST) { $env:PAPER_MES_IT_DB_HOST } else { "127.0.0.1" }
$dbPort = if ($env:PAPER_MES_IT_DB_PORT) { $env:PAPER_MES_IT_DB_PORT } else { "3306" }
$dbUser = if ($env:PAPER_MES_IT_DB_USERNAME) { $env:PAPER_MES_IT_DB_USERNAME } else { "root" }
$dbPassword = $env:PAPER_MES_IT_DB_PASSWORD
if (-not $dbPassword) {
    throw "Set PAPER_MES_IT_DB_PASSWORD before running the migration recovery test"
}

$tempRoot = Join-Path ([IO.Path]::GetTempPath()) ("paper-mes-migration-recovery-" + [Guid]::NewGuid().ToString("N"))
$migrationDirectory = Join-Path $tempRoot "migrations"
New-Item -ItemType Directory -Path $migrationDirectory -Force | Out-Null

function ConvertTo-BashPath {
    param([string]$Path)
    if ($Path -notmatch '^([A-Za-z]):\\(.*)$') { throw "Expected a Windows absolute path: $Path" }
    return "/$($matches[1].ToLowerInvariant())/$($matches[2] -replace '\\', '/')"
}

$runnerForBash = ConvertTo-BashPath $runner
$migrationDirectoryForBash = ConvertTo-BashPath $migrationDirectory
$migrationEnvFile = Join-Path $tempRoot "migration.env"

function Quote-BashValue {
    param([string]$Value)
    return "'" + ($Value -replace "'", "'\\''") + "'"
}

$lockName = "paper_mes_${Database}_migration"
$migrationEnvLines = @(
    "DB_HOST=$(Quote-BashValue $dbHost)",
    "DB_PORT=$(Quote-BashValue $dbPort)",
    "DB_NAME=$(Quote-BashValue $Database)",
    "DB_USER=$(Quote-BashValue $dbUser)",
    "DB_PASSWORD=$(Quote-BashValue $dbPassword)",
    "MIGRATION_DIR=$(Quote-BashValue $migrationDirectoryForBash)",
    "MIGRATION_BASELINE='0'",
    "MIGRATION_LOCK_NAME=$(Quote-BashValue $lockName)"
)
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllText($migrationEnvFile, ($migrationEnvLines -join "`n") + "`n", $utf8NoBom)
Set-Content -LiteralPath (Join-Path $migrationDirectory "V1.0__create_recovery_table.sql") `
    -Encoding UTF8 -Value "CREATE TABLE migration_recovery_ok (id INT NOT NULL PRIMARY KEY);"
Set-Content -LiteralPath (Join-Path $migrationDirectory "V2.0__create_conflicting_table.sql") `
    -Encoding UTF8 -Value "CREATE TABLE migration_recovery_conflict (id INT NOT NULL PRIMARY KEY);"

$created = $false
$environmentNames = @(
    "DB_HOST", "DB_PORT", "DB_NAME", "DB_USER", "DB_PASSWORD", "MIGRATION_DIR",
    "MIGRATION_ENV_FILE", "MIGRATION_BASELINE", "MIGRATION_RETRY_FAILED", "MIGRATION_LOCK_NAME"
)
$previousEnvironment = @{}
foreach ($name in $environmentNames) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name)
}
$previousMysqlPassword = $env:MYSQL_PWD
$env:MYSQL_PWD = $dbPassword

function Invoke-MySql {
    param([string]$Sql, [string]$TargetDatabase = "")
    $arguments = @("--host=$dbHost", "--port=$dbPort", "--user=$dbUser", "--batch", "--skip-column-names")
    if ($TargetDatabase) { $arguments += $TargetDatabase }
    $output = $Sql | & $mysql @arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($output -join "`n") }
    return @($output)
}

$migrationEnvForBash = ConvertTo-BashPath $migrationEnvFile

function Invoke-BashRunner {
    param([string]$RetryFailed)
    $command = "MIGRATION_ENV_FILE=$(Quote-BashValue $migrationEnvForBash) MIGRATION_RETRY_FAILED=$RetryFailed bash $(Quote-BashValue $runnerForBash)"
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $output = (& $bash -lc $command 2>&1) -join "`n"
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousPreference
    return [PSCustomObject]@{ Output = $output; ExitCode = $exitCode }
}

try {
    $exists = Invoke-MySql "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$Database';"
    if ([int]$exists -ne 0) { throw "Migration recovery test database already exists; refusing to overwrite it" }
    Invoke-MySql "CREATE DATABASE ``$Database`` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" | Out-Null
    $created = $true
    Invoke-MySql "CREATE TABLE migration_recovery_conflict (id INT NOT NULL PRIMARY KEY);" $Database | Out-Null

    $firstResult = Invoke-BashRunner "0"
    $firstOutput = $firstResult.Output
    $firstExit = $firstResult.ExitCode
    if ($firstExit -eq 0) { throw "Migration runner unexpectedly accepted the DDL conflict" }
    $firstState = (Invoke-MySql "SELECT CONCAT(status, '|', execution_type) FROM sys_schema_migration WHERE version='2.0';" $Database) -join ""
    if ($firstState -ne "failed|applied") { throw "Expected failed migration state, got: $firstState`n$firstOutput" }

    $retryBlockedResult = Invoke-BashRunner "0"
    $retryBlockedOutput = $retryBlockedResult.Output
    $retryBlockedExit = $retryBlockedResult.ExitCode
    if ($retryBlockedExit -eq 0 -or $retryBlockedOutput -notmatch "MIGRATION_RETRY_FAILED=1") {
        throw "Failed migration was retried without explicit opt-in"
    }

    Invoke-MySql "DROP TABLE migration_recovery_conflict;" $Database | Out-Null
    $recoveryResult = Invoke-BashRunner "1"
    $recoveryOutput = $recoveryResult.Output
    if ($recoveryResult.ExitCode -ne 0) { throw "Migration recovery retry failed: $recoveryOutput" }
    $recoveredState = (Invoke-MySql "SELECT CONCAT(status, '|', execution_type) FROM sys_schema_migration WHERE version='2.0';" $Database) -join ""
    if ($recoveredState -ne "applied|applied") { throw "Expected applied migration state, got: $recoveredState" }

    Write-Output "migration failure recovery guard passed"
}
finally {
    if ($created) {
        try { Invoke-MySql "DROP DATABASE ``$Database``;" | Out-Null } catch { Write-Warning $_.Exception.Message }
    }
    foreach ($name in $environmentNames) {
        if ($null -eq $previousEnvironment[$name]) {
            Remove-Item "Env:$name" -ErrorAction SilentlyContinue
        } else {
            Set-Item "Env:$name" $previousEnvironment[$name]
        }
    }
    $env:MYSQL_PWD = $previousMysqlPassword
    Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
}
