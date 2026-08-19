#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

BACKUP_ENV_FILE="${BACKUP_ENV_FILE:-/etc/paper-mes/backup.env}"
MIGRATION_ENV_FILE="${MIGRATION_ENV_FILE:-/etc/paper-mes/migration.env}"

for env_file in "${BACKUP_ENV_FILE}" "${MIGRATION_ENV_FILE}"; do
  if [ -r "${env_file}" ]; then
    set -a
    # shellcheck disable=SC1090
    . "${env_file}"
    set +a
  fi
done

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-paper_processing}"
DB_USER="${DB_USER:-paper_mes_migrator}"
DB_PASSWORD="${DB_PASSWORD:?set DB_PASSWORD or MIGRATION_ENV_FILE before preflight}"
BACKUP_ROOT="${BACKUP_ROOT:-/opt/backups/paper-mes}"
MAX_BACKUP_AGE_HOURS="${MAX_BACKUP_AGE_HOURS:-48}"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8081/actuator/health}"
HTTP_TIMEOUT_SECONDS="${HTTP_TIMEOUT_SECONDS:-10}"
APP_USER="${APP_USER:-paper-mes}"
APP_GROUP="${APP_GROUP:-paper-mes}"
APP_SERVICE="${APP_SERVICE:-paper-mes}"
APP_TMP_DIR="${APP_TMP_DIR:-/run/paper-mes}"
APP_TMP_MODE="${APP_TMP_MODE:-750}"
APP_RUNTIME_DIRECTORY="${APP_RUNTIME_DIRECTORY:-paper-mes}"
PROC_ROOT="${PROC_ROOT:-/proc}"
SOURCE_ROOT="${SOURCE_ROOT:-/opt/paper-mes/source}"
SOURCE_PROVENANCE_SCRIPT="${SOURCE_PROVENANCE_SCRIPT:-${SOURCE_ROOT}/deploy/verify-paper-mes-source.example.sh}"
SCHEMA_BASELINE_FILE="${SCHEMA_BASELINE_FILE:-${SOURCE_ROOT}/sql/schema-baseline.version}"
APP_ENV_FILE="${APP_ENV_FILE:-/etc/paper-mes/paper-mes.env}"

mysql_cnf="$(mktemp)"
cleanup() { rm -f "${mysql_cnf}"; }
trap cleanup EXIT

fail() {
  echo "release preflight failed: $1" >&2
  exit 1
}

require_positive_integer() {
  [[ "$2" =~ ^[1-9][0-9]*$ ]] || fail "$1 must be a positive integer"
}

mysql_query() {
  mysql --defaults-extra-file="${mysql_cnf}" --batch --skip-column-names \
    "${DB_NAME}" -e "$1"
}

require_zero() {
  local label="$1"
  local value
  value="$(mysql_query "$2")"
  [[ "${value}" =~ ^[0-9]+$ ]] || fail "${label} check returned an invalid result"
  [ "${value}" = "0" ] || fail "${label}: ${value} conflict group(s) found"
}

check_health() {
  local response
  response="$(curl --fail --silent --show-error --max-time "${HTTP_TIMEOUT_SECONDS}" "${HEALTH_URL}")" \
    || fail "backend health request failed"
  grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' <<< "${response}" \
    || fail "backend health is not UP"
}

check_app_temp() {
  [ -d "${APP_TMP_DIR}" ] || fail "application temp directory not found: ${APP_TMP_DIR}"
  local actual_metadata
  actual_metadata="$(stat -c '%U:%G:%a' "${APP_TMP_DIR}")"
  [ "${actual_metadata}" = "${APP_USER}:${APP_GROUP}:${APP_TMP_MODE}" ] \
    || fail "application temp directory metadata is ${actual_metadata}, expected ${APP_USER}:${APP_GROUP}:${APP_TMP_MODE}"
  runuser -u "${APP_USER}" -- test -w "${APP_TMP_DIR}" \
    || fail "application temp directory is not writable by ${APP_USER}: ${APP_TMP_DIR}"
}

