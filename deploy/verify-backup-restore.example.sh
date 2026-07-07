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
SOURCE_DB_NAME="${SOURCE_DB_NAME:-${DB_NAME:-paper_processing}}"
RESTORE_DB_NAME="${RESTORE_DB_NAME:-paper_mes_restore_check}"
DB_ADMIN_HOST="${DB_ADMIN_HOST:-${DB_HOST:-127.0.0.1}}"
DB_ADMIN_PORT="${DB_ADMIN_PORT:-${DB_PORT:-3306}}"
DB_ADMIN_USER="${DB_ADMIN_USER:-root}"
DB_ADMIN_PASSWORD="${DB_ADMIN_PASSWORD:?set DB_ADMIN_PASSWORD before running restore check}"
DROP_AFTER_VERIFY="${DROP_AFTER_VERIFY:-true}"

mysql_cnf="$(mktemp)"

cleanup() {
  rm -f "${mysql_cnf}"
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

if [ "${SOURCE_DB_NAME}" = "${RESTORE_DB_NAME}" ]; then
  fail "RESTORE_DB_NAME must not equal SOURCE_DB_NAME"
fi

[ -d "${BACKUP_DIR}" ] || fail "backup directory not found: ${BACKUP_DIR}"
sql_archive="${BACKUP_DIR}/${SOURCE_DB_NAME}.sql.gz"
upload_archive="${BACKUP_DIR}/upload.tar.gz"
[ -f "${sql_archive}" ] || fail "sql archive not found: ${sql_archive}"

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

if [ -f "${BACKUP_DIR}/SHA256SUMS" ] && command -v sha256sum >/dev/null 2>&1; then
  (cd "${BACKUP_DIR}" && sha256sum -c SHA256SUMS)
fi

mysql --defaults-extra-file="${mysql_cnf}" \
  -e "DROP DATABASE IF EXISTS \`${RESTORE_DB_NAME}\`; CREATE DATABASE \`${RESTORE_DB_NAME}\` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_0900_ai_ci;"

gzip -dc "${sql_archive}" | mysql --defaults-extra-file="${mysql_cnf}" "${RESTORE_DB_NAME}"

table_count="$(mysql --defaults-extra-file="${mysql_cnf}" -N -B \
  -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${RESTORE_DB_NAME}';")"
order_count="$(mysql --defaults-extra-file="${mysql_cnf}" -N -B "${RESTORE_DB_NAME}" \
  -e "SELECT COUNT(*) FROM biz_process_order;" 2>/dev/null || echo "n/a")"

if [ "${table_count}" -le 0 ]; then
  fail "restore check failed: restored database has no tables"
fi

if [ -f "${upload_archive}" ]; then
  tar -tzf "${upload_archive}" >/dev/null
fi

if [ "${DROP_AFTER_VERIFY}" = "true" ]; then
  mysql --defaults-extra-file="${mysql_cnf}" -e "DROP DATABASE IF EXISTS \`${RESTORE_DB_NAME}\`;"
fi

echo "restore check completed: tables=${table_count}, process_orders=${order_count}, restore_db=${RESTORE_DB_NAME}, dropped=${DROP_AFTER_VERIFY}"
