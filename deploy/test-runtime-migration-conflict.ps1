param(
    [string]$Database = "paper_processing_migration_guard_test"
)

$ErrorActionPreference = "Stop"
$allowedName = '^paper_processing_migration_guard_test(?:_[0-9]+)?$'
if ($Database -notmatch $allowedName) {
    throw "Unsafe migration test database name: $Database"
}

$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
$dbHost = if ($env:PAPER_MES_IT_DB_HOST) { $env:PAPER_MES_IT_DB_HOST } else { "127.0.0.1" }
$dbPort = if ($env:PAPER_MES_IT_DB_PORT) { $env:PAPER_MES_IT_DB_PORT } else { "3306" }
$dbUser = if ($env:PAPER_MES_IT_DB_USERNAME) { $env:PAPER_MES_IT_DB_USERNAME } else { "root" }
$dbPassword = $env:PAPER_MES_IT_DB_PASSWORD
if (-not $dbPassword) {
    throw "Set PAPER_MES_IT_DB_PASSWORD before running the migration test"
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$schemaFile = Join-Path $repoRoot "sql\01_schema_v4.1.sql"
$migrationFile = Join-Path $repoRoot "sql\V2.8__add_runtime_safety_infrastructure.sql"
$env:MYSQL_PWD = $dbPassword
$created = $false

function Invoke-MySql {
    param([string]$Sql, [string]$TargetDatabase = "")
    $arguments = @("--host=$dbHost", "--port=$dbPort", "--user=$dbUser", "--batch", "--skip-column-names")
    if ($TargetDatabase) { $arguments += $TargetDatabase }
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $output = $Sql | & $mysql @arguments 2>&1
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousPreference
    if ($exitCode -ne 0) { throw ($output -join "`n") }
    return $output
}

function Invoke-MySqlFile {
    param([string]$Path, [string]$TargetDatabase, [bool]$ExpectFailure = $false)
    $arguments = @("--host=$dbHost", "--port=$dbPort", "--user=$dbUser", "--batch", $TargetDatabase)
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $output = Get-Content -Raw -Encoding UTF8 $Path | & $mysql @arguments 2>&1
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousPreference
    $failed = $exitCode -ne 0
    if ($ExpectFailure -eq $failed) { return $output }
    if ($ExpectFailure) { throw "Migration unexpectedly accepted duplicate finish reservations" }
    throw ($output -join "`n")
}

try {
    $exists = Invoke-MySql "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = '$Database';"
    if ([int]$exists -ne 0) { throw "Migration test database already exists; refusing to overwrite it" }

    Invoke-MySql "CREATE DATABASE ``$Database`` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
    $created = $true
    Invoke-MySqlFile $schemaFile $Database | Out-Null

    Invoke-MySql @'
ALTER TABLE biz_delivery_detail
  DROP INDEX uk_biz_delivery_detail_active_finish,
  DROP COLUMN finish_uuid_active,
  DROP COLUMN stock_lock_status;
INSERT INTO biz_delivery_order
  (uuid, delivery_no, customer_uuid, customer_name, delivery_date, delivery_status)
VALUES
  ('delivery-1', 'IT-MIGRATION-1', 'customer-1', 'migration test', CURRENT_DATE, 1),
  ('delivery-2', 'IT-MIGRATION-2', 'customer-1', 'migration test', CURRENT_DATE, 1);
INSERT INTO biz_delivery_detail
  (uuid, delivery_uuid, finish_uuid, order_uuid, out_weight)
VALUES
  ('detail-1', 'delivery-1', 'finish-1', 'order-1', 10.000),
  ('detail-2', 'delivery-2', 'finish-1', 'order-1', 10.000);
'@ $Database | Out-Null

    $failure = (Invoke-MySqlFile $migrationFile $Database $true) -join "`n"
    if ($failure -notmatch 'Duplicate entry|uk_biz_delivery_detail_active_finish') {
        throw "Migration failed for an unexpected reason: $failure"
    }

    $result = (Invoke-MySql "SELECT COUNT(*), SUM(is_deleted) FROM biz_delivery_detail WHERE finish_uuid='finish-1';" $Database) -split '\s+'
    if ($result[0] -ne "2" -or $result[1] -ne "0") {
        throw "Migration changed conflicting business rows: $($result -join ',')"
    }

    Write-Output "runtime migration conflict guard passed"
}
finally {
    if ($created) {
        Invoke-MySql "DROP DATABASE ``$Database``;" | Out-Null
    }
}
