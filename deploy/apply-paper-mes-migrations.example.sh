#!/usr/bin/env bash
set -euo pipefail
umask 077

MIGRATION_ENV_FILE="${MIGRATION_ENV_FILE:-/etc/paper-mes/migration.env}"
if [ -r "${MIGRATION_ENV_FILE}" ]; then
  set -a
  . "${MIGRATION_ENV_FILE}"
  set +a
fi

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-paper_processing}"
DB_USER="${DB_USER:-paper_mes_app}"
DB_PASSWORD="${DB_PASSWORD:?set DB_PASSWORD or MIGRATION_ENV_FILE before running migrations}"
MIGRATION_DIR="${MIGRATION_DIR:-$(pwd)/sql}"
MIGRATION_TABLE="${MIGRATION_TABLE:-sys_schema_migration}"
MIGRATION_BASELINE="${MIGRATION_BASELINE:-0}"
MIGRATION_BASELINE_VERSION="${MIGRATION_BASELINE_VERSION:-}"
SCHEMA_BASELINE_FILE="${SCHEMA_BASELINE_FILE:-${MIGRATION_DIR}/01_schema_v4.1.sql}"
SCHEMA_BASELINE_CHECKSUM="${SCHEMA_BASELINE_CHECKSUM:-}"
MIGRATION_RETRY_FAILED="${MIGRATION_RETRY_FAILED:-0}"
MIGRATION_LOCK_TIMEOUT_SECONDS="${MIGRATION_LOCK_TIMEOUT_SECONDS:-30}"
MIGRATION_LOCK_NAME="${MIGRATION_LOCK_NAME:-paper_mes_${DB_NAME}_migration}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"

mysql_cnf="$(mktemp)"
lock_acquired=0

cleanup() {
  if [ "${lock_acquired}" = "1" ]; then
    mysql_query -e "SELECT RELEASE_LOCK('$(sql_escape "${MIGRATION_LOCK_NAME}")')" >/dev/null 2>&1 || true
  fi
  rm -f "${mysql_cnf}"
}
trap cleanup EXIT

fail() {
  echo "$1" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

require_safe_identifier() {
  local name="$1"
  local value="$2"
  [[ "${value}" =~ ^[A-Za-z0-9_]+$ ]] || fail "invalid ${name}: ${value}"
}

require_safe_path() {
  local name="$1"
  local value="$2"
  [ -n "${value}" ] || fail "${name} cannot be empty"
  [ -d "${value}" ] || fail "${name} does not exist: ${value}"
}

require_safe_file() {
  local name="$1"
  local value="$2"
  [ -n "${value}" ] || fail "${name} cannot be empty"
  [ -f "${value}" ] || fail "${name} does not exist: ${value}"
}

sql_escape() {
  printf "%s" "$1" | sed "s/'/''/g"
}

migration_version() {
  local filename="$1"
  local version="${filename%%__*}"
  printf "%s" "${version#V}"
}

script_checksum() {
  sha256sum "$1" | awk '{print $1}'
}

mysql_exec() {
  "${MYSQL_BIN}" --defaults-extra-file="${mysql_cnf}" "${DB_NAME}" "$@"
}

mysql_query() {
  "${MYSQL_BIN}" --defaults-extra-file="${mysql_cnf}" "${DB_NAME}" "$@" < /dev/null
}

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

acquire_lock() {
  local result
  result="$(mysql_query --batch --skip-column-names -e \
    "SELECT GET_LOCK('$(sql_escape "${MIGRATION_LOCK_NAME}")', ${MIGRATION_LOCK_TIMEOUT_SECONDS})")"
  [ "${result}" = "1" ] || fail "could not acquire migration lock ${MIGRATION_LOCK_NAME}"
  lock_acquired=1
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
  local script_name
  local version
  local checksum
  local row
  local existing_status
  local existing_checksum
  local execution_type="applied"
  local output

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
    if [ "${existing_status}" = "running" ]; then
      fail "migration ${script_name} is left in running state; inspect the database before retrying"
    fi
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
    record_applied "${version}"
    return
  fi
  record_failed "${version}" "${output}"
  printf '%s\n' "${output}" >&2
  return 1
}

main() {
  require_command "${MYSQL_BIN}"
  require_command sha256sum
  require_command sort
  require_command sed
  require_command head
  require_safe_identifier "DB_NAME" "${DB_NAME}"
  require_safe_identifier "DB_USER" "${DB_USER}"
  require_safe_identifier "MIGRATION_TABLE" "${MIGRATION_TABLE}"
  require_safe_identifier "MIGRATION_LOCK_NAME" "${MIGRATION_LOCK_NAME}"
  [ "${#MIGRATION_LOCK_NAME}" -le 64 ] \
    || fail "MIGRATION_LOCK_NAME must not exceed MySQL's 64-character GET_LOCK limit"
  require_safe_path "MIGRATION_DIR" "${MIGRATION_DIR}"
  [[ "${MIGRATION_BASELINE}" =~ ^[01]$ ]] || fail "MIGRATION_BASELINE must be 0 or 1"
  [[ "${MIGRATION_RETRY_FAILED}" =~ ^[01]$ ]] || fail "MIGRATION_RETRY_FAILED must be 0 or 1"
  if [ "${MIGRATION_BASELINE}" = "1" ]; then
    require_safe_file "SCHEMA_BASELINE_FILE" "${SCHEMA_BASELINE_FILE}"
    [[ "${MIGRATION_BASELINE_VERSION}" =~ ^[0-9]+(\.[0-9]+)*$ ]] \
      || fail "MIGRATION_BASELINE_VERSION is required when MIGRATION_BASELINE=1"
    [[ "${SCHEMA_BASELINE_CHECKSUM}" =~ ^[0-9a-fA-F]{64}$ ]] \
      || fail "SCHEMA_BASELINE_CHECKSUM is required when MIGRATION_BASELINE=1"
    [ "$(script_checksum "${SCHEMA_BASELINE_FILE}")" = "${SCHEMA_BASELINE_CHECKSUM,,}" ] \
      || fail "schema baseline checksum mismatch; review the file before baseline registration"
  fi

  cat > "${mysql_cnf}" <<EOF
[client]
host=${DB_HOST}
port=${DB_PORT}
user=${DB_USER}
password=${DB_PASSWORD}
default-character-set=utf8mb4
EOF
  chmod 600 "${mysql_cnf}"

  acquire_lock
  create_migration_table
  if [ "${MIGRATION_BASELINE}" = "1" ]; then
    verify_baseline_contract
  fi

  mapfile -d '' scripts < <(find "${MIGRATION_DIR}" -maxdepth 1 -type f -name 'V*.sql' -print0 | sort -z -V)
  [ "${#scripts[@]}" -gt 0 ] || fail "no migration scripts found in ${MIGRATION_DIR}"
  for script in "${scripts[@]}"; do
    apply_migration "${script}"
  done

  echo "migrations completed"
}

main "$@"
