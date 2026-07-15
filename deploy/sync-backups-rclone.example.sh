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
RCLONE_REMOTE="${RCLONE_REMOTE:?set RCLONE_REMOTE to a configured rclone remote name}"
RCLONE_PATH="${RCLONE_PATH:-paper-mes-backups}"
STATUS_FILE="${BACKUP_ROOT}/.remote-sync-status"

fail() {
  echo "$1" >&2
  exit 1
}

[[ "${BACKUP_ROOT}" = /* ]] || fail "BACKUP_ROOT must be an absolute path"
[[ "${RCLONE_REMOTE}" =~ ^[A-Za-z0-9._-]+$ ]] || fail "invalid RCLONE_REMOTE"
[[ "${RCLONE_PATH}" =~ ^[A-Za-z0-9._/-]+$ ]] || fail "invalid RCLONE_PATH"
[[ "${RCLONE_PATH}" != /* && "${RCLONE_PATH}" != *".."* ]] || fail "unsafe RCLONE_PATH"
[ -d "${BACKUP_ROOT}" ] || fail "backup root not found: ${BACKUP_ROOT}"

write_status() {
  local result="$1"
  local temp_file="${STATUS_FILE}.tmp.$$"
  {
    printf 'version=1\n'
    printf 'status=%s\n' "${result}"
    printf 'completed_at=%s\n' "$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
    printf 'remote_name=%s\n' "${RCLONE_REMOTE}"
  } > "${temp_file}"
  mv -f -- "${temp_file}" "${STATUS_FILE}"
}

on_exit() {
  local code=$?
  trap - EXIT
  if [ "${code}" -eq 0 ]; then
    write_status "SUCCESS" || true
  else
    write_status "FAILED" || true
  fi
  exit "${code}"
}

trap on_exit EXIT
command -v rclone >/dev/null 2>&1 || fail "required command not found: rclone"
command -v flock >/dev/null 2>&1 || fail "required command not found: flock"

exec 9>"${BACKUP_ROOT}/.remote-sync.lock"
flock -n 9 || fail "another remote sync is already running"

# copy intentionally keeps older remote backups after local retention removes them.
rclone copy "${BACKUP_ROOT}" "${RCLONE_REMOTE}:${RCLONE_PATH}" \
  --include '/????????-??????/**' \
  --checksum \
  --transfers 2 \
  --checkers 4

echo "remote backup sync completed: ${RCLONE_REMOTE}:${RCLONE_PATH}"
