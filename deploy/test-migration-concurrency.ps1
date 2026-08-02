param(
    [string]$Database = "paper_mes_migration_lock_test"
)

$ErrorActionPreference = "Stop"
if ($Database -notmatch '^paper_mes_migration_lock_test(?:_[0-9]+)?$') {
    throw "Unsafe migration concurrency test database name: $Database"
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$runner = Join-Path $repoRoot "deploy\apply-paper-mes-migrations.example.sh"
$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
$bash = Join-Path ${env:ProgramFiles} "Git\bin\bash.exe"
if (-not (Test-Path -LiteralPath $bash)) { throw "Git Bash is required: $bash" }

$dbHost = if ($env:PAPER_MES_IT_DB_HOST) { $env:PAPER_MES_IT_DB_HOST } else { "127.0.0.1" }
$dbPort = if ($env:PAPER_MES_IT_DB_PORT) { $env:PAPER_MES_IT_DB_PORT } else { "3306" }
$dbUser = if ($env:PAPER_MES_IT_DB_USERNAME) { $env:PAPER_MES_IT_DB_USERNAME } else { "root" }
$dbPassword = $env:PAPER_MES_IT_DB_PASSWORD
if (-not $dbPassword) { throw "Set PAPER_MES_IT_DB_PASSWORD before running the concurrency test" }

$tempBase = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$tempRoot = Join-Path $tempBase ("paper-mes-migration-lock-" + [Guid]::NewGuid().ToString("N"))
$migrationDirectory = Join-Path $tempRoot "migrations"
$envFile = Join-Path $tempRoot "migration.env"
$lockName = "paper_mes_migration_concurrency_test"
$created = $false
$first = $null
$second = $null
$previousMysqlPassword = $env:MYSQL_PWD
$env:MYSQL_PWD = $dbPassword

function ConvertTo-BashPath {
    param([string]$Path)
    if ($Path -notmatch '^([A-Za-z]):\\(.*)$') { throw "Expected a Windows absolute path: $Path" }
    return "/$($matches[1].ToLowerInvariant())/$($matches[2] -replace '\\', '/')"
}

function Quote-BashValue {
    param([string]$Value)
    return "'" + ($Value -replace "'", "'\''") + "'"
}

function Invoke-MySql {
    param([string]$Sql, [string]$TargetDatabase = "")
    $arguments = @("--host=$dbHost", "--port=$dbPort", "--user=$dbUser", "--batch", "--skip-column-names")
    if ($TargetDatabase) { $arguments += $TargetDatabase }
    $output = $Sql | & $mysql @arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($output -join "`n") }
    return @($output)
}

function Start-MigrationRunner {
    $runnerPath = ConvertTo-BashPath $runner
    $envPath = ConvertTo-BashPath $envFile
    $command = "MIGRATION_ENV_FILE=$(Quote-BashValue $envPath) bash $(Quote-BashValue $runnerPath)"
    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $bash
    $startInfo.Arguments = "-lc `"$command`""
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    if (-not $process.Start()) { throw "Failed to start migration runner" }
    return $process
}

function Wait-ForLockOwner {
    for ($attempt = 0; $attempt -lt 100; $attempt++) {
        $held = (Invoke-MySql "SELECT IS_USED_LOCK('$lockName') IS NOT NULL;" $Database) -join ""
        if ($held -eq "1") { return }
        if ($first.HasExited) {
            $output = Read-ProcessOutput $first
            throw "First migration runner exited before holding the lock:`n$output"
        }
        Start-Sleep -Milliseconds 100
    }
    throw "First migration runner did not acquire the lock"
}

function Read-ProcessOutput {
    param([Diagnostics.Process]$Process)
    if (-not $Process.HasExited) { throw "Cannot read output from a running process" }
    return (($Process.StandardOutput.ReadToEnd(), $Process.StandardError.ReadToEnd()) -join "`n")
}

try {
    New-Item -ItemType Directory -Path $migrationDirectory -Force | Out-Null
    $exists = Invoke-MySql "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$Database';"
    if ([int]$exists -ne 0) { throw "Concurrency test database already exists; refusing to overwrite it" }
    Invoke-MySql "CREATE DATABASE ``$Database`` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" | Out-Null
    $created = $true

    $migrationSql = "CREATE TABLE migration_lock_probe (id INT NOT NULL PRIMARY KEY);`nSELECT SLEEP(4);"
    Set-Content -LiteralPath (Join-Path $migrationDirectory "V1.0__hold_migration_lock.sql") `
        -Value $migrationSql -Encoding utf8
    $lines = @(
        "DB_HOST=$(Quote-BashValue $dbHost)", "DB_PORT=$(Quote-BashValue $dbPort)",
        "DB_NAME=$(Quote-BashValue $Database)", "DB_USER=$(Quote-BashValue $dbUser)",
        "DB_PASSWORD=$(Quote-BashValue $dbPassword)",
        "MIGRATION_DIR=$(Quote-BashValue (ConvertTo-BashPath $migrationDirectory))",
        "MIGRATION_LOCK_NAME=$(Quote-BashValue $lockName)", "MIGRATION_LOCK_TIMEOUT_SECONDS='1'"
    )
    [IO.File]::WriteAllText($envFile, ($lines -join "`n") + "`n", [Text.UTF8Encoding]::new($false))

    $first = Start-MigrationRunner
    Wait-ForLockOwner
    $second = Start-MigrationRunner
    $second.WaitForExit()
    $first.WaitForExit()

    $firstOutput = Read-ProcessOutput $first
    $secondOutput = Read-ProcessOutput $second
    if ($first.ExitCode -ne 0) { throw "First runner failed:`n$firstOutput" }
    if ($second.ExitCode -eq 0 -or $secondOutput -notmatch 'could not acquire migration lock') {
        throw "Second runner was not rejected by the lock:`n$secondOutput"
    }
    $state = (Invoke-MySql "SELECT CONCAT(status, '|', execution_type) FROM sys_schema_migration WHERE version='1.0';" $Database) -join ""
    if ($state -ne "applied|applied") { throw "Unexpected migration state: $state" }
    $tableCount = (Invoke-MySql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$Database' AND table_name='migration_lock_probe';") -join ""
    if ($tableCount -ne "1") { throw "Migration probe table was not created" }
    Write-Output "migration concurrency guard passed"
}
finally {
    foreach ($process in @($first, $second)) {
        if ($process -and -not $process.HasExited) { Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue }
    }
    if ($created) { Invoke-MySql "DROP DATABASE IF EXISTS ``$Database``;" | Out-Null }
    $env:MYSQL_PWD = $previousMysqlPassword
    $resolvedTemp = [IO.Path]::GetFullPath($tempRoot)
    if ($resolvedTemp.StartsWith($tempBase, [StringComparison]::OrdinalIgnoreCase) -and
        (Split-Path $resolvedTemp -Leaf) -like 'paper-mes-migration-lock-*') {
        Remove-Item -LiteralPath $resolvedTemp -Recurse -Force -ErrorAction SilentlyContinue
    }
}
