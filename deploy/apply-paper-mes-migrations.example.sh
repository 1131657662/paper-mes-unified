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
runner_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
lock_support="${runner_dir}/migration-lock-support.sh"
state_support="${runner_dir}/migration-state-support.sh"
for support_file in "${lock_support}" "${state_support}"; do
  [ -r "${support_file}" ] || { echo "missing migration support: ${support_file}" >&2; exit 1; }
done
. "${lock_support}"
. "${state_support}"

mysql_cnf="$(mktemp)"
lock_session_error="$(mktemp)"
lock_acquired=0
lock_session_pid=""
lock_owner_name=""

cleanup() {
  release_lock_session
  rm -f "${mysql_cnf}" "${lock_session_error}"
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

main() {
  require_command "${MYSQL_BIN}"
  require_command sha256sum
  require_command sort
  require_command sed
  require_command head
  require_command sleep
  require_safe_identifier "DB_NAME" "${DB_NAME}"
  require_safe_identifier "DB_USER" "${DB_USER}"
  require_safe_identifier "MIGRATION_TABLE" "${MIGRATION_TABLE}"
  require_safe_identifier "MIGRATION_LOCK_NAME" "${MIGRATION_LOCK_NAME}"
  [[ "${DB_PORT}" =~ ^[0-9]+$ ]] && [ "${DB_PORT}" -ge 1 ] && [ "${DB_PORT}" -le 65535 ] \
    || fail "DB_PORT must be between 1 and 65535"
  [[ "${MIGRATION_LOCK_TIMEOUT_SECONDS}" =~ ^[0-9]+$ ]] \
    && [ "${MIGRATION_LOCK_TIMEOUT_SECONDS}" -le 300 ] \
    || fail "MIGRATION_LOCK_TIMEOUT_SECONDS must be between 0 and 300"
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
