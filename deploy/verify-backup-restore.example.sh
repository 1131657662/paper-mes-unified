#!/usr/bin/env bash
set -euo pipefail
umask 077

BACKUP_ENV_FILE="${BACKUP_ENV_FILE:-/etc/paper-mes/backup.env}"
if [ -r "${BACKUP_ENV_FILE}" ]; then
  set -a
  . "${BACKUP_ENV_FILE}"
  set +a
fi

BACKUP_DIR="${BACKUP_DIR:?set BACKUP_DIR to a backup timestamp directory}"
BACKUP_ROOT="${BACKUP_ROOT:-/opt/backups/paper-mes}"
SOURCE_DB_NAME="${SOURCE_DB_NAME:-${DB_NAME:-paper_processing}}"
RESTORE_DB_NAME="${RESTORE_DB_NAME:-paper_mes_restore_check}"
DB_ADMIN_HOST="${DB_ADMIN_HOST:-${DB_HOST:-127.0.0.1}}"
DB_ADMIN_PORT="${DB_ADMIN_PORT:-${DB_PORT:-3306}}"
DB_ADMIN_USER="${DB_ADMIN_USER:-root}"
DB_ADMIN_PASSWORD="${DB_ADMIN_PASSWORD:?set DB_ADMIN_PASSWORD before running restore check}"
DROP_AFTER_VERIFY="${DROP_AFTER_VERIFY:-true}"

mysql_cnf="$(mktemp)"
restore_created=false

cleanup() {
  local exit_code=$?
  local cleanup_failed=0
  trap - EXIT
  set +e
  if [ "${restore_created}" = "true" ] \
      && { [ "${exit_code}" -ne 0 ] || [ "${DROP_AFTER_VERIFY}" = "true" ]; }; then
    mysql --defaults-extra-file="${mysql_cnf}" \
      -e "DROP DATABASE IF EXISTS \`${RESTORE_DB_NAME}\`;"
    cleanup_failed=$?
    if [ "${cleanup_failed}" -ne 0 ]; then
      echo "failed to clean restore database: ${RESTORE_DB_NAME}" >&2
    fi
  fi
  rm -f "${mysql_cnf}"
  if [ "${exit_code}" -eq 0 ] && [ "${cleanup_failed}" -ne 0 ]; then
    exit_code=${cleanup_failed}
  fi
  exit "${exit_code}"
}
trap cleanup EXIT

fail() {
  echo "$1" >&2
  exit 1
}

require_safe_identifier() {
  local name="$1"
  local value="$2"
  [[ "${value}" =~ ^[A-Za-z0-9_]+$ ]] || fail "invalid ${name}: ${value}"
}

require_non_negative_integer() {
  local name="$1"
  local value="$2"
  [[ "${value}" =~ ^[0-9]+$ ]] || fail "invalid ${name}: ${value}"
}

require_safe_identifier "SOURCE_DB_NAME" "${SOURCE_DB_NAME}"
require_safe_identifier "RESTORE_DB_NAME" "${RESTORE_DB_NAME}"
require_safe_identifier "DB_ADMIN_USER" "${DB_ADMIN_USER}"
require_non_negative_integer "DB_ADMIN_PORT" "${DB_ADMIN_PORT}"

for command_name in mysql gzip tar sha256sum realpath flock; do
  command -v "${command_name}" >/dev/null 2>&1 || fail "required command not found: ${command_name}"
done

backup_root_real="$(realpath -m "${BACKUP_ROOT}")"
backup_dir_real="$(realpath -m "${BACKUP_DIR}")"
case "${backup_dir_real}" in
  "${backup_root_real}"/*) ;;
  *) fail "BACKUP_DIR must be inside BACKUP_ROOT" ;;
esac
VERIFY_REPORT_FILE="${backup_dir_real}/restore-check.txt"

mkdir -p "${BACKUP_ROOT}"
exec 9>"${BACKUP_ROOT}/.restore-check.lock"
flock -n 9 || fail "another restore check is already running"

if [ "${SOURCE_DB_NAME}" = "${RESTORE_DB_NAME}" ]; then
  fail "RESTORE_DB_NAME must not equal SOURCE_DB_NAME"
fi

[ -d "${BACKUP_DIR}" ] || fail "backup directory not found: ${BACKUP_DIR}"
sql_archive="${BACKUP_DIR}/${SOURCE_DB_NAME}.sql.gz"
upload_archive="${BACKUP_DIR}/upload.tar.gz"
[ -f "${sql_archive}" ] || fail "sql archive not found: ${sql_archive}"
[ -f "${BACKUP_DIR}/SHA256SUMS" ] || fail "checksum file not found: ${BACKUP_DIR}/SHA256SUMS"

cat > "${mysql_cnf}" <<EOF
[client]
host=${DB_ADMIN_HOST}
port=${DB_ADMIN_PORT}
user=${DB_ADMIN_USER}
password=${DB_ADMIN_PASSWORD}
default-character-set=utf8mb4
EOF
chmod 600 "${mysql_cnf}"

gzip -t "${sql_archive}"

(cd "${BACKUP_DIR}" && sha256sum -c SHA256SUMS)

mysql --defaults-extra-file="${mysql_cnf}" \
  -e "DROP DATABASE IF EXISTS \`${RESTORE_DB_NAME}\`; CREATE DATABASE \`${RESTORE_DB_NAME}\` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_0900_ai_ci;"
restore_created=true

gzip -dc "${sql_archive}" | mysql --defaults-extra-file="${mysql_cnf}" "${RESTORE_DB_NAME}"

table_count="$(mysql --defaults-extra-file="${mysql_cnf}" -N -B \
  -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${RESTORE_DB_NAME}';")"
if [ "${table_count}" -le 0 ]; then
  fail "restore check failed: restored database has no tables"
fi

order_table_count="$(mysql --defaults-extra-file="${mysql_cnf}" -N -B \
  -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${RESTORE_DB_NAME}' AND table_name='biz_process_order';")"
if [ "${order_table_count}" -le 0 ]; then
  fail "restore check failed: required table biz_process_order is missing"
fi

order_count="$(mysql --defaults-extra-file="${mysql_cnf}" -N -B "${RESTORE_DB_NAME}" \
  -e "SELECT COUNT(*) FROM biz_process_order;")"

if [ -f "${upload_archive}" ]; then
  tar -tzf "${upload_archive}" >/dev/null
fi

if [ "${DROP_AFTER_VERIFY}" = "true" ]; then
  mysql --defaults-extra-file="${mysql_cnf}" -e "DROP DATABASE IF EXISTS \`${RESTORE_DB_NAME}\`;"
  restore_created=false
fi

{
  echo "verified_at=$(date --iso-8601=seconds)"
  echo "backup_dir=${backup_dir_real}"
  echo "restore_db=${RESTORE_DB_NAME}"
  echo "table_count=${table_count}"
  echo "process_order_count=${order_count}"
  echo "dropped_after_verify=${DROP_AFTER_VERIFY}"
} > "${VERIFY_REPORT_FILE}"

echo "restore check completed: tables=${table_count}, process_orders=${order_count}, restore_db=${RESTORE_DB_NAME}, dropped=${DROP_AFTER_VERIFY}"