check_service_temp_runtime() {
  local private_tmp runtime_directory exec_start main_pid cmdline_file
  private_tmp="$(systemctl show "${APP_SERVICE}" --property=PrivateTmp --value)" \
    || fail "cannot read PrivateTmp for ${APP_SERVICE}"
  [ "${private_tmp}" = "yes" ] || fail "${APP_SERVICE} must use PrivateTmp=yes"
  runtime_directory="$(systemctl show "${APP_SERVICE}" --property=RuntimeDirectory --value)" \
    || fail "cannot read RuntimeDirectory for ${APP_SERVICE}"
  [ "${runtime_directory}" = "${APP_RUNTIME_DIRECTORY}" ] \
    || fail "${APP_SERVICE} must use RuntimeDirectory=${APP_RUNTIME_DIRECTORY}"
  exec_start="$(systemctl show "${APP_SERVICE}" --property=ExecStart --value)" \
    || fail "cannot read ExecStart for ${APP_SERVICE}"
  [[ "${exec_start}" == *"-Djava.io.tmpdir=${APP_TMP_DIR}"* ]] \
    || fail "${APP_SERVICE} ExecStart does not use ${APP_TMP_DIR}"
  main_pid="$(systemctl show "${APP_SERVICE}" --property=MainPID --value)" \
    || fail "cannot read MainPID for ${APP_SERVICE}"
  [[ "${main_pid}" =~ ^[1-9][0-9]*$ ]] || fail "${APP_SERVICE} has no running main process"
  cmdline_file="${PROC_ROOT}/${main_pid}/cmdline"
  [ -r "${cmdline_file}" ] || fail "cannot read running command line for ${APP_SERVICE}"
  tr '\0' '\n' < "${cmdline_file}" | grep -Fxq -- "-Djava.io.tmpdir=${APP_TMP_DIR}" \
    || fail "running ${APP_SERVICE} process does not use ${APP_TMP_DIR}; restart it after daemon-reload"
}

check_source_provenance() {
  [ -f "${SOURCE_PROVENANCE_SCRIPT}" ] \
    || fail "source provenance verifier not found: ${SOURCE_PROVENANCE_SCRIPT}"
  bash "${SOURCE_PROVENANCE_SCRIPT}" \
    || fail "cloud source or installed runtime files do not match GitHub"
}

