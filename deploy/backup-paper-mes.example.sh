#!/usr/bin/env bash
set -euo pipefail

BACKUP_ROOT="${BACKUP_ROOT:-/opt/backups/paper-mes}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-paper_processing}"
DB_USER="${DB_USER:-paper_mes_backup}"
DB_PASSWORD="${DB_PASSWORD:?set DB_PASSWORD before running backup}"
UPLOAD_DIR="${UPLOAD_DIR:-/opt/paper-mes/upload}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

timestamp="$(date +%Y%m%d-%H%M%S)"
target_dir="${BACKUP_ROOT}/${timestamp}"
mysql_cnf="$(mktemp)"

if [ "${BACKUP_ROOT}" = "/" ] || [ -z "${BACKUP_ROOT}" ]; then
  echo "invalid BACKUP_ROOT: ${BACKUP_ROOT}" >&2
  exit 1
fi

cleanup() {
  rm -f "${mysql_cnf}"
}
trap cleanup EXIT

mkdir -p "${target_dir}"
chmod 700 "${target_dir}"

cat > "${mysql_cnf}" <<EOF
[client]
host=${DB_HOST}
port=${DB_PORT}
user=${DB_USER}
password=${DB_PASSWORD}
default-character-set=utf8mb4
EOF
chmod 600 "${mysql_cnf}"

mysqldump \
  --defaults-extra-file="${mysql_cnf}" \
  --no-tablespaces \
  --single-transaction \
  --routines \
  --triggers \
  "${DB_NAME}" | gzip > "${target_dir}/${DB_NAME}.sql.gz"

if [ -d "${UPLOAD_DIR}" ]; then
  tar -C "$(dirname "${UPLOAD_DIR}")" -czf "${target_dir}/upload.tar.gz" "$(basename "${UPLOAD_DIR}")"
fi

find "${BACKUP_ROOT}" -mindepth 1 -maxdepth 1 -type d -mtime +"${RETENTION_DAYS}" -exec rm -rf {} +

echo "backup completed: ${target_dir}"
