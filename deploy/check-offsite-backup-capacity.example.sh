#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

RCLONE_CONFIG="${RCLONE_CONFIG:-/etc/rclone/paper-mes.conf}"
RCLONE_REMOTE="${RCLONE_REMOTE:-paper_mes_archive}"
CAPACITY_REMOTE="${CAPACITY_REMOTE:-${RCLONE_REMOTE}:}"
INCLUDE_B2_VERSIONS="${INCLUDE_B2_VERSIONS:-false}"
WARNING_BYTES="${WARNING_BYTES:-8000000000}"
CRITICAL_BYTES="${CRITICAL_BYTES:-9000000000}"
STATE_DIR="${STATE_DIR:-/var/lib/paper-mes}"
STATE_FILE="${STATE_FILE:-${STATE_DIR}/offsite-capacity.state}"
EMAIL_COMMAND="${EMAIL_COMMAND:-/usr/local/bin/send-paper-mes-alert-email}"

fail() {
  echo "$1" >&2
  exit 1
}

require_bytes() {
  [[ "$2" =~ ^[1-9][0-9]*$ ]] || fail "$1 must be a positive integer"
}

read_remote_bytes() {
  local response
  local options=(size "$CAPACITY_REMOTE" --json)
  [ "$INCLUDE_B2_VERSIONS" = false ] || options+=(--b2-versions)
  response="$(rclone "${options[@]}")"
  bytes="$(sed -n 's/.*"bytes"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p' <<< "$response")"
  [[ "$bytes" =~ ^[0-9]+$ ]] || fail "cannot read remote backup size"
}

capacity_level() {
  if (( bytes >= CRITICAL_BYTES )); then
    level=CRITICAL
  elif (( bytes >= WARNING_BYTES )); then
    level=WARNING
  else
    level=NORMAL
  fi
}

send_transition_alert() {
  [ "$level" != "$previous_level" ] || return 0
  case "$level" in
    WARNING) "$EMAIL_COMMAND" WARNING "Backblaze B2 storage usage warning: ${bytes} bytes used; warning threshold is ${WARNING_BYTES} bytes" ;;
    CRITICAL) "$EMAIL_COMMAND" CRITICAL "Backblaze B2 storage usage critical: ${bytes} bytes used; critical threshold is ${CRITICAL_BYTES} bytes" ;;
    NORMAL)
      [ "$previous_level" = WARNING ] || [ "$previous_level" = CRITICAL ] || return 0
      "$EMAIL_COMMAND" RECOVERED "Backblaze B2 storage usage recovered below warning threshold: ${bytes} bytes used"
      ;;
  esac
}

write_state() {
  local temp
  temp="$(mktemp "${STATE_DIR}/.offsite-capacity.XXXXXX")"
  printf 'level=%s\nbytes=%s\nchecked_at=%s\n' \
    "$level" "$bytes" "$(date -u +'%Y-%m-%dT%H:%M:%SZ')" > "$temp"
  chmod 0600 "$temp"
  mv -f "$temp" "$STATE_FILE"
}

require_bytes WARNING_BYTES "$WARNING_BYTES"
require_bytes CRITICAL_BYTES "$CRITICAL_BYTES"
(( CRITICAL_BYTES > WARNING_BYTES )) || fail "critical threshold must exceed warning threshold"
[[ "$RCLONE_REMOTE" =~ ^[A-Za-z0-9._-]+$ ]] || fail "invalid RCLONE_REMOTE"
[[ "$CAPACITY_REMOTE" =~ ^[A-Za-z0-9._:/-]+$ ]] || fail "invalid CAPACITY_REMOTE"
[[ "$INCLUDE_B2_VERSIONS" =~ ^(true|false)$ ]] || fail "invalid INCLUDE_B2_VERSIONS"
command -v rclone >/dev/null 2>&1 || fail "required command not found: rclone"
[ -x "$EMAIL_COMMAND" ] || fail "email command is not executable"
export RCLONE_CONFIG
install -d -m 0750 "$STATE_DIR"
previous_level="$(sed -n 's/^level=//p' "$STATE_FILE" 2>/dev/null || true)"
read_remote_bytes
capacity_level
send_transition_alert
write_state
printf 'off-site capacity check completed: level=%s bytes=%s\n' "$level" "$bytes"
