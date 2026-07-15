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

for command_name in mysql curl sha256sum find sort stat grep; do
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

check_health
check_backup
check_database
echo "paper-mes release preflight passed"
