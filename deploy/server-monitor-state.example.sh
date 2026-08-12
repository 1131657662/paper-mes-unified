#!/usr/bin/env bash

json_escape() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//$'\n'/\\n}"
  value="${value//$'\r'/\\r}"
  value="${value//$'\t'/\\t}"
  printf '%s' "${value}"
}

curl_config_escape() {
  local value="$1"
  [[ "${value}" != *$'\n'* && "${value}" != *$'\r'* ]] || return 1
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  printf '%s' "${value}"
}

write_webhook_config() {
  printf 'url = "%s"\n' "$(curl_config_escape "${ALERT_WEBHOOK_URL}")"
  printf 'header = "Content-Type: application/json"\n'
  if [ -n "${ALERT_WEBHOOK_BEARER_TOKEN}" ]; then
    printf 'header = "Authorization: Bearer %s"\n' \
      "$(curl_config_escape "${ALERT_WEBHOOK_BEARER_TOKEN}")"
  fi
}

send_webhook() {
  local status="$1" message="$2" payload
  [ -n "${ALERT_WEBHOOK_URL}" ] || return 0
  printf -v payload '{"service":"server-monitor","host":"%s","status":"%s","message":"%s"}' \
    "$(json_escape "$(hostname)")" "$(json_escape "${status}")" "$(json_escape "${message}")"
  printf '%s' "${payload}" | curl --fail --silent --show-error \
    --max-time "${HTTP_TIMEOUT_SECONDS}" --config <(write_webhook_config) \
    --data-binary @- >/dev/null
}

send_notifications() {
  local status="$1" message_en="$2" message_zh="$3" failed=0
  send_webhook "${status}" "${message_en}" || failed=1
  if [ -n "${ALERT_EMAIL_COMMAND}" ]; then
    "${ALERT_EMAIL_COMMAND}" "${status}" "${message_en}" server "${message_zh}" || failed=1
  fi
  return "${failed}"
}

read_state_value() {
  sed -n "s/^$1=//p" "${STATE_FILE}" 2>/dev/null | head -1 || true
}

valid_epoch_or_zero() {
  [[ "$1" =~ ^[0-9]+$ ]] && printf '%s' "$1" || printf '0'
}

load_monitor_state() {
  state_status="$(read_state_value status)"
  state_fingerprint="$(read_state_value fingerprint)"
  state_last_run="$(valid_epoch_or_zero "$(read_state_value last_run_epoch)")"
  state_last_success="$(valid_epoch_or_zero "$(read_state_value last_success_epoch)")"
  state_last_alert="$(valid_epoch_or_zero "$(read_state_value last_alert_epoch)")"
  state_status="${state_status:-UNKNOWN}"
  state_fingerprint="${state_fingerprint:-none}"
}

save_monitor_state() {
  local temp
  install -d -m 0750 "$(dirname "${STATE_FILE}")"
  temp="$(mktemp "$(dirname "${STATE_FILE}")/.state.XXXXXX")"
  printf 'version=2\nstatus=%s\nfingerprint=%s\n' \
    "${state_status}" "${state_fingerprint}" > "${temp}"
  printf 'last_run_epoch=%s\nlast_success_epoch=%s\nlast_alert_epoch=%s\n' \
    "${state_last_run}" "${state_last_success}" "${state_last_alert}" >> "${temp}"
  chmod 0600 "${temp}"
  mv -f "${temp}" "${STATE_FILE}"
}

join_issues() {
  local -n entries="$1"
  local joined="" entry
  for entry in "${entries[@]}"; do
    [ -z "${joined}" ] || joined+="; "
    joined+="${entry}"
  done
  printf '%s' "${joined}"
}

failure_fingerprint() {
  printf '%s\n' "${issue_keys[@]}" | sha256sum | awk '{print $1}'
}

failure_notification_due() {
  local fingerprint="$1" now="$2" remind_after
  [ "${state_status}" = FAILED ] || return 0
  [ "${state_fingerprint}" = "${fingerprint}" ] || return 0
  remind_after=$((REMINDER_HOURS * 3600))
  (( now - state_last_alert >= remind_after ))
}

record_failure_state() {
  local message_en="$1" message_zh="$2" fingerprint now
  fingerprint="$(failure_fingerprint)"
  now="$(date +%s)"
  state_last_run="${now}"
  if ! failure_notification_due "${fingerprint}" "${now}"; then
    state_status=FAILED
    state_fingerprint="${fingerprint}"
    save_monitor_state
    return 0
  fi
  if send_notifications FAILED "${message_en}" "${message_zh}"; then
    state_status=FAILED
    state_fingerprint="${fingerprint}"
    state_last_alert="${now}"
    save_monitor_state
    return 0
  fi
  state_status=ALERT_PENDING
  state_fingerprint="${fingerprint}"
  save_monitor_state
  return 1
}

record_success_state() {
  local previous_status now
  previous_status="${state_status}"
  now="$(date +%s)"
  state_last_run="${now}"
  state_last_success="${now}"
  state_fingerprint=none
  case "${previous_status}" in
    FAILED|ALERT_PENDING|RECOVERY_PENDING)
      if send_notifications RECOVERED "all server checks are healthy" \
        "服务器全部检查项均已恢复正常"; then
        state_status=UP
        state_last_alert="${now}"
        save_monitor_state
      else
        state_status=RECOVERY_PENDING
        save_monitor_state
        return 1
      fi
      ;;
    *) state_status=UP; save_monitor_state ;;
  esac
}

process_monitor_result() {
  local message_en message_zh
  load_monitor_state
  if (( ${#issues_en[@]} > 0 )); then
    message_en="server monitor detected ${#issues_en[@]} issue(s): $(join_issues issues_en)"
    message_zh="服务器统一监控发现 ${#issues_zh[@]} 项异常：$(join_issues issues_zh)"
    record_failure_state "${message_en}" "${message_zh}" || return 1
    echo "server monitor failed: ${message_en}" >&2
    return 3
  fi
  record_success_state || return 1
  echo "server monitor ok"
}
