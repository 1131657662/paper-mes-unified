#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

# Validate a business-approved, sanitized historical dump by replaying only
# migrations newer than its attested version in disposable databases.
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-paper_mes_migrator}"
DB_PASSWORD="${DB_PASSWORD:?set DB_PASSWORD before fixture verification}"
FIXTURE_DUMP="${FIXTURE_DUMP:?set FIXTURE_DUMP to the approved .sql or .sql.gz file}"
FIXTURE_MANIFEST="${FIXTURE_MANIFEST:?set FIXTURE_MANIFEST to the approval manifest}"
FIXTURE_MANIFEST_SHA256="${FIXTURE_MANIFEST_SHA256:?set FIXTURE_MANIFEST_SHA256 from the approval record}"
FIXTURE_DB="${FIXTURE_DB:-paper_mes_history_fixture}"
CANONICAL_DB="${CANONICAL_DB:-paper_mes_history_canonical}"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
MIGRATION_DIR="${MIGRATION_DIR:-${SCRIPT_DIR}/../sql}"
SCHEMA_FILE="${SCHEMA_FILE:-${MIGRATION_DIR}/01_schema_v4.1.sql}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"
MYSQLDUMP_BIN="${MYSQLDUMP_BIN:-mysqldump}"
SUPPORT_FILE="${SCRIPT_DIR}/historical-fixture-support.sh"
[ -r "${SUPPORT_FILE}" ] || { echo "missing fixture support: ${SUPPORT_FILE}" >&2; exit 1; }
. "${SUPPORT_FILE}"

mysql_cnf="$(mktemp)"
temp_dir="$(mktemp -d)"
fixture_created=0
canonical_created=0

fail() { echo "historical fixture verification failed: $1" >&2; exit 1; }

mysql_server() {
  "${MYSQL_BIN}" --defaults-extra-file="${mysql_cnf}" --batch --skip-column-names "$@"
}

mysql_db() {
  local database="$1"
  shift
  mysql_server "${database}" "$@"
}

cleanup() {
  if [ "${fixture_created}" = "1" ]; then
    mysql_server -e "DROP DATABASE IF EXISTS \`${FIXTURE_DB}\`;" || true
  fi
  if [ "${canonical_created}" = "1" ]; then
    mysql_server -e "DROP DATABASE IF EXISTS \`${CANONICAL_DB}\`;" || true
  fi
  rm -rf -- "${temp_dir}"
  rm -f -- "${mysql_cnf}"
}
trap cleanup EXIT

require_command() { command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"; }

require_database_name() {
  local label="$1"
  local value="$2"
  [[ "${value}" =~ ^paper_mes_history_[a-z0-9_]+$ ]] || fail "invalid ${label}"
}

schema_dump() {
  local database="$1"
  local output="$2"
  "${MYSQLDUMP_BIN}" --defaults-extra-file="${mysql_cnf}" --no-data --skip-comments \
    --skip-set-charset --skip-add-drop-table --routines=false --events=false \
    --ignore-table="${database}.sys_schema_migration_fixture_gate" "${database}" \
    | sed -E -e 's/ AUTO_INCREMENT=[0-9]+//g' -e '/^\/\*!/d' -e '/^--/d' -e '/^SET /d' > "${output}"
}

create_databases() {
  local existing
  existing="$(mysql_server -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name IN ('${FIXTURE_DB}','${CANONICAL_DB}')")"
  [ "${existing}" = "0" ] || fail "fixture databases already exist; refusing to overwrite"
  mysql_server -e "CREATE DATABASE \`${FIXTURE_DB}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
  fixture_created=1
  mysql_server -e "CREATE DATABASE \`${CANONICAL_DB}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
  canonical_created=1
}

apply_pending_migrations() {
  DB_NAME="${FIXTURE_DB}" DB_HOST="${DB_HOST}" DB_PORT="${DB_PORT}" DB_USER="${DB_USER}" \
  DB_PASSWORD="${DB_PASSWORD}" MYSQL_BIN="${MYSQL_BIN}" MIGRATION_ENV_FILE=/dev/null \
  MIGRATION_DIR="${temp_dir}/migrations" MIGRATION_TABLE=sys_schema_migration_fixture_gate \
  MIGRATION_BASELINE=0 bash "${SCRIPT_DIR}/apply-paper-mes-migrations.example.sh"
}

