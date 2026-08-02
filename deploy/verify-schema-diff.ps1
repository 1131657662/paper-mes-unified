param(
    [string]$DatabasePrefix = "paper_mes_schema_gate",
    # This is the historical replay cut used to exercise the pending window
    # against the canonical baseline; it is not the canonical baseline file's
    # current version (sql/schema-baseline.version).
    [string]$BaselineVersion = "3.49",
    [switch]$KeepDatabases
)

$ErrorActionPreference = "Stop"
if ($DatabasePrefix -notmatch '^[A-Za-z0-9_]+$') { throw "Unsafe database prefix: $DatabasePrefix" }
if ($BaselineVersion -notmatch '^[0-9]+(?:\.[0-9]+)*$') { throw "Unsafe baseline version: $BaselineVersion" }

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$schema = Join-Path $repoRoot "sql\01_schema_v4.1.sql"
$migrationDirectory = Join-Path $repoRoot "sql"
$mysql = (Get-Command mysql.exe -ErrorAction Stop).Source
$dbHost = if ($env:PAPER_MES_IT_DB_HOST) { $env:PAPER_MES_IT_DB_HOST } else { "127.0.0.1" }
$dbPort = if ($env:PAPER_MES_IT_DB_PORT) { $env:PAPER_MES_IT_DB_PORT } else { "3306" }
$dbUser = if ($env:PAPER_MES_IT_DB_USERNAME) { $env:PAPER_MES_IT_DB_USERNAME } else { "root" }
$dbPassword = $env:PAPER_MES_IT_DB_PASSWORD
if (-not $dbPassword) { throw "Set PAPER_MES_IT_DB_PASSWORD before running the schema gate" }

$baselineDb = "${DatabasePrefix}_baseline"
$replayDb = "${DatabasePrefix}_replay"
$created = @()
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

function Apply-SqlFile {
    param([string]$Path, [string]$TargetDatabase)
    $sql = Get-Content -Raw -Encoding UTF8 $Path
    Invoke-MySql $sql $TargetDatabase | Out-Null
}

function Apply-PendingMigrations {
    param([string]$TargetDatabase)
    $allFiles = Get-ChildItem -LiteralPath $migrationDirectory -Filter "V*.sql" -File
    $invalidFiles = @($allFiles | Where-Object { $_.Name -notmatch '^V[0-9]+(?:\.[0-9]+)*__[A-Za-z0-9._-]+\.sql$' })
    if ($invalidFiles.Count -gt 0) {
        throw "Invalid migration filename(s): $($invalidFiles.Name -join ', ')"
    }
    $files = $allFiles |
        Sort-Object { [version]([regex]::Match($_.Name, '^V([0-9]+(?:\.[0-9]+)*)__').Groups[1].Value) } |
        Where-Object {
            [version]([regex]::Match($_.Name, '^V([0-9]+(?:\.[0-9]+)*)__').Groups[1].Value) -gt [version]$BaselineVersion
        }
    if (-not $files) { throw "No migrations found after baseline $BaselineVersion" }
    foreach ($file in $files) {
        Write-Output "replay $($file.Name)"
        Apply-SqlFile $file.FullName $TargetDatabase
    }
}

