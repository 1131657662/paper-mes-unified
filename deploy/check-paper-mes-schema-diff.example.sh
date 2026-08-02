#!/usr/bin/env bash
set -euo pipefail
umask 077

# Compare a fresh canonical baseline with the same baseline after replaying
# migrations newer than MIGRATION_BASELINE_VERSION. This is a compatibility
# replay against the current canonical schema, not reconstruction of a
# historical schema at MIGRATION_BASELINE_VERSION. It therefore proves that
# the pending window remains safe to execute and structurally idempotent, but
# does not prove a V1-to-current upgrade chain. That needs a real, approved
# historical fixture. Both databases must be disposable, uniquely named
# fixtures; existing databases are never dropped.
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-paper_mes_migrator}"
DB_PASSWORD="${DB_PASSWORD:?set DB_PASSWORD before running schema diff}"
BASELINE_DB="${BASELINE_DB:-paper_mes_schema_diff_baseline}"
MIGRATED_DB="${MIGRATED_DB:-paper_mes_schema_diff_migrated}"
# This script is safe to invoke from any working directory. Override these
# paths explicitly when the deployment layout differs from the repository.
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
MIGRATION_DIR="${MIGRATION_DIR:-${SCRIPT_DIR}/../sql}"
SCHEMA_FILE="${SCHEMA_FILE:-${MIGRATION_DIR}/01_schema_v4.1.sql}"
MIGRATION_BASELINE_VERSION="${MIGRATION_BASELINE_VERSION:?set MIGRATION_BASELINE_VERSION explicitly}"
KEEP_DATABASES="${KEEP_DATABASES:-0}"

