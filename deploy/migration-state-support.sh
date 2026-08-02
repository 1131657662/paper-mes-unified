#!/usr/bin/env bash

create_migration_table() {
  mysql_exec <<SQL
CREATE TABLE IF NOT EXISTS \`${MIGRATION_TABLE}\` (
  \`version\` VARCHAR(50) NOT NULL,
  \`script_name\` VARCHAR(255) NOT NULL,
  \`checksum\` CHAR(64) NOT NULL,
  \`execution_type\` VARCHAR(20) NOT NULL DEFAULT 'applied',
  \`status\` VARCHAR(20) NOT NULL DEFAULT 'applied',
  \`failure_message\` TEXT NULL,
  \`started_at\` DATETIME NULL,
  \`finished_at\` DATETIME NULL,
  \`executed_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\`version\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES schema migration history';
SQL
  ensure_column status "VARCHAR(20) NOT NULL DEFAULT 'applied'"
  ensure_column failure_message "TEXT NULL"
  ensure_column started_at "DATETIME NULL"
  ensure_column finished_at "DATETIME NULL"
}

ensure_column() {
  local column="$1"
  local definition="$2"
  local exists
  exists="$(mysql_query --batch --skip-column-names -e \
    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = '$(sql_escape "${MIGRATION_TABLE}")' AND column_name = '$(sql_escape "${column}")'")"
  if [ "${exists}" = "0" ]; then
    mysql_exec -e "ALTER TABLE \`${MIGRATION_TABLE}\` ADD COLUMN \`${column}\` ${definition}"
  fi
}

verify_baseline_contract() {
  local value
  value="$(mysql_query --batch --skip-column-names -e "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'biz_settle_detail' AND column_name = 'active_order_uuid'")"
  [ "${value}" = "1" ] || fail "baseline schema contract missing biz_settle_detail.active_order_uuid"
  value="$(mysql_query --batch --skip-column-names -e "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'biz_settle_detail' AND index_name = 'uk_settle_detail_order_active'")"
  [ "${value}" = "1" ] || fail "baseline schema contract missing uk_settle_detail_order_active"
  value="$(mysql_query --batch --skip-column-names -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'rpt_report_query_snapshot'")"
  [ "${value}" = "1" ] || fail "baseline schema contract missing rpt_report_query_snapshot"
}

migration_row() {
  local version="$1"
  mysql_query --batch --skip-column-names -e \
    "SELECT status, checksum FROM \`${MIGRATION_TABLE}\` WHERE version = '$(sql_escape "${version}")'"
}

record_running() {
  local version="$1"
  local script_name="$2"
  local checksum="$3"
  local execution_type="$4"
  mysql_query -e "
INSERT INTO \`${MIGRATION_TABLE}\`
  (version, script_name, checksum, execution_type, status, failure_message, started_at, finished_at)
VALUES ('$(sql_escape "${version}")', '$(sql_escape "${script_name}")', '${checksum}', '${execution_type}', 'running', NULL, NOW(), NULL)
ON DUPLICATE KEY UPDATE
  script_name = VALUES(script_name), checksum = VALUES(checksum), execution_type = VALUES(execution_type),
  status = 'running', failure_message = NULL, started_at = NOW(), finished_at = NULL"
}

record_applied() {
  local version="$1"
  mysql_query -e "
UPDATE \`${MIGRATION_TABLE}\`
SET status = 'applied', failure_message = NULL, finished_at = NOW(), executed_at = NOW()
WHERE version = '$(sql_escape "${version}")'"
}

record_failed() {
  local version="$1"
  local message="$2"
  mysql_query -e "
UPDATE \`${MIGRATION_TABLE}\`
SET status = 'failed', failure_message = '$(sql_escape "${message:0:2000}")', finished_at = NOW()
WHERE version = '$(sql_escape "${version}")'" || true
}

is_at_most_baseline() {
  local version="$1"
  [ "$(printf '%s\n' "${version}" "${MIGRATION_BASELINE_VERSION}" | sort -V | head -n 1)" = "${version}" ]
}

apply_migration() {
  local script="$1"
  local script_name version checksum row existing_status existing_checksum output
  local execution_type="applied"

  assert_lock_owned
  script_name="$(basename "${script}")"
  [[ "${script_name}" =~ ^V[0-9]+(\.[0-9]+)*__[A-Za-z0-9._-]+\.sql$ ]] \
    || fail "invalid migration filename: ${script_name}"
  version="$(migration_version "${script_name}")"
  checksum="$(script_checksum "${script}")"
  row="$(migration_row "${version}" || true)"

  if [ -n "${row}" ]; then
    IFS=$'\t' read -r existing_status existing_checksum <<< "${row}"
    [ "${existing_checksum}" = "${checksum}" ] \
      || fail "checksum mismatch for ${script_name}; do not edit applied migrations"
    if [ "${existing_status}" = "applied" ]; then
      echo "skip ${script_name}"
      return
    fi
    [ "${existing_status}" != "running" ] \
      || fail "migration ${script_name} is left in running state; inspect the database before retrying"
    if [ "${existing_status}" = "failed" ] && [ "${MIGRATION_RETRY_FAILED}" != "1" ]; then
      fail "migration ${script_name} is failed; set MIGRATION_RETRY_FAILED=1 after recovery review"
    fi
  fi

  if [ "${MIGRATION_BASELINE}" = "1" ] && is_at_most_baseline "${version}"; then
    execution_type="baseline"
    record_running "${version}" "${script_name}" "${checksum}" "${execution_type}"
    record_applied "${version}"
    echo "baseline ${script_name}"
    return
  fi

  record_running "${version}" "${script_name}" "${checksum}" "${execution_type}"
  echo "apply ${script_name}"
  if output="$(mysql_exec < "${script}" 2>&1)"; then
    assert_lock_owned
    record_applied "${version}"
    return
  fi
  record_failed "${version}" "${output}"
  printf '%s\n' "${output}" >&2
  return 1
}
