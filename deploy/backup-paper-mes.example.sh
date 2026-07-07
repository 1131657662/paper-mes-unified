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
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-paper_processing}"
DB_USER="${DB_USER:-paper_mes_backup}"
DB_PASSWORD="${DB_PASSWORD:?set DB_PASSWORD or BACKUP_ENV_FILE before running backup}"
UPLOAD_DIR="${UPLOAD_DIR:-/opt/paper-mes/upload}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

timestamp="$(date +%Y%m%d-%H%M%S)"
target_dir="${BACKUP_ROOT}/${timestamp}"
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

require_safe_backup_root() {
  [ -n "${BACKUP_ROOT}" ] || fail "BACKUP_ROOT cannot be empty"
  [[ "${BACKUP_ROOT}" = /* ]] || fail "BACKUP_ROOT must be an absolute path"
  case "${BACKUP_ROOT%/}" in
    ""|"/"|"/opt"|"/opt/backups")
      fail "BACKUP_ROOT is too broad: ${BACKUP_ROOT}"
      ;;
  esac
}

require_safe_backup_root
require_safe_identifier "DB_NAME" "${DB_NAME}"
require_safe_identifier "DB_USER" "${DB_USER}"
require_non_negative_integer "DB_PORT" "${DB_PORT}"
require_non_negative_integer "RETENTION_DAYS" "${RETENTION_DAYS}"

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
  --hex-blob \
  --routines \
  --triggers \
  --events \
  "${DB_NAME}" | gzip > "${target_dir}/${DB_NAME}.sql.gz"

if [ -d "${UPLOAD_DIR}" ]; then
  tar -C "$(dirname "${UPLOAD_DIR}")" -czf "${target_dir}/upload.tar.gz" "$(basename "${UPLOAD_DIR}")"
fi

{
  echo "timestamp=${timestamp}"
  echo "db_host=${DB_HOST}"
  echo "db_port=${DB_PORT}"
  echo "db_name=${DB_NAME}"
  echo "upload_dir=${UPLOAD_DIR}"
} > "${target_dir}/backup-info.txt"

if command -v sha256sum >/dev/null 2>&1; then
  (cd "${target_dir}" && sha256sum ./*.gz > SHA256SUMS)
fi

find "${BACKUP_ROOT}" -mindepth 1 -maxdepth 1 -type d -mtime +"${RETENTION_DAYS}" -exec rm -rf {} +

echo "backup completed: ${target_dir}"