mysql_cnf="$(mktemp)"
baseline_created=0
migrated_created=0
temp_dir=""
cleanup() {
  [ -z "${temp_dir}" ] || rm -rf -- "${temp_dir}" || true
  if [ "${KEEP_DATABASES}" != "1" ]; then
    [ "${baseline_created}" = "1" ] && mysql_exec -e "DROP DATABASE IF EXISTS \`${BASELINE_DB}\`;" || true
    [ "${migrated_created}" = "1" ] && mysql_exec -e "DROP DATABASE IF EXISTS \`${MIGRATED_DB}\`;" || true
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

require_identifier() {
  local name="$1"
  local value="$2"
  [[ "${value}" =~ ^paper_mes_schema_diff_[a-z0-9_]+$ ]] \
    || fail "${name} must use the paper_mes_schema_diff_ prefix"
}

require_directory() {
  [ -d "$1" ] || fail "directory does not exist: $1"
}

sql_escape() {
  printf "%s" "$1" | sed "s/'/''/g"
}

mysql_exec() {
  mysql --defaults-extra-file="${mysql_cnf}" --batch --skip-column-names "$@"
}

mysql_db() {
  local database="$1"
  shift
  mysql --defaults-extra-file="${mysql_cnf}" --batch --skip-column-names "${database}" "$@"
}

schema_dump() {
  local database="$1"
  local output="$2"
  # The runner owns this metadata table. Excluding exactly this table keeps
  # the comparison focused on the application schema while retaining every
  # application table, index, and constraint in the gate.
  mysqldump --defaults-extra-file="${mysql_cnf}" \
    --no-data --skip-comments --skip-set-charset --skip-add-drop-table \
    --routines=false --events=false \
    --ignore-table="${database}.sys_schema_migration" "${database}" \
    | sed -E \
        -e 's/ AUTO_INCREMENT=[0-9]+//g' \
        -e '/^\/\*!/d' \
        -e '/^--/d' \
        -e '/^SET /d' \
    > "${output}"
}

main() {
  # These are all invoked below; fail before creating databases when a
  # minimal/BusyBox image is missing one of the GNU/coreutils features used
  # by the gate (notably sort -V and sort -z -V).
  local command_name
  for command_name in mysql mysqldump sha256sum sed diff awk find sort head basename mktemp rm; do
    require_command "${command_name}"
  done
  printf '2\n10\n' | sort -V >/dev/null 2>&1 \
    || fail "sort does not support version sorting (-V)"
  printf '2\0 10\0' | sort -z -V >/dev/null 2>&1 \
    || fail "sort does not support NUL-delimited version sorting (-z -V)"
  require_directory "${MIGRATION_DIR}"
  [ -f "${SCHEMA_FILE}" ] || fail "schema file does not exist: ${SCHEMA_FILE}"
  require_identifier BASELINE_DB "${BASELINE_DB}"
  require_identifier MIGRATED_DB "${MIGRATED_DB}"
  [ "${BASELINE_DB}" != "${MIGRATED_DB}" ] || fail "baseline and migrated databases must differ"
  [[ "${MIGRATION_BASELINE_VERSION}" =~ ^[0-9]+(\.[0-9]+)*$ ]] \
    || fail "invalid MIGRATION_BASELINE_VERSION"
  [[ "${KEEP_DATABASES}" =~ ^[01]$ ]] || fail "KEEP_DATABASES must be 0 or 1"

  cat > "${mysql_cnf}" <<EOF
[client]
host=${DB_HOST}
port=${DB_PORT}
user=${DB_USER}
password=${DB_PASSWORD}
default-character-set=utf8mb4
EOF
  chmod 600 "${mysql_cnf}"

  local existing
  existing="$(mysql_exec --batch --skip-column-names -e \
    "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name IN ('$(sql_escape "${BASELINE_DB}")','$(sql_escape "${MIGRATED_DB}")')")"
  [ "${existing}" = "0" ] \
    || fail "schema diff databases already exist; refusing to overwrite them"

  mysql_exec -e "CREATE DATABASE \`${BASELINE_DB}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
  baseline_created=1
  mysql_exec -e "CREATE DATABASE \`${MIGRATED_DB}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
  migrated_created=1
  mysql_db "${BASELINE_DB}" < "${SCHEMA_FILE}"
  mysql_db "${MIGRATED_DB}" < "${SCHEMA_FILE}"

  # The canonical schema is already materialized in the replay database.  The
  # migration runner still needs the exact checksum when it records the
  # pre-baseline history; without it the runner must (correctly) refuse to
  # proceed.  Keep this derived from the file used above so the gate cannot
  # silently baseline a different schema artifact.
  local schema_checksum
  schema_checksum="$(sha256sum "${SCHEMA_FILE}" | awk '{print $1}')"

  DB_NAME="${MIGRATED_DB}" \
  MIGRATION_ENV_FILE=/dev/null \
  MIGRATION_DIR="${MIGRATION_DIR}" \
  MIGRATION_BASELINE=1 \
  MIGRATION_BASELINE_VERSION="${MIGRATION_BASELINE_VERSION}" \
  SCHEMA_BASELINE_FILE="${SCHEMA_FILE}" \
  SCHEMA_BASELINE_CHECKSUM="${schema_checksum}" \
  DB_HOST="${DB_HOST}" DB_PORT="${DB_PORT}" DB_USER="${DB_USER}" DB_PASSWORD="${DB_PASSWORD}" \
  bash "${SCRIPT_DIR}/apply-paper-mes-migrations.example.sh"

  # A schema gate must prove that the pending window was actually executed;
  # merely recording every migration as a baseline would make the comparison
  # vacuous.  Compare parsed migration versions with sort -V rather than SQL
  # decimals (where 3.10 would incorrectly compare as 3.1).
  local pending_count=0
  local script script_name version row
  while IFS= read -r -d '' script; do
    script_name="$(basename "${script}")"
    version="${script_name%%__*}"
    version="${version#V}"
    if [ "$(printf '%s\n' "${version}" "${MIGRATION_BASELINE_VERSION}" | sort -V | head -n 1)" = "${version}" ] && [ "${version}" != "${MIGRATION_BASELINE_VERSION}" ]; then
      continue
    fi
    if [ "${version}" = "${MIGRATION_BASELINE_VERSION}" ]; then
      continue
    fi
    pending_count=$((pending_count + 1))
    row="$(mysql_db "${MIGRATED_DB}" --batch --skip-column-names -e \
      "SELECT CONCAT(status, '|', execution_type) FROM \`sys_schema_migration\` WHERE version = '$(sql_escape "${version}")'")"
    [ "${row}" = "applied|applied" ] \
      || fail "pending migration ${script_name} was not applied (state: ${row:-missing})"
  done < <(find "${MIGRATION_DIR}" -maxdepth 1 -type f -name 'V*.sql' -print0 | sort -z -V)
  [ "${pending_count}" -gt 0 ] \
    || fail "no migration newer than MIGRATION_BASELINE_VERSION was applied"

  temp_dir="$(mktemp -d)"
  schema_dump "${BASELINE_DB}" "${temp_dir}/baseline.sql"
  schema_dump "${MIGRATED_DB}" "${temp_dir}/migrated.sql"
  if ! diff -u "${temp_dir}/baseline.sql" "${temp_dir}/migrated.sql"; then
    fail "baseline schema and replayed migration schema differ"
  fi
  echo "schema diff gate passed: canonical schema is unchanged after pending-window compatibility replay"
}

main "$@"