function Schema-Signature {
    param([string]$TargetDatabase)
    $tables = Invoke-MySql @"
SELECT CONCAT('T|', table_name, '|', table_type, '|', COALESCE(engine, '<NULL>'),
              '|', COALESCE(table_collation, '<NULL>'))
FROM information_schema.tables
WHERE table_schema = '$TargetDatabase'
"@ $TargetDatabase
    $columns = Invoke-MySql @"
SELECT CONCAT('C|', table_name, '|', ordinal_position, '|', column_name, '|', column_type, '|', is_nullable,
              '|', COALESCE(character_set_name, '<NULL>'), '|', COALESCE(collation_name, '<NULL>'),
              '|', COALESCE(column_default, '<NULL>'), '|', extra, '|', COALESCE(generation_expression, '<NULL>'))
FROM information_schema.columns
WHERE table_schema = '$TargetDatabase'
"@ $TargetDatabase
    $indexes = Invoke-MySql @"
SELECT CONCAT('I|', table_name, '|', index_name, '|', non_unique, '|', seq_in_index,
              '|', COALESCE(column_name, '<EXPRESSION>'), '|', COALESCE(collation, '<NULL>'),
              '|', COALESCE(sub_part, '<NULL>'), '|', COALESCE(index_type, '<NULL>'))
FROM information_schema.statistics
WHERE table_schema = '$TargetDatabase'
"@ $TargetDatabase
    $constraints = Invoke-MySql @"
SELECT CONCAT('K|', table_name, '|', constraint_name, '|', constraint_type)
FROM information_schema.table_constraints
WHERE constraint_schema = '$TargetDatabase'
"@ $TargetDatabase
    $foreignKeys = Invoke-MySql @"
SELECT CONCAT('F|', kcu.table_name, '|', kcu.constraint_name, '|', kcu.ordinal_position,
              '|', kcu.column_name, '|', kcu.referenced_table_name, '|', kcu.referenced_column_name,
              '|', COALESCE(rc.update_rule, '<NULL>'), '|', COALESCE(rc.delete_rule, '<NULL>'))
FROM information_schema.key_column_usage kcu
LEFT JOIN information_schema.referential_constraints rc
  ON rc.constraint_schema = kcu.constraint_schema
 AND rc.table_name = kcu.table_name
 AND rc.constraint_name = kcu.constraint_name
WHERE kcu.constraint_schema = '$TargetDatabase'
  AND kcu.referenced_table_name IS NOT NULL
"@ $TargetDatabase
    $checkExpressions = Invoke-MySql @"
SELECT CONCAT('X|', tc.table_name, '|', tc.constraint_name, '|', cc.check_clause)
FROM information_schema.table_constraints tc
JOIN information_schema.check_constraints cc
  ON cc.constraint_schema = tc.constraint_schema
 AND cc.constraint_name = tc.constraint_name
WHERE tc.constraint_schema = '$TargetDatabase'
  AND tc.constraint_type = 'CHECK'
"@ $TargetDatabase
    $triggers = Invoke-MySql @"
SELECT CONCAT('G|', trigger_name, '|', event_object_table, '|', action_timing,
              '|', event_manipulation, '|', action_statement)
FROM information_schema.triggers
WHERE trigger_schema = '$TargetDatabase'
"@ $TargetDatabase
    return @($tables + $columns + $indexes + $constraints + $foreignKeys + $checkExpressions + $triggers | Sort-Object)
}

function Assert-RequiredContract {
    param([string]$TargetDatabase)
    $checks = @(
        @{ Label = "biz_settle_detail.active_order_uuid"; Sql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='$TargetDatabase' AND table_name='biz_settle_detail' AND column_name='active_order_uuid';" },
        @{ Label = "uk_settle_detail_order_active"; Sql = "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema='$TargetDatabase' AND table_name='biz_settle_detail' AND index_name='uk_settle_detail_order_active';" },
        @{ Label = "idx_finish_unassigned_order"; Sql = "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema='$TargetDatabase' AND table_name='biz_finish_roll' AND index_name='idx_finish_unassigned_order';" },
        @{ Label = "rpt_report_query_snapshot"; Sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$TargetDatabase' AND table_name='rpt_report_query_snapshot';" },
        @{ Label = "uk_report_query_snapshot_idempotency"; Sql = "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema='$TargetDatabase' AND table_name='rpt_report_query_snapshot' AND index_name='uk_report_query_snapshot_idempotency';" },
        @{ Label = "token digest column"; Sql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='$TargetDatabase' AND table_name='sys_user_session' AND column_name='token' AND character_maximum_length=64;" },
        @{ Label = "inventory no-update trigger"; Sql = "SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema='$TargetDatabase' AND trigger_name='trg_inventory_transaction_no_update';" },
        @{ Label = "inventory no-delete trigger"; Sql = "SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema='$TargetDatabase' AND trigger_name='trg_inventory_transaction_no_delete';" }
    )
    foreach ($check in $checks) {
        $value = Invoke-MySql $check.Sql
        if ([int](($value | Select-Object -First 1).ToString().Trim()) -lt 1) { throw "Schema contract missing: $($check.Label)" }
    }
}

try {
    foreach ($db in @($baselineDb, $replayDb)) {
        $exists = Invoke-MySql "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$db';"
        if ([int](($exists | Select-Object -First 1).ToString().Trim()) -ne 0) { throw "Refusing to overwrite existing database $db" }
        Invoke-MySql "CREATE DATABASE ``$db`` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" | Out-Null
        $created += $db
        Apply-SqlFile $schema $db
    }

    Assert-RequiredContract $baselineDb
    Apply-PendingMigrations $replayDb
    Assert-RequiredContract $replayDb

    $baselineSignature = Schema-Signature $baselineDb
    $replaySignature = Schema-Signature $replayDb
    $diff = Compare-Object -ReferenceObject $baselineSignature -DifferenceObject $replaySignature
    if ($diff) {
        $diff | Format-Table -AutoSize | Out-String | Write-Output
        throw "Baseline and pending-migration schemas differ"
    }
    Write-Output "schema diff gate passed: baseline=$baselineDb replay=$replayDb pending_after=$BaselineVersion"
}
finally {
    if (-not $KeepDatabases) {
        foreach ($db in $created) {
            try { Invoke-MySql "DROP DATABASE ``$db``;" | Out-Null } catch { Write-Warning $_.Exception.Message }
        }
    }
    $env:MYSQL_PWD = $previousMysqlPassword
}
