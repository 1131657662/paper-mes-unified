#!/usr/bin/env bash
set -Eeuo pipefail

MONITOR_ENV_FILE="${MONITOR_ENV_FILE:-/etc/paper-mes/server-monitor.env}"
INTERNAL_FAILURE_STATE_FILE="${INTERNAL_FAILURE_STATE_FILE:-/var/lib/server-monitor/internal-failure.state}"

if [ -r "${MONITOR_ENV_FILE}" ]; then
  set -a
  # shellcheck disable=SC1090
  . "${MONITOR_ENV_FILE}"
  set +a
fi

ALERT_EMAIL_COMMAND="${ALERT_EMAIL_COMMAND:-}"
INTERNAL_FAILURE_REMINDER_HOURS="${INTERNAL_FAILURE_REMINDER_HOURS:-2}"
action="${1:-}"
unit="${2:-server-monitor.service}"

[[ "${INTERNAL_FAILURE_REMINDER_HOURS}" =~ ^[1-9][0-9]*$ ]] || {
  echo "INTERNAL_FAILURE_REMINDER_HOURS must be a positive integer" >&2
  exit 2
}
[[ "${unit}" =~ ^[A-Za-z0-9@_.:-]+$ ]] || {
  echo "invalid systemd unit name" >&2
  exit 2
}
[ -n "${ALERT_EMAIL_COMMAND}" ] || {
  echo "ALERT_EMAIL_COMMAND is required" >&2
  exit 2
}

read_last_alert() {
  sed -n 's/^last_alert_epoch=//p' "${INTERNAL_FAILURE_STATE_FILE}" 2>/dev/null \
    | head -1 || true
}

write_failure_state() {
  local now="$1" temp
  install -d -m 0750 "$(dirname "${INTERNAL_FAILURE_STATE_FILE}")"
  temp="$(mktemp "$(dirname "${INTERNAL_FAILURE_STATE_FILE}")/.internal.XXXXXX")"
  printf 'status=FAILED\nlast_alert_epoch=%s\n' "${now}" > "${temp}"
  chmod 0600 "${temp}"
  mv -f "${temp}" "${INTERNAL_FAILURE_STATE_FILE}"
}

notify_failure() {
  local now last_alert reminder_seconds message_en message_zh
  now="$(date +%s)"
  last_alert="$(read_last_alert)"
  reminder_seconds=$((INTERNAL_FAILURE_REMINDER_HOURS * 3600))
  if [[ "${last_alert}" =~ ^[0-9]+$ ]] && (( now - last_alert < reminder_seconds )); then
    return 0
  fi
  message_en="server monitor unit ${unit} failed; inspect its systemd journal"
  message_zh="服务器统一监控器自身运行失败，请检查 ${unit} 的 systemd 日志。"
  "${ALERT_EMAIL_COMMAND}" CRITICAL "${message_en}" monitor-internal "${message_zh}"
  write_failure_state "${now}"
}

notify_recovery() {
  [ -f "${INTERNAL_FAILURE_STATE_FILE}" ] || return 0
  "${ALERT_EMAIL_COMMAND}" RECOVERED "server monitor execution recovered" \
    monitor-internal "服务器统一监控器已恢复正常运行。"
  rm -f "${INTERNAL_FAILURE_STATE_FILE}"
}

case "${action}" in
  failed) notify_failure ;;
  recovered) notify_recovery ;;
  *) echo "usage: $0 {failed|recovered} [systemd-unit]" >&2; exit 2 ;;
esac
