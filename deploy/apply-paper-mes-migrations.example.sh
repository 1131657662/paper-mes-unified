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

mysql_cnf="$(mktemp)"

cleanup() {
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
  mysql --defaults-extra-file="${mysql_cnf}" "${DB_NAME}" "$@"
}

mysql_query() {
  mysql --defaults-extra-file="${mysql_cnf}" "${DB_NAME}" "$@" < /dev/null
}

create_migration_table() {
  mysql_exec <<SQL
CREATE TABLE IF NOT EXISTS \`${MIGRATION_TABLE}\` (
  \`version\` VARCHAR(50) NOT NULL,
  \`script_name\` VARCHAR(255) NOT NULL,
  \`checksum\` CHAR(64) NOT NULL,
  \`execution_type\` VARCHAR(20) NOT NULL DEFAULT 'applied',
  \`executed_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\`version\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES schema migration history';
SQL
}

applied_checksum() {
  local version="$1"
  mysql_query --batch --skip-column-names -e \
    "SELECT checksum FROM \`${MIGRATION_TABLE}\` WHERE version = '$(sql_escape "${version}")'"
}

record_migration() {
  local version="$1"
  local script_name="$2"
  local checksum="$3"
  local execution_type="$4"
  mysql_query -e "
INSERT INTO \`${MIGRATION_TABLE}\` (version, script_name, checksum, execution_type)
VALUES ('$(sql_escape "${version}")', '$(sql_escape "${script_name}")', '${checksum}', '${execution_type}')
"
}

apply_migration() {
  local script="$1"
  local script_name
  local version
  local checksum
  local existing_checksum

  script_name="$(basename "${script}")"
  [[ "${script_name}" =~ ^V[0-9]+(\.[0-9]+)*__[A-Za-z0-9._-]+\.sql$ ]] \
    || fail "invalid migration filename: ${script_name}"

  version="$(migration_version "${script_name}")"
  checksum="$(script_checksum "${script}")"
  existing_checksum="$(applied_checksum "${version}")"

  if [ -n "${existing_checksum}" ]; then
    [ "${existing_checksum}" = "${checksum}" ] \
      || fail "checksum mismatch for ${script_name}; do not edit applied migrations"
    echo "skip ${script_name}"
    return
  fi

  if [ "${MIGRATION_BASELINE}" = "1" ]; then
    echo "baseline ${script_name}"
    record_migration "${version}" "${script_name}" "${checksum}" "baseline"
    return
  fi

  echo "apply ${script_name}"
  mysql_exec < "${script}"
  record_migration "${version}" "${script_name}" "${checksum}" "applied"
}

main() {
  require_command mysql
  require_command sha256sum
  require_command sort
  require_command sed
  require_safe_identifier "DB_NAME" "${DB_NAME}"
  require_safe_identifier "DB_USER" "${DB_USER}"
  require_safe_identifier "MIGRATION_TABLE" "${MIGRATION_TABLE}"
  require_safe_path "MIGRATION_DIR" "${MIGRATION_DIR}"
  [[ "${MIGRATION_BASELINE}" =~ ^[01]$ ]] || fail "MIGRATION_BASELINE must be 0 or 1"

  cat > "${mysql_cnf}" <<EOF
[client]
host=${DB_HOST}
port=${DB_PORT}
user=${DB_USER}
password=${DB_PASSWORD}
default-character-set=utf8mb4
EOF
  chmod 600 "${mysql_cnf}"

  create_migration_table
  find "${MIGRATION_DIR}" -maxdepth 1 -type f -name 'V*.sql' -print0 \
    | sort -z -V \
    | while IFS= read -r -d '' script; do
        apply_migration "${script}"
      done

  echo "migrations completed"
}

main "$@"
