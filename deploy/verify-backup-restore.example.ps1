$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Import-EnvFile([string]$Path) {
    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path -LiteralPath $Path -PathType Leaf)) { return }
    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
        $parts = $trimmed.Split('=', 2)
        if ($parts.Count -eq 2) { Set-Item -Path "Env:$($parts[0].Trim())" -Value $parts[1].Trim() }
    }
}

function Require-Value([string]$Name) {
    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) { throw "missing required environment variable: $Name" }
    return $value
}

function Test-Identifier([string]$Name, [string]$Value) {
    if ($Value -notmatch '^[A-Za-z0-9_]+$') { throw "invalid ${Name}: $Value" }
}

function Expand-Gzip([string]$Source, [string]$Destination) {
    $input = [IO.File]::OpenRead($Source)
    $output = [IO.File]::Create($Destination)
    try {
        $gzip = New-Object IO.Compression.GZipStream($input, [IO.Compression.CompressionMode]::Decompress)
        try { $gzip.CopyTo($output) } finally { $gzip.Dispose() }
    } finally {
        $input.Dispose()
        $output.Dispose()
    }
}

function Write-Utf8NoBom([string]$Path, [string[]]$Lines) {
    [IO.File]::WriteAllLines($Path, $Lines, (New-Object Text.UTF8Encoding($false)))
}

function Import-MySql([string]$Executable, [string]$DefaultsFile, [string]$Database, [string]$SqlFile) {
    $start = New-Object Diagnostics.ProcessStartInfo
    $start.FileName = $Executable
    $start.Arguments = "--defaults-extra-file=`"$DefaultsFile`" `"$Database`""
    $start.UseShellExecute = $false
    $start.RedirectStandardInput = $true
    $process = [Diagnostics.Process]::Start($start)
    $input = [IO.File]::OpenRead($SqlFile)
    $exitCode = -1
    try {
        $input.CopyTo($process.StandardInput.BaseStream)
        $process.StandardInput.Close()
        $process.WaitForExit()
        $exitCode = $process.ExitCode
    } finally {
        $input.Dispose()
        $process.Dispose()
    }
    if ($exitCode -ne 0) { throw "mysql import failed with exit code $exitCode" }
}

function Remove-RestoreDatabase([string]$Executable, [string]$DefaultsFile, [string]$Database) {
    & $Executable "--defaults-extra-file=$DefaultsFile" '-e' "DROP DATABASE IF EXISTS ``$Database``;"
    if ($LASTEXITCODE -ne 0) { throw 'failed to drop restore database' }
}

