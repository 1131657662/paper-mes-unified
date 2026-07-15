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

function Test-Gzip([string]$Path) {
    $input = [IO.File]::OpenRead($Path)
    try {
        $gzip = New-Object IO.Compression.GZipStream($input, [IO.Compression.CompressionMode]::Decompress)
        try { $gzip.CopyTo([IO.Stream]::Null) } finally { $gzip.Dispose() }
    } finally { $input.Dispose() }
}

function Write-Utf8NoBom([string]$Path, [string[]]$Lines) {
    [IO.File]::WriteAllLines($Path, $Lines, (New-Object Text.UTF8Encoding($false)))
}

$envFile = if ($env:BACKUP_ENV_FILE) { $env:BACKUP_ENV_FILE } else { '' }
Import-EnvFile $envFile
$backupRoot = [IO.Path]::GetFullPath((Require-Value 'BACKUP_ROOT'))
$dbHost = Require-Value 'DB_HOST'
$dbPort = Require-Value 'DB_PORT'
$dbName = Require-Value 'DB_NAME'
$dbUser = Require-Value 'DB_USER'
$dbPassword = Require-Value 'DB_PASSWORD'
$uploadDir = Require-Value 'UPLOAD_DIR'

Test-Identifier 'DB_NAME' $dbName
Test-Identifier 'DB_USER' $dbUser
$dump = Get-Command mysqldump.exe -ErrorAction Stop
$tar = Get-Command tar.exe -ErrorAction Stop

New-Item -ItemType Directory -Force -Path $backupRoot | Out-Null
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$targetDir = Join-Path $backupRoot $timestamp
$tempDir = Join-Path $backupRoot ".tmp-$timestamp-$PID"
$mysqlCnf = [IO.Path]::GetTempFileName()
$lock = $null

try {
    $lockPath = Join-Path $backupRoot '.backup.lock'
    $lock = [IO.File]::Open($lockPath, 'OpenOrCreate', 'ReadWrite', 'None')
    if (Test-Path -LiteralPath $targetDir) { throw "backup target already exists: $targetDir" }
    New-Item -ItemType Directory -Path $tempDir | Out-Null

    @"
[client]
host=$dbHost
port=$dbPort
user=$dbUser
password=$dbPassword
default-character-set=utf8mb4
"@ | Set-Content -LiteralPath $mysqlCnf -Encoding ASCII

    $plainSql = Join-Path $tempDir "$dbName.sql"
    $sqlArchive = "$plainSql.gz"
    $dumpArgs = @(
        "--defaults-extra-file=$mysqlCnf", '--no-tablespaces', '--single-transaction',
        '--hex-blob', '--routines', '--triggers', '--events', "--result-file=$plainSql", $dbName
    )
    & $dump.Source @dumpArgs
    if ($LASTEXITCODE -ne 0) { throw "mysqldump failed with exit code $LASTEXITCODE" }

    $source = [IO.File]::OpenRead($plainSql)
    $destination = [IO.File]::Create($sqlArchive)
    try {
        $gzip = New-Object IO.Compression.GZipStream($destination, [IO.Compression.CompressionLevel]::Optimal)
        try { $source.CopyTo($gzip) } finally { $gzip.Dispose() }
    } finally {
        $source.Dispose()
        $destination.Dispose()
        Remove-Item -LiteralPath $plainSql -Force
    }

    if (Test-Path -LiteralPath $uploadDir -PathType Container) {
        $uploadFull = [IO.Path]::GetFullPath($uploadDir).TrimEnd('\')
        $uploadArchive = Join-Path $tempDir 'upload.tar.gz'
        & $tar.Source '-C' (Split-Path $uploadFull -Parent) '-czf' $uploadArchive (Split-Path $uploadFull -Leaf)
        if ($LASTEXITCODE -ne 0) { throw "upload archive failed with exit code $LASTEXITCODE" }
    }

    $metadata = @(
        'script_version=2'
        "timestamp=$timestamp"
        "db_host=$dbHost"
        "db_port=$dbPort"
        "db_name=$dbName"
        "upload_dir=$uploadDir"
        "hostname=$env:COMPUTERNAME"
    )
    Write-Utf8NoBom (Join-Path $tempDir 'backup-info.txt') $metadata

    $checksumLines = Get-ChildItem -LiteralPath $tempDir -Filter '*.gz' | ForEach-Object {
        $hash = Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256
        "$($hash.Hash.ToLowerInvariant())  $($_.Name)"
    }
    $checksumLines | Set-Content -LiteralPath (Join-Path $tempDir 'SHA256SUMS') -Encoding ASCII
    Test-Gzip $sqlArchive
    Move-Item -LiteralPath $tempDir -Destination $targetDir

    Write-Output "backup completed: $targetDir"
} finally {
    if ($lock) { $lock.Dispose() }
    Remove-Item -LiteralPath $mysqlCnf -Force -ErrorAction SilentlyContinue
    if (Test-Path -LiteralPath $tempDir) { Remove-Item -LiteralPath $tempDir -Recurse -Force }
}
