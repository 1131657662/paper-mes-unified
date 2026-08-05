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
        $output = & $mysql.Source "--defaults-extra-file=$($defaultsFile.FullName)" '--batch' '--skip-column-names' '--raw' 'paper_processing' '-e' $Sql 2>&1
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
            $output = $sql | & $mysql.Source "--defaults-extra-file=$($defaultsFile.FullName)" 'paper_processing' 2>&1
        } finally {
            $OutputEncoding = $previousOutputEncoding
        }
        if ($LASTEXITCODE -ne 0) { throw (($output | Out-String).Trim()) }
    } finally {
        Remove-Item -LiteralPath $defaultsFile.FullName -Force -ErrorAction SilentlyContinue
    }
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