$envFile = if ($env:BACKUP_ENV_FILE) { $env:BACKUP_ENV_FILE } else { '' }
Import-EnvFile $envFile
$backupRoot = [IO.Path]::GetFullPath((Require-Value 'BACKUP_ROOT')).TrimEnd('\')
$backupDir = [IO.Path]::GetFullPath((Require-Value 'BACKUP_DIR')).TrimEnd('\')
$sourceDb = Require-Value 'SOURCE_DB_NAME'
$restoreDb = if ($env:RESTORE_DB_NAME) { $env:RESTORE_DB_NAME } else { 'paper_mes_restore_check' }
$dbHost = Require-Value 'DB_HOST'
$dbPort = Require-Value 'DB_PORT'
$dbUser = Require-Value 'DB_ADMIN_USER'
$dbPassword = Require-Value 'DB_ADMIN_PASSWORD'
$dropAfter = -not $env:DROP_AFTER_VERIFY -or $env:DROP_AFTER_VERIFY -eq 'true'

Test-Identifier 'SOURCE_DB_NAME' $sourceDb
Test-Identifier 'RESTORE_DB_NAME' $restoreDb
Test-Identifier 'DB_ADMIN_USER' $dbUser
if ($sourceDb -eq $restoreDb) { throw 'RESTORE_DB_NAME must not equal SOURCE_DB_NAME' }
if (-not $backupDir.StartsWith("$backupRoot\", [StringComparison]::OrdinalIgnoreCase)) {
    throw 'BACKUP_DIR must be inside BACKUP_ROOT'
}

$mysql = Get-Command mysql.exe -ErrorAction Stop
$tar = Get-Command tar.exe -ErrorAction Stop
$sqlArchive = Join-Path $backupDir "$sourceDb.sql.gz"
$checksumFile = Join-Path $backupDir 'SHA256SUMS'
if (-not (Test-Path -LiteralPath $sqlArchive -PathType Leaf)) { throw "sql archive not found: $sqlArchive" }
if (-not (Test-Path -LiteralPath $checksumFile -PathType Leaf)) { throw "checksum file not found: $checksumFile" }

$mysqlCnf = [IO.Path]::GetTempFileName()
$plainSql = [IO.Path]::GetTempFileName()
$lock = $null
$restoreCreated = $false
try {
    $lock = [IO.File]::Open((Join-Path $backupRoot '.restore-check.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
    foreach ($line in Get-Content -LiteralPath $checksumFile -Encoding ASCII) {
        if ($line -notmatch '^([0-9a-fA-F]{64})\s+(.+)$') { throw 'invalid SHA256SUMS format' }
        $file = Join-Path $backupDir $Matches[2]
        $actual = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash
        if ($actual -ne $Matches[1]) { throw "checksum mismatch: $($Matches[2])" }
    }
    Expand-Gzip $sqlArchive $plainSql

    @"
[client]
host=$dbHost
port=$dbPort
user=$dbUser
password=$dbPassword
default-character-set=utf8mb4
"@ | Set-Content -LiteralPath $mysqlCnf -Encoding ASCII

    $createSql = "DROP DATABASE IF EXISTS ``$restoreDb``; CREATE DATABASE ``$restoreDb`` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_0900_ai_ci;"
    & $mysql.Source "--defaults-extra-file=$mysqlCnf" '-e' $createSql
    if ($LASTEXITCODE -ne 0) { throw 'failed to create restore database' }
    $restoreCreated = $true
    Import-MySql $mysql.Source $mysqlCnf $restoreDb $plainSql

    $tableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$restoreDb';"
    $tableCount = [int](& $mysql.Source "--defaults-extra-file=$mysqlCnf" '-N' '-B' '-e' $tableSql)
    if ($tableCount -le 0) { throw 'restore check failed: restored database has no tables' }
    $orderTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$restoreDb' AND table_name='biz_process_order';"
    $orderTableCount = [int](& $mysql.Source "--defaults-extra-file=$mysqlCnf" '-N' '-B' '-e' $orderTableSql)
    if ($orderTableCount -le 0) { throw 'restore check failed: required table biz_process_order is missing' }
    $orderCount = & $mysql.Source "--defaults-extra-file=$mysqlCnf" '-N' '-B' $restoreDb '-e' 'SELECT COUNT(*) FROM biz_process_order;'
    if ($LASTEXITCODE -ne 0) { throw 'restore check failed: cannot query biz_process_order' }

    $uploadArchive = Join-Path $backupDir 'upload.tar.gz'
    if (Test-Path -LiteralPath $uploadArchive -PathType Leaf) {
        & $tar.Source '-tzf' $uploadArchive | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'upload archive validation failed' }
    }
    if ($dropAfter) {
        Remove-RestoreDatabase $mysql.Source $mysqlCnf $restoreDb
        $restoreCreated = $false
    }

    $report = @(
        "verified_at=$((Get-Date).ToString('o'))"
        "backup_dir=$backupDir"
        "restore_db=$restoreDb"
        "table_count=$tableCount"
        "process_order_count=$orderCount"
        "dropped_after_verify=$($dropAfter.ToString().ToLowerInvariant())"
    )
    Write-Utf8NoBom (Join-Path $backupDir 'restore-check.txt') $report
    Write-Output "restore check completed: tables=$tableCount, process_orders=$orderCount, restore_db=$restoreDb, dropped=$dropAfter"
} finally {
    if ($dropAfter -and $restoreCreated -and (Test-Path -LiteralPath $mysqlCnf -PathType Leaf)) {
        try {
            Remove-RestoreDatabase $mysql.Source $mysqlCnf $restoreDb
        } catch {
            Write-Warning "failed to clean restore database after verification error: $($_.Exception.Message)"
        }
    }
    if ($lock) { $lock.Dispose() }
    Remove-Item -LiteralPath $mysqlCnf, $plainSql -Force -ErrorAction SilentlyContinue
}