check_schema_version_configuration() {
  [ -r "${SCHEMA_BASELINE_FILE}" ] \
    || fail "schema baseline version file not found: ${SCHEMA_BASELINE_FILE}"
  [ -r "${APP_ENV_FILE}" ] \
    || fail "application environment file not found: ${APP_ENV_FILE}"

  local source_version configured_version
  source_version="$(tr -d '[:space:]' < "${SCHEMA_BASELINE_FILE}")"
  [[ "${source_version}" =~ ^[0-9]+(\.[0-9]+)*$ ]] \
    || fail "schema baseline version is invalid: ${source_version}"
  if ! configured_version="$(awk -F= '
    /^[[:space:]]*(#|$)/ { next }
    /^[[:space:]]*(export[[:space:]]+)?PAPER_MES_EXPECTED_SCHEMA_VERSION[[:space:]]*=/ {
      line=$0
      sub(/^[^=]*=/, "", line)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", line)
      if (line ~ /^".*"$/ || line ~ /^'"'"'.*'"'"'$/) line=substr(line, 2, length(line) - 2)
      if (found) duplicate=1
      value=line
      found=1
    }
    END {
      if (duplicate) exit 2
      if (found) print value
    }
  ' "${APP_ENV_FILE}")"; then
    fail "application environment contains duplicate schema version settings"
  fi
  [ -n "${configured_version}" ] \
    || fail "PAPER_MES_EXPECTED_SCHEMA_VERSION is missing from ${APP_ENV_FILE}"
  [[ "${configured_version}" =~ ^[0-9]+(\.[0-9]+)*$ ]] \
    || fail "configured schema version is invalid"
  [ "${configured_version}" = "${source_version}" ] \
    || fail "schema baseline mismatch: source=${source_version}, configured=${configured_version}"
}

env_value() {
  local key="$1"
  awk -F= -v key="${key}" '
    /^[[:space:]]*(#|$)/ { next }
    $1 ~ "^[[:space:]]*(export[[:space:]]+)?" key "[[:space:]]*$" {
      value=$0
      sub(/^[^=]*=/, "", value)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      if (value ~ /^".*"$/ || value ~ /^'"'"'.*'"'"'$/) value=substr(value, 2, length(value) - 2)
      if (found) duplicate=1
      found=1
      result=value
    }
    END {
      if (duplicate) exit 2
      if (found) print result
    }
  ' "${APP_ENV_FILE}"
}

check_ai_configuration() {
  local provider data_mode master_key key_bytes
  provider="$(env_value PAPER_MES_AI_PROVIDER 2>/dev/null)" \
    || fail "application environment contains duplicate AI provider settings"
  data_mode="$(env_value PAPER_MES_AI_DATA_MODE 2>/dev/null)" \
    || fail "application environment contains duplicate AI data mode settings"
  if [ "${provider:-LOCAL_RULES}" = "LOCAL_RULES" ] && [ "${data_mode:-DISABLED}" = "DISABLED" ]; then
    return 0
  fi
  master_key="$(env_value PAPER_MES_AI_CONFIG_MASTER_KEY 2>/dev/null)" \
    || fail "application environment contains duplicate AI master key settings"
  [ -n "${master_key}" ] && [ "${master_key}" != "CHANGE_ME_DIFFERENT_BASE64_32_BYTE_KEY" ] \
    || fail "AI provider credential encryption key is missing while AI is enabled"
  key_bytes="$(printf '%s' "${master_key}" | base64 --decode 2>/dev/null | wc -c)"
  [ "${key_bytes}" = "32" ] \
    || fail "AI provider credential encryption key must decode to exactly 32 bytes"
}

check_backup() {
  [ -d "${BACKUP_ROOT}" ] || fail "backup root not found"
  local latest age_hours
  latest="$(find "${BACKUP_ROOT}" -mindepth 1 -maxdepth 1 -type d -name '????????-??????' \
    -printf '%T@ %p\n' | sort -nr | head -1 | cut -d' ' -f2-)"
  [ -n "${latest}" ] || fail "no completed backup found"
  [ -f "${latest}/SHA256SUMS" ] || fail "latest backup has no checksum manifest"
  age_hours=$(( ($(date +%s) - $(stat -c %Y "${latest}")) / 3600 ))
  (( age_hours <= MAX_BACKUP_AGE_HOURS )) || fail "latest backup is ${age_hours} hours old"
  (cd "${latest}" && sha256sum -c SHA256SUMS >/dev/null) \
    || fail "latest backup checksum verification failed"
}

check_database() {
  require_zero "duplicate pending finish reservation" \
    "SELECT COUNT(*) FROM (SELECT d.finish_uuid FROM biz_delivery_detail d JOIN biz_delivery_order o ON o.uuid=d.delivery_uuid AND o.is_deleted=0 WHERE d.is_deleted=0 AND o.delivery_status=1 AND d.finish_uuid IS NOT NULL AND TRIM(d.finish_uuid)<>'' GROUP BY d.finish_uuid HAVING COUNT(*)>1) conflicts"
  require_zero "duplicate active customer code" \
    "SELECT COUNT(*) FROM (SELECT TRIM(customer_code) FROM sys_customer WHERE is_deleted=0 AND TRIM(customer_code)<>'' GROUP BY TRIM(customer_code) HAVING COUNT(*)>1) conflicts"
  require_zero "duplicate active paper code" \
    "SELECT COUNT(*) FROM (SELECT TRIM(paper_code) FROM sys_paper WHERE is_deleted=0 AND TRIM(paper_code)<>'' GROUP BY TRIM(paper_code) HAVING COUNT(*)>1) conflicts"
  require_zero "duplicate active machine code" \
    "SELECT COUNT(*) FROM (SELECT TRIM(machine_code) FROM sys_machine WHERE is_deleted=0 AND TRIM(machine_code)<>'' GROUP BY TRIM(machine_code) HAVING COUNT(*)>1) conflicts"
  require_zero "duplicate active warehouse code" \
    "SELECT COUNT(*) FROM (SELECT TRIM(warehouse_code) FROM sys_warehouse WHERE is_deleted=0 AND TRIM(warehouse_code)<>'' GROUP BY TRIM(warehouse_code) HAVING COUNT(*)>1) conflicts"

  local task_table_exists
  task_table_exists="$(mysql_query "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='sys_backup_task'")"
  if [ "${task_table_exists}" = "1" ]; then
    require_zero "running backup task" \
      "SELECT COUNT(*) FROM sys_backup_task WHERE is_deleted=0 AND task_status='RUNNING'"
  fi
}

for command_name in mysql curl sha256sum find sort stat grep runuser systemctl tr awk bash base64 wc; do
  command -v "${command_name}" >/dev/null 2>&1 || fail "required command not found: ${command_name}"
done
[[ "${DB_NAME}" =~ ^[A-Za-z0-9_]+$ ]] || fail "invalid DB_NAME"
require_positive_integer DB_PORT "${DB_PORT}"
require_positive_integer MAX_BACKUP_AGE_HOURS "${MAX_BACKUP_AGE_HOURS}"
require_positive_integer HTTP_TIMEOUT_SECONDS "${HTTP_TIMEOUT_SECONDS}"

cat > "${mysql_cnf}" <<EOF
[client]
host=${DB_HOST}
port=${DB_PORT}
user=${DB_USER}
password=${DB_PASSWORD}
default-character-set=utf8mb4
EOF
chmod 600 "${mysql_cnf}"

check_source_provenance
check_schema_version_configuration
check_ai_configuration
check_health
check_app_temp
check_service_temp_runtime
check_backup
check_database
echo "paper-mes release preflight passed"
