#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

MIGRATION_ENV_FILE="${MIGRATION_ENV_FILE:-/etc/paper-mes/migration.env}"
if [ -r "${MIGRATION_ENV_FILE}" ]; then
  set -a
  # shellcheck disable=SC1090
  . "${MIGRATION_ENV_FILE}"
  set +a
fi

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-paper_processing}"
DB_USER="${DB_USER:-paper_mes_migrator}"
DB_PASSWORD="${DB_PASSWORD:?set DB_PASSWORD or MIGRATION_ENV_FILE before migration state verification}"
MIGRATION_DIR="${MIGRATION_DIR:-/opt/paper-mes/source/sql}"
MIGRATION_TABLE="${MIGRATION_TABLE:-sys_schema_migration}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"

mysql_cnf="$(mktemp)"
cleanup() { rm -f "${mysql_cnf}"; }
trap cleanup EXIT

fail() {
  echo "migration state verification failed: $1" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "required command not found: $1"
}

require_identifier() {
  local name="$1"
  local value="$2"
  [[ "${value}" =~ ^[A-Za-z0-9_]+$ ]] || fail "invalid ${name}"
}

sql_escape() {
  printf "%s" "$1" | sed "s/'/''/g"
}

migration_version() {
  local script_name
  script_name="$(basename "$1")"
  printf "%s" "${script_name%%__*}" | sed 's/^V//'
}

mysql_query() {
  "${MYSQL_BIN}" --defaults-extra-file="${mysql_cnf}" --batch --skip-column-names \
    "${DB_NAME}" -e "$1"
}

check_table_state() {
  local table_exists invalid_rows
  table_exists="$(mysql_query "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='$(sql_escape "${MIGRATION_TABLE}")'")"
  [ "${table_exists}" = "1" ] || fail "${MIGRATION_TABLE} does not exist"
  invalid_rows="$(mysql_query "SELECT COUNT(*) FROM \`${MIGRATION_TABLE}\` WHERE status IS NULL OR status <> 'applied'")"
  [ "${invalid_rows}" = "0" ] || fail "${invalid_rows} migration record(s) are not applied"
}

check_migration_file() {
  local script="$1"
  local script_name version checksum row status recorded_checksum
  script_name="$(basename "${script}")"
  [[ "${script_name}" =~ ^V[0-9]+(\.[0-9]+)*__[A-Za-z0-9._-]+\.sql$ ]] \
    || fail "invalid migration filename: ${script_name}"
  version="$(migration_version "${script}")"
  checksum="$(sha256sum "${script}" | awk '{print $1}')"
  row="$(mysql_query "SELECT status, checksum FROM \`${MIGRATION_TABLE}\` WHERE version='$(sql_escape "${version}")'")"
  [ -n "${row}" ] || fail "migration ${script_name} is not recorded"
  IFS=$'\t' read -r status recorded_checksum <<< "${row}"
  [ "${status}" = "applied" ] || fail "migration ${script_name} has status ${status}"
  [ "${recorded_checksum}" = "${checksum}" ] \
    || fail "migration ${script_name} checksum does not match the deployed source"
}

main() {
  local command_name script count=0
  for command_name in "${MYSQL_BIN}" sha256sum awk sed find sort basename mktemp rm; do
    require_command "${command_name}"
  done
  require_identifier DB_NAME "${DB_NAME}"
  require_identifier DB_USER "${DB_USER}"
  require_identifier MIGRATION_TABLE "${MIGRATION_TABLE}"
  [ -d "${MIGRATION_DIR}" ] || fail "migration directory does not exist: ${MIGRATION_DIR}"

  cat > "${mysql_cnf}" <<EOF
[client]
host=${DB_HOST}
port=${DB_PORT}
user=${DB_USER}
password=${DB_PASSWORD}
default-character-set=utf8mb4
EOF
  chmod 600 "${mysql_cnf}"

  check_table_state
  while IFS= read -r -d '' script; do
    check_migration_file "${script}"
    count=$((count + 1))
  done < <(find "${MIGRATION_DIR}" -maxdepth 1 -type f -name 'V*.sql' -print0 | sort -z -V)
  [ "${count}" -gt 0 ] || fail "no migration scripts found in ${MIGRATION_DIR}"
  echo "migration state verification passed: ${count} migration script(s) applied"
}

main "$@"