validate_inputs() {
  local command_name
  for command_name in "${MYSQL_BIN}" "${MYSQLDUMP_BIN}" awk bash cat cp diff find gzip head mkdir mktemp rm sed sha256sum sort; do
    require_command "${command_name}"
  done
  [ -f "${FIXTURE_DUMP}" ] || fail "fixture dump does not exist"
  [ -f "${FIXTURE_MANIFEST}" ] || fail "fixture manifest does not exist"
  [ -f "${SCHEMA_FILE}" ] || fail "canonical schema does not exist"
  [ -d "${MIGRATION_DIR}" ] || fail "migration directory does not exist"
  require_database_name FIXTURE_DB "${FIXTURE_DB}"
  require_database_name CANONICAL_DB "${CANONICAL_DB}"
  [ "${FIXTURE_DB}" != "${CANONICAL_DB}" ] || fail "fixture and canonical databases must differ"
  [[ "${DB_HOST}" =~ ^[A-Za-z0-9._:-]+$ ]] || fail "invalid DB_HOST"
  [[ "${DB_PORT}" =~ ^[0-9]+$ ]] && [ "${DB_PORT}" -ge 1 ] && [ "${DB_PORT}" -le 65535 ] || fail "invalid DB_PORT"
  [[ "${DB_USER}" =~ ^[A-Za-z0-9_]+$ ]] || fail "invalid DB_USER"
  [[ "${DB_PASSWORD}" != *$'\n'* && "${DB_PASSWORD}" != *$'\r'* ]] || fail "DB_PASSWORD contains a line break"
  [[ "${FIXTURE_MANIFEST_SHA256}" =~ ^[0-9a-f]{64}$ ]] || fail "invalid FIXTURE_MANIFEST_SHA256"
}

verify_artifacts() {
  local actual_checksum manifest_checksum schema_checksum
  stage_artifacts
  validate_manifest
  manifest_checksum="$(sha256sum "${FIXTURE_MANIFEST}" | awk '{print $1}')"
  [ "${manifest_checksum}" = "${FIXTURE_MANIFEST_SHA256}" ] || fail "fixture manifest checksum mismatch"
  actual_checksum="$(sha256sum "${FIXTURE_DUMP}" | awk '{print $1}')"
  [ "${actual_checksum}" = "${fixture_checksum}" ] || fail "fixture checksum mismatch"
  schema_checksum="$(sha256sum "${SCHEMA_FILE}" | awk '{print $1}')"
  [ "${actual_checksum}" != "${schema_checksum}" ] || fail "canonical schema is not a historical fixture"
  validate_dump_scope
  prepare_pending_migrations
}

stage_artifacts() {
  local dump_copy
  case "${FIXTURE_DUMP}" in
    *.sql.gz) dump_copy="${temp_dir}/approved-fixture.sql.gz" ;;
    *.sql) dump_copy="${temp_dir}/approved-fixture.sql" ;;
    *) fail "FIXTURE_DUMP must end in .sql or .sql.gz" ;;
  esac
  cp -- "${FIXTURE_DUMP}" "${dump_copy}"
  cp -- "${FIXTURE_MANIFEST}" "${temp_dir}/approved-fixture.manifest"
  cp -- "${SCHEMA_FILE}" "${temp_dir}/canonical-schema.sql"
  FIXTURE_DUMP="${dump_copy}"
  FIXTURE_MANIFEST="${temp_dir}/approved-fixture.manifest"
  SCHEMA_FILE="${temp_dir}/canonical-schema.sql"
}

write_mysql_config() {
  printf '[client]\nhost=%s\nport=%s\nuser=%s\npassword=%s\ndefault-character-set=utf8mb4\n' \
    "${DB_HOST}" "${DB_PORT}" "${DB_USER}" "${DB_PASSWORD}" > "${mysql_cnf}"
  chmod 600 "${mysql_cnf}"
}

load_fixture() {
  local required_count
  create_databases
  dump_stream | mysql_db "${FIXTURE_DB}"
  mysql_db "${CANONICAL_DB}" < "${SCHEMA_FILE}"
  required_count="$(mysql_db "${FIXTURE_DB}" -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('biz_process_order','biz_process_step')")"
  [ "${required_count}" = "2" ] || fail "fixture is missing required historical tables"
}

compare_schemas() {
  apply_pending_migrations
  schema_dump "${FIXTURE_DB}" "${temp_dir}/fixture.sql"
  schema_dump "${CANONICAL_DB}" "${temp_dir}/canonical.sql"
  diff -u "${temp_dir}/canonical.sql" "${temp_dir}/fixture.sql" \
    || fail "migrated fixture schema differs from canonical schema"
}

report_success() {
  printf 'historical fixture gate passed: version=%s migrations=%s dump_sha256=%s manifest_sha256=%s approved_by=%s approved_at=%s\n' \
    "${fixture_version}" "${pending_count}" "${fixture_checksum}" \
    "${FIXTURE_MANIFEST_SHA256}" "${approved_by}" "${approved_at}"
}

main() {
  validate_inputs
  verify_artifacts
  write_mysql_config
  load_fixture
  compare_schemas
  report_success
}

main "$@"
