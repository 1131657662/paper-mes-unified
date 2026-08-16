#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

RCLONE_CONFIG="${RCLONE_CONFIG:-/etc/rclone/paper-mes.conf}"
EMAIL_COMMAND="${EMAIL_COMMAND:-/usr/local/bin/send-paper-mes-alert-email}"
STATE_DIR="${STATE_DIR:-/var/lib/paper-mes}"
STATE_FILE="${STATE_FILE:-${STATE_DIR}/offsite-sync.state}"
LOCK_FILE="${LOCK_FILE:-/run/paper-mes-offsite-sync.lock}"
BUSINESS_SYNC_COMMAND="${BUSINESS_SYNC_COMMAND:-/usr/local/sbin/business-projects-sync-rclone.sh}"
MES_SYNC_COMMAND="${MES_SYNC_COMMAND:-/usr/local/sbin/paper-mes-sync-rclone.sh}"
RETENTION_COMMAND="${RETENTION_COMMAND:-/usr/local/sbin/prune-offsite-backups}"
CAPACITY_COMMAND="${CAPACITY_COMMAND:-/usr/local/sbin/check-offsite-backup-capacity}"
BUSINESS_STATUS_FILE_GROUP="${BUSINESS_STATUS_FILE_GROUP:-root}"
BUSINESS_STATUS_FILE_MODE="${BUSINESS_STATUS_FILE_MODE:-0600}"
MES_STATUS_FILE_GROUP="${MES_STATUS_FILE_GROUP:-paper-mes}"
MES_STATUS_FILE_MODE="${MES_STATUS_FILE_MODE:-0640}"

previous_state="$(cat "$STATE_FILE" 2>/dev/null || true)"
completed=false

write_state() {
  install -d -m 0750 "$STATE_DIR"
  printf '%s\n' "$1" > "$STATE_FILE"
  chmod 0600 "$STATE_FILE"
}

notify() {
  "$EMAIL_COMMAND" "$1" "$2" || {
    logger -t paper-mes-offsite-sync "failed to send $1 email"
    return 1
  }
}

finish() {
  local code=$?
  trap - EXIT
  if [ "$completed" = true ] && [ "$code" -eq 0 ]; then
    if [[ "$previous_state" =~ ^(FAILED|ALERT_PENDING|RECOVERY_PENDING)$ ]]; then
      notify RECOVERED 'Backblaze B2 encrypted off-site backup sync recovered' || {
        write_state RECOVERY_PENDING
        exit 1
      }
    fi
    write_state SUCCESS
    exit 0
  fi
  if [ "$previous_state" != FAILED ]; then
    if ! notify FAILED "Backblaze B2 encrypted off-site backup sync failed with exit code $code"; then
      write_state ALERT_PENDING
      exit "$code"
    fi
  fi
  write_state FAILED
  exit "$code"
}

run_sync() {
  export RCLONE_CONFIG BACKUP_ENV_FILE=/dev/null RCLONE_REMOTE=paper_mes_archive
  STATUS_FILE_GROUP="$BUSINESS_STATUS_FILE_GROUP" STATUS_FILE_MODE="$BUSINESS_STATUS_FILE_MODE" \
    BACKUP_ROOT=/opt/backups/business-projects RCLONE_PATH=business-projects "$BUSINESS_SYNC_COMMAND"
  STATUS_FILE_GROUP="$MES_STATUS_FILE_GROUP" STATUS_FILE_MODE="$MES_STATUS_FILE_MODE" \
    BACKUP_ROOT=/opt/backups/paper-mes RCLONE_PATH=paper-mes "$MES_SYNC_COMMAND"
  "$RETENTION_COMMAND"
  "$CAPACITY_COMMAND"
}

trap finish EXIT
for command_path in "$EMAIL_COMMAND" "$BUSINESS_SYNC_COMMAND" "$MES_SYNC_COMMAND" \
  "$RETENTION_COMMAND" "$CAPACITY_COMMAND"; do
  [ -x "$command_path" ]
done
exec 9>"$LOCK_FILE"
flock -n 9
run_sync
completed=true
