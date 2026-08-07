function Get-LocalDbCredentials {
    $configPath = Join-Path $Root 'src/main/resources/application-dev.yml'
    $config = Get-Content -LiteralPath $configPath -Raw
    $userMatch = [regex]::Match($config, '(?m)^\s+username:\s*(?<value>[^\r\n]+)\s*$')
    $passwordMatch = [regex]::Match($config, '(?m)^\s+password:\s*(?<value>[^\r\n]+)\s*$')
    $dbUser = if ($env:PAPER_MES_DEV_DB_USER) { $env:PAPER_MES_DEV_DB_USER } elseif ($userMatch.Success) { $userMatch.Groups['value'].Value.Trim() } else { 'root' }
    $dbPassword = if ($env:PAPER_MES_DEV_DB_PASSWORD) { $env:PAPER_MES_DEV_DB_PASSWORD } elseif ($passwordMatch.Success) { $passwordMatch.Groups['value'].Value.Trim() } else { '' }
    if ([string]::IsNullOrWhiteSpace($dbPassword)) {
        throw 'Set PAPER_MES_DEV_DB_PASSWORD or configure spring.datasource.password in application-dev.yml.'
    }
    return @{ User = $dbUser; Password = $dbPassword }
}

function Invoke-LocalMySql([string]$Sql) {
    $mysql = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($null -eq $mysql) {
        throw 'MySQL client is required for local schema checks. Add mysql.exe to PATH.'
    }
    $credentials = Get-LocalDbCredentials
    $defaultsFile = New-TemporaryFile
    try {
        @("[client]", "user=$($credentials.User)", "password=$($credentials.Password)", 'host=127.0.0.1', 'port=3306') |
            Set-Content -LiteralPath $defaultsFile.FullName -Encoding ascii
        $output = & $mysql.Source "--defaults-extra-file=$($defaultsFile.FullName)" '--default-character-set=utf8mb4' '--batch' '--skip-column-names' '--raw' 'paper_processing' '-e' $Sql 2>&1
        if ($LASTEXITCODE -ne 0) { throw ($output | Out-String).Trim() }
        return ($output | Out-String).Trim()
    } finally {
        Remove-Item -LiteralPath $defaultsFile.FullName -Force -ErrorAction SilentlyContinue
    }
}

function Invoke-LocalMigration([string]$MigrationPath) {
    $mysql = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($null -eq $mysql) {
        throw 'MySQL client is required for local migrations. Add mysql.exe to PATH.'
    }
    $credentials = Get-LocalDbCredentials
    $defaultsFile = New-TemporaryFile
    try {
        @("[client]", "user=$($credentials.User)", "password=$($credentials.Password)", 'host=127.0.0.1', 'port=3306') |
            Set-Content -LiteralPath $defaultsFile.FullName -Encoding ascii
        $sql = Get-Content -LiteralPath $MigrationPath -Raw -Encoding utf8
        $previousOutputEncoding = $OutputEncoding
        try {
            $OutputEncoding = [System.Text.UTF8Encoding]::new($false)
            $output = $sql | & $mysql.Source "--defaults-extra-file=$($defaultsFile.FullName)" '--default-character-set=utf8mb4' 'paper_processing' 2>&1
        } finally {
            $OutputEncoding = $previousOutputEncoding
        }
        if ($LASTEXITCODE -ne 0) { throw (($output | Out-String).Trim()) }
    } finally {
        Remove-Item -LiteralPath $defaultsFile.FullName -Force -ErrorAction SilentlyContinue
    }
}

function Register-LocalMigrationState([string]$MigrationPath) {
    if ((Invoke-LocalMySql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_schema_migration'") -ne '1') {
        return
    }
    $name = [IO.Path]::GetFileName($MigrationPath)
    $match = [regex]::Match($name, '^V(?<version>[0-9]+(?:\.[0-9]+)*)__')
    if (-not $match.Success) { throw "Invalid migration filename: $name" }
    $version = $match.Groups['version'].Value
    if ((Invoke-LocalMySql "SELECT COUNT(*) FROM sys_schema_migration WHERE version = '$version' AND status = 'applied'") -eq '1') {
        return
    }
    $checksum = (Get-FileHash -LiteralPath $MigrationPath -Algorithm SHA256).Hash.ToLowerInvariant()
    Invoke-LocalMySql "INSERT INTO sys_schema_migration (version,script_name,checksum,execution_type,status,started_at,finished_at,executed_at) VALUES ('$version','$name','$checksum','applied','applied',NOW(),NOW(),NOW()) ON DUPLICATE KEY UPDATE script_name=VALUES(script_name),checksum=VALUES(checksum),execution_type='applied',status='applied',failure_message=NULL,finished_at=NOW(),executed_at=NOW()" | Out-Null
}

function Ensure-LocalIssueVersionSchema {
    $status = Invoke-LocalMySql @"
SELECT IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'biz_process_order_issue_version') = 1
  AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'biz_process_order_issue_version' AND column_name IN ('request_id','payload_hash')) = 2
  AND (SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema = DATABASE() AND trigger_name IN ('trg_process_order_issue_version_no_terminal_update','trg_process_order_issue_version_no_terminal_delete')) = 2,
  'ready', 'missing')
"@
    if ($status -eq 'ready') {
        Write-Output 'Local schema ready: issue version history'
        return
    }

    Write-Output 'Local schema incomplete: applying V3.53 and V3.54...'
    Invoke-LocalMigration (Join-Path $Root 'sql/V3.53__add_process_order_issue_versions.sql')
    Invoke-LocalMigration (Join-Path $Root 'sql/V3.54__protect_issue_versions_and_add_reissue_idempotency.sql')
    if ((Invoke-LocalMySql "SELECT IF((SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'biz_process_order_issue_version') = 1 AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'biz_process_order_issue_version' AND column_name IN ('request_id','payload_hash')) = 2, 'ready', 'missing')") -ne 'ready') {
        throw 'Local issue version schema is still incomplete after migrations.'
    }
}

