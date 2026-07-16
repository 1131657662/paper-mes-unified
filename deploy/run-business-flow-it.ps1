param(
    [string]$EnvFile = (Join-Path $PSScriptRoot 'business-flow-it.env')
)

$ErrorActionPreference = 'Stop'
$required = @(
    'PAPER_MES_IT_DB_URL',
    'PAPER_MES_IT_DB_USERNAME',
    'PAPER_MES_IT_DB_PASSWORD'
)

function Import-AllowlistedEnvFile([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return }
    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $text = $line.Trim()
        if (-not $text -or $text.StartsWith('#')) { continue }
        $parts = $text.Split('=', 2)
        if ($parts.Count -ne 2 -or $required -notcontains $parts[0]) {
            throw "Unsupported integration-test environment entry: $($parts[0])"
        }
        if (-not [Environment]::GetEnvironmentVariable($parts[0], 'Process')) {
            [Environment]::SetEnvironmentVariable($parts[0], $parts[1], 'Process')
        }
    }
}

function Require-Environment {
    foreach ($name in $required) {
        if (-not [Environment]::GetEnvironmentVariable($name, 'Process')) {
            throw "Missing $name. Set it in the process or $EnvFile"
        }
    }
}

function Read-TestDatabaseTarget {
    $jdbcUrl = $env:PAPER_MES_IT_DB_URL
    if (-not $jdbcUrl.StartsWith('jdbc:mysql://')) {
        throw 'PAPER_MES_IT_DB_URL must be a MySQL JDBC URL'
    }
    $uri = [Uri]$jdbcUrl.Substring(5)
    $database = $uri.AbsolutePath.Trim('/')
    if (-not $database -or $database -notmatch '(^|_)test($|_)') {
        throw "Refusing non-test database: $database"
    }
    return @{ Host = $uri.Host; Port = $uri.Port; Database = $database }
}

function Test-DatabasePort($Target) {
    $client = [Net.Sockets.TcpClient]::new()
    try {
        $task = $client.ConnectAsync($Target.Host, $Target.Port)
        if (-not $task.Wait(3000) -or -not $client.Connected) {
            throw "Cannot connect to MySQL at $($Target.Host):$($Target.Port)"
        }
    } finally {
        $client.Dispose()
    }
}

Import-AllowlistedEnvFile $EnvFile
Require-Environment
$target = Read-TestDatabaseTarget
Test-DatabasePort $target

$repoRoot = Split-Path -Parent $PSScriptRoot
$maven = Join-Path $repoRoot 'mvnw.cmd'
Write-Host "Integration preflight passed for $($target.Host):$($target.Port)/$($target.Database)"
& $maven verify -Pbusiness-flow-it
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
