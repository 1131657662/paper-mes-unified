#!/usr/bin/env bash
set -euo pipefail
umask 077

BACKUP_ENV_FILE="${BACKUP_ENV_FILE:-/etc/paper-mes/backup.env}"
if [ -r "${BACKUP_ENV_FILE}" ]; then
  set -a
  . "${BACKUP_ENV_FILE}"
  set +a
fi

BACKUP_ROOT="${BACKUP_ROOT:-/opt/backups/paper-mes}"
BACKUP_DIR="${BACKUP_DIR:?set BACKUP_DIR to a backup timestamp directory}"
SOURCE_DB_NAME="${SOURCE_DB_NAME:-${DB_NAME:-paper_processing}}"
TARGET_DB_NAME="${TARGET_DB_NAME:?set TARGET_DB_NAME}"
CONFIRM_TARGET_DB_NAME="${CONFIRM_TARGET_DB_NAME:?set CONFIRM_TARGET_DB_NAME}"
PRODUCTION_DB_NAME="${PRODUCTION_DB_NAME:-paper_processing}"
ALLOW_PRODUCTION_RESTORE="${ALLOW_PRODUCTION_RESTORE:-false}"
PRODUCTION_CONFIRMATION="${PRODUCTION_CONFIRMATION:-}"
DB_ADMIN_HOST="${DB_ADMIN_HOST:-${DB_HOST:-127.0.0.1}}"
DB_ADMIN_PORT="${DB_ADMIN_PORT:-${DB_PORT:-3306}}"
DB_ADMIN_USER="${DB_ADMIN_USER:-root}"
DB_ADMIN_PASSWORD="${DB_ADMIN_PASSWORD:?set DB_ADMIN_PASSWORD before restoring}"
PREFLIGHT_DB_NAME="${RESTORE_PREFLIGHT_DB_NAME:-${TARGET_DB_NAME}_restore_preflight}"

mysql_cnf="$(mktemp)"
preflight_created=false

cleanup() {
  local exit_code=$?
  trap - EXIT
  set +e
  if [ "${preflight_created}" = "true" ] && [ -s "${mysql_cnf}" ]; then
    mysql --defaults-extra-file="${mysql_cnf}" \
      -e "DROP DATABASE IF EXISTS \`${PREFLIGHT_DB_NAME}\`;" >/dev/null 2>&1
  fi
  rm -f "${mysql_cnf}"
  exit "${exit_code}"
}
trap cleanup EXIT

fail() {
  echo "$1" >&2
  exit 1
}

require_identifier() {
  local name="$1"
  local value="$2"
  [[ "${value}" =~ ^[A-Za-z0-9_]+$ ]] || fail "invalid ${name}: ${value}"
}

for command_name in mysql gzip sha256sum realpath flock; do
  command -v "${command_name}" >/dev/null 2>&1 || fail "required command not found: ${command_name}"
done

require_identifier "SOURCE_DB_NAME" "${SOURCE_DB_NAME}"
require_identifier "TARGET_DB_NAME" "${TARGET_DB_NAME}"
require_identifier "RESTORE_PREFLIGHT_DB_NAME" "${PREFLIGHT_DB_NAME}"
require_identifier "DB_ADMIN_USER" "${DB_ADMIN_USER}"
[[ "${DB_ADMIN_PORT}" =~ ^[0-9]+$ ]] || fail "invalid DB_ADMIN_PORT: ${DB_ADMIN_PORT}"
[ "${TARGET_DB_NAME}" = "${CONFIRM_TARGET_DB_NAME}" ] || fail "target database confirmation does not match"
[ "${PREFLIGHT_DB_NAME}" != "${TARGET_DB_NAME}" ] || fail "preflight database must differ from target database"
[ "${#PREFLIGHT_DB_NAME}" -le 64 ] || fail "preflight database name exceeds MySQL limit"

backup_root_real="$(realpath -m "${BACKUP_ROOT}")"
backup_dir_real="$(realpath -m "${BACKUP_DIR}")"
case "${backup_dir_real}" in
  "${backup_root_real}"/*) ;;
  *) fail "BACKUP_DIR must be inside BACKUP_ROOT" ;;
esac

if [ "${TARGET_DB_NAME}" = "${PRODUCTION_DB_NAME}" ]; then
  [ "${ALLOW_PRODUCTION_RESTORE}" = "true" ] || fail "production restore is disabled"
  expected="RESTORE-${PRODUCTION_DB_NAME}-FROM-$(basename "${backup_dir_real}")"
  [ "${PRODUCTION_CONFIRMATION}" = "${expected}" ] || fail "production confirmation does not match"
fi

sql_archive="${backup_dir_real}/${SOURCE_DB_NAME}.sql.gz"
[ -f "${sql_archive}" ] || fail "sql archive not found: ${sql_archive}"
[ -f "${backup_dir_real}/SHA256SUMS" ] || fail "checksum file not found"
(cd "${backup_dir_real}" && sha256sum -c SHA256SUMS)
gzip -t "${sql_archive}"

mkdir -p "${BACKUP_ROOT}"
exec 9>"${BACKUP_ROOT}/.restore.lock"
flock -n 9 || fail "another restore is already running"

cat > "${mysql_cnf}" <<EOF
[client]
host=${DB_ADMIN_HOST}
port=${DB_ADMIN_PORT}
user=${DB_ADMIN_USER}
password=${DB_ADMIN_PASSWORD}
default-character-set=utf8mb4
EOF
chmod 600 "${mysql_cnf}"

create_database() {
  local database="$1"
  mysql --defaults-extra-file="${mysql_cnf}" \
    -e "DROP DATABASE IF EXISTS \`${database}\`; CREATE DATABASE \`${database}\` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_0900_ai_ci;"
}

import_database() {
  local database="$1"
  gzip -dc "${sql_archive}" | mysql --defaults-extra-file="${mysql_cnf}" "${database}"
}

assert_required_schema() {
  local database="$1"
  local required_count
  required_count="$(mysql --defaults-extra-file="${mysql_cnf}" -N -B \
    -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${database}' AND table_name='biz_process_order';")"
  [ "${required_count}" -gt 0 ] || fail "restore validation failed: required table biz_process_order is missing"
  mysql --defaults-extra-file="${mysql_cnf}" -N -B "${database}" \
    -e "SELECT COUNT(*) FROM biz_process_order;" >/dev/null
}

create_database "${PREFLIGHT_DB_NAME}"
preflight_created=true
import_database "${PREFLIGHT_DB_NAME}"
assert_required_schema "${PREFLIGHT_DB_NAME}"
mysql --defaults-extra-file="${mysql_cnf}" -e "DROP DATABASE IF EXISTS \`${PREFLIGHT_DB_NAME}\`;"
preflight_created=false

create_database "${TARGET_DB_NAME}"
import_database "${TARGET_DB_NAME}"
assert_required_schema "${TARGET_DB_NAME}"

echo "restore completed: backup=${backup_dir_real}, target_db=${TARGET_DB_NAME}"