function Ensure-LocalInventoryLedgerSchema {
    $status = Invoke-LocalMySql @"
SELECT IF(
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'biz_inventory_transaction') = 1
  AND (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'biz_inventory_transaction'
      AND column_name IN ('uuid','sequence_no','finish_roll_uuid','event_type','source_business_type',
        'source_business_uuid','quantity_delta','weight_delta','reserved_quantity_delta',
        'reserved_weight_delta','quantity_before','quantity_after','weight_before','weight_after',
        'reserved_quantity_before','reserved_quantity_after','reserved_weight_before',
        'reserved_weight_after','available_quantity_before','available_quantity_after',
        'available_weight_before','available_weight_after','reason','operator_uuid','operator_name',
        'occurred_at','idempotency_key','payload_hash','created_at')) = 29
  AND (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'biz_inventory_transaction'
      AND index_name IN ('uk_inventory_transaction_idempotency','idx_inventory_transaction_finish_time',
        'idx_inventory_transaction_source')) = 3
  AND (SELECT COUNT(*) FROM information_schema.triggers
    WHERE trigger_schema = DATABASE() AND trigger_name IN
      ('trg_inventory_transaction_no_update','trg_inventory_transaction_no_delete')) = 2,
  'ready', 'missing')
"@
    if ($status -eq 'ready') {
        Write-Output 'Local schema ready: inventory transaction ledger'
        return
    }

    $tableExists = Invoke-LocalMySql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'biz_inventory_transaction'"
    if ($tableExists -eq '1') {
        throw 'Local inventory transaction schema is incomplete; apply V3.52 manually after inspecting the existing table.'
    }

    Write-Output 'Local schema incomplete: applying V3.52...'
    Invoke-LocalMigration (Join-Path $Root 'sql/V3.52__add_inventory_transaction_ledger.sql')
    $verified = Invoke-LocalMySql @"
SELECT IF(
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'biz_inventory_transaction') = 1
  AND (SELECT COUNT(*) FROM information_schema.triggers
    WHERE trigger_schema = DATABASE() AND trigger_name IN
      ('trg_inventory_transaction_no_update','trg_inventory_transaction_no_delete')) = 2,
  'ready', 'missing')
"@
    if ($verified -ne 'ready') {
        throw 'Local inventory transaction schema is still incomplete after V3.52.'
    }
}

function Ensure-LocalAppendSessionSchema {
    $status = Invoke-LocalMySql @"
SELECT IF(
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name IN
      ('biz_process_order_append_session','biz_process_order_append_roll')) = 2
  AND (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'biz_process_order_append_session'
      AND column_name IN ('commit_request_id','active_order_uuid')) = 2
  AND (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'biz_process_order_append_session'
      AND index_name = 'uk_process_append_active_order') = 1,
  'ready', 'missing')
"@
    if ($status -eq 'ready') {
        Write-Output 'Local schema ready: process order append sessions'
        return
    }

    Write-Output 'Local schema incomplete: applying V3.61...'
    Invoke-LocalMigration (Join-Path $Root 'sql/V3.61__add_process_order_append_sessions.sql')
    if ((Invoke-LocalMySql "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'biz_process_order_append_session' AND index_name = 'uk_process_append_active_order'") -ne '1') {
        throw 'Local process order append session schema is still incomplete after V3.61.'
    }
}

function Ensure-LocalActualReceivedMetricSemantic {
    $migrationPath = Join-Path $Root 'sql/V3.62__clarify_actual_received_amount_semantics.sql'
    $tableExists = Invoke-LocalMySql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'rpt_metric_definition'"
    if ($tableExists -ne '1') {
        Write-Output 'Local report metric table is not present; application bootstrap will seed current semantics.'
        return
    }

    $status = Invoke-LocalMySql @"
SELECT CASE
  WHEN COUNT(*) = 0 THEN 'seed'
  WHEN MAX(HEX(metric_name)) = 'E5AE9EE99985E588B0E8B4A6'
    AND MAX(HEX(description)) = 'E69C89E69588E694B6E6ACBEE6B581E6B0B4E4B8ADE79A84E5AE9EE99985E588B0E8B4A6E98791E9A29DEFBC8CE58C85E590ABE78EB0E98791E38081E8BDACE8B4A6E38081E5BEAEE4BFA1E5928CE694AFE4BB98E5AE9D'
    THEN 'ready'
  ELSE 'stale'
END
FROM rpt_metric_definition
WHERE metric_code = 'cash_received_amount'
"@
    if ($status -eq 'ready') {
        Register-LocalMigrationState $migrationPath
        Write-Output 'Local report metric ready: actual received amount'
        return
    }
    if ($status -eq 'seed') {
        Invoke-LocalMigration $migrationPath
        Register-LocalMigrationState $migrationPath
        Write-Output 'Local actual received metric is not seeded yet; application bootstrap will create it.'
        return
    }

    Write-Output 'Local report metric label is stale: applying V3.62...'
    Invoke-LocalMigration $migrationPath
    $verified = Invoke-LocalMySql "SELECT COUNT(*) FROM rpt_metric_definition WHERE metric_code = 'cash_received_amount' AND HEX(metric_name) = 'E5AE9EE99985E588B0E8B4A6'"
    if ($verified -ne '1') {
        throw 'Local actual received metric semantic is still stale after V3.62.'
    }
    Register-LocalMigrationState $migrationPath
}
