param(
    [int]$BackendPort = 8081,
    [int]$FrontendPort = 5173
)

$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot
$LogDir = Join-Path $Root '.codex-run-logs'
$FrontendDir = Join-Path $Root 'frontend'
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Import-UserAiEnvironment {
    $names = @(
        'PAPER_MES_AI_CONFIG_MASTER_KEY',
        'PAPER_MES_AI_MESSAGE_ENCRYPTION_KEY',
        'PAPER_MES_AI_PROVIDER',
        'PAPER_MES_AI_DATA_MODE',
        'PAPER_MES_AI_DEEPSEEK_API_KEY',
        'PAPER_MES_AI_DEEPSEEK_MODEL_PRO',
        'PAPER_MES_AI_DEEPSEEK_BASE_URL'
    )
    foreach ($name in $names) {
        if (-not [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name, 'Process'))) {
            continue
        }
        $userValue = [Environment]::GetEnvironmentVariable($name, 'User')
        if (-not [string]::IsNullOrWhiteSpace($userValue)) {
            Set-Item -LiteralPath "Env:$name" -Value $userValue
        }
    }
}

Import-UserAiEnvironment

function Resolve-LocalRuntimeMetadata {
    try {
        $sha = (& git -C $Root rev-parse --short HEAD 2>$null).Trim()
        if ($sha) { $env:PAPER_MES_GIT_SHA = $sha }
    } catch {
        # Keep an explicitly supplied value when git metadata is unavailable.
    }
    if (-not $env:PAPER_MES_BUILD_TIME) {
        $env:PAPER_MES_BUILD_TIME = (Get-Date).ToUniversalTime().ToString('o')
    }
}

Resolve-LocalRuntimeMetadata

function Test-HttpOk([string]$Uri) {
    try {
        $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 2
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

function Test-PortListening([int]$Port) {
    return $null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-Http([string]$Uri, [int]$TimeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-HttpOk $Uri) { return }
        Start-Sleep -Milliseconds 750
    }
    throw "Service startup timed out: $Uri"
}

$SchemaScript = Join-Path $Root 'dev-schema.ps1'
if (-not (Test-Path -LiteralPath $SchemaScript)) {
    throw "Missing local schema helper: $SchemaScript"
}
. $SchemaScript

function Start-Backend {
    $healthUri = "http://127.0.0.1:$BackendPort/actuator/health"
    if (Test-HttpOk $healthUri) {
        Write-Output "Backend already running: $healthUri"
        return
    }
    if (Test-PortListening $BackendPort) {
        throw "Port $BackendPort is occupied and the backend health check failed."
    }
    Start-Process -FilePath (Join-Path $Root 'mvnw.cmd') `
        -ArgumentList @('spring-boot:run', '-Dspring-boot.run.profiles=dev') `
        -WorkingDirectory $Root -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $LogDir 'backend.log') `
        -RedirectStandardError (Join-Path $LogDir 'backend.err.log') | Out-Null
    Wait-Http $healthUri 60
    Write-Output "Backend started: $healthUri"
}

function Install-FrontendDependencies {
    if (Test-Path (Join-Path $FrontendDir 'node_modules')) { return }
    Write-Output 'Installing frontend dependencies...'
    Push-Location $FrontendDir
    try { & npm.cmd install; if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' } }
    finally { Pop-Location }
}

function Start-Frontend {
    $frontendUri = "http://127.0.0.1:$FrontendPort"
    if (Test-HttpOk $frontendUri) {
        Write-Output "Frontend already running: $frontendUri"
        return
    }
    if (Test-PortListening $FrontendPort) {
        throw "Port $FrontendPort is occupied and the frontend is unreachable."
    }
    Install-FrontendDependencies
    Start-Process -FilePath 'npm.cmd' `
        -ArgumentList @('run', 'dev', '--', '--host', '127.0.0.1', '--port', "$FrontendPort") `
        -WorkingDirectory $FrontendDir -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $LogDir 'frontend.log') `
        -RedirectStandardError (Join-Path $LogDir 'frontend.err.log') | Out-Null
    Wait-Http $frontendUri 30
    Write-Output "Frontend started: $frontendUri"
}

Ensure-LocalInventoryLedgerSchema
Ensure-LocalIssueVersionSchema
Ensure-LocalAppendSessionSchema
Ensure-LocalActualReceivedMetricSemantic
Start-Backend
Start-Frontend
Write-Output ''
Write-Output "Paper MES: http://127.0.0.1:$FrontendPort"
