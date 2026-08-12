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

write_state() {
  local status="$1" fingerprint="$2" last_alert="$3" temp
  install -d -m 0750 "$(dirname "${STATE_FILE}")"
  temp="$(mktemp "$(dirname "${STATE_FILE}")/.state.XXXXXX")"
  printf 'status=%s\nfingerprint=%s\nlast_alert_epoch=%s\n' \
    "${status}" "${fingerprint}" "${last_alert}" > "${temp}"
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

record_failure_state() {
  local message_en="$1" message_zh="$2" previous_status="$3"
  local previous_fingerprint="$4" previous_alert="$5" fingerprint now remind_after
  fingerprint="$(failure_fingerprint)"
  now="$(date +%s)"
  remind_after=$((REMINDER_HOURS * 3600))
  if [ "${previous_status}" = FAILED ] && [ "${previous_fingerprint}" = "${fingerprint}" ] \
    && [[ "${previous_alert}" =~ ^[0-9]+$ ]] && (( now - previous_alert < remind_after )); then
    write_state FAILED "${fingerprint}" "${previous_alert}"
    return
  fi
  if send_notifications FAILED "${message_en}" "${message_zh}"; then
    write_state FAILED "${fingerprint}" "${now}"
  else
    write_state ALERT_PENDING "${fingerprint}" "${previous_alert:-0}"
  fi
}

record_success_state() {
  local previous_status="$1" now
  now="$(date +%s)"
  case "${previous_status}" in
    FAILED|ALERT_PENDING|RECOVERY_PENDING)
      if send_notifications RECOVERED "all server checks are healthy" \
        "服务器全部检查项均已恢复正常"; then
        write_state UP none "${now}"
      else
        write_state RECOVERY_PENDING none "${now}"
        return 1
      fi
      ;;
    *) write_state UP none "${now}" ;;
  esac
}

process_monitor_result() {
  local previous_status previous_fingerprint previous_alert message_en message_zh
  previous_status="$(read_state_value status)"
  previous_fingerprint="$(read_state_value fingerprint)"
  previous_alert="$(read_state_value last_alert_epoch)"
  if (( ${#issues_en[@]} > 0 )); then
    message_en="server monitor detected ${#issues_en[@]} issue(s): $(join_issues issues_en)"
    message_zh="服务器统一监控发现 ${#issues_zh[@]} 项异常：$(join_issues issues_zh)"
    record_failure_state "${message_en}" "${message_zh}" "${previous_status}" \
      "${previous_fingerprint}" "${previous_alert}"
    echo "server monitor failed: ${message_en}" >&2
    return 1
  fi
  record_success_state "${previous_status}" || return 1
  echo "server monitor ok"
}
