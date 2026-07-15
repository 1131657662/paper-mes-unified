#!/usr/bin/env bash
set -Eeuo pipefail

MONITOR_ENV_FILE="${MONITOR_ENV_FILE:-/etc/paper-mes/monitor.env}"
if [ -r "${MONITOR_ENV_FILE}" ]; then
  set -a
  # shellcheck disable=SC1090
  . "${MONITOR_ENV_FILE}"
  set +a
fi

HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8081/actuator/health}"
PUBLIC_URL="${PUBLIC_URL:-}"
BACKUP_ROOT="${BACKUP_ROOT:-/opt/backups/paper-mes}"
MAX_BACKUP_AGE_HOURS="${MAX_BACKUP_AGE_HOURS:-48}"
MIN_BACKUP_FREE_MB="${MIN_BACKUP_FREE_MB:-1024}"
HTTP_TIMEOUT_SECONDS="${HTTP_TIMEOUT_SECONDS:-10}"
STATE_FILE="${STATE_FILE:-/var/lib/paper-mes/monitor.state}"
ALERT_WEBHOOK_URL="${ALERT_WEBHOOK_URL:-}"
ALERT_WEBHOOK_BEARER_TOKEN="${ALERT_WEBHOOK_BEARER_TOKEN:-}"

errors=()

require_positive_integer() {
  local name="$1"
  local value="$2"
  [[ "${value}" =~ ^[1-9][0-9]*$ ]] || {
    echo "${name} must be a positive integer" >&2
    exit 2
  }
}

check_health() {
  local response
  if ! response="$(curl --fail --silent --show-error --max-time "${HTTP_TIMEOUT_SECONDS}" "${HEALTH_URL}" 2>&1)"; then
    response="${response//$'\n'/ }"
    response="${response//$'\r'/ }"
    errors+=("backend health request failed: ${response}")
    return
  fi
  grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' <<< "${response}" || \
    errors+=("backend health is not UP")
}

check_public_url() {
  [ -n "${PUBLIC_URL}" ] || return 0
  curl --fail --silent --show-error --max-time "${HTTP_TIMEOUT_SECONDS}" \
    --output /dev/null "${PUBLIC_URL}" || errors+=("public URL request failed")
}

check_backup() {
  [ -d "${BACKUP_ROOT}" ] || {
    errors+=("backup root not found")
    return
  }
  local latest
  latest="$(find "${BACKUP_ROOT}" -mindepth 1 -maxdepth 1 -type d -name '????????-??????' \
    -printf '%T@ %p\n' | sort -nr | head -1 | cut -d' ' -f2-)"
  [ -n "${latest}" ] || {
    errors+=("no completed backup found")
    return
  }
  [ -f "${latest}/SHA256SUMS" ] || errors+=("latest backup has no checksum manifest")
  local age_hours=$(( ($(date +%s) - $(stat -c %Y "${latest}")) / 3600 ))
  (( age_hours <= MAX_BACKUP_AGE_HOURS )) || errors+=("latest backup is ${age_hours} hours old")
}

check_disk() {
  [ -d "${BACKUP_ROOT}" ] || return 0
  local available_kb
  available_kb="$(df -Pk "${BACKUP_ROOT}" | awk 'NR == 2 { print $4 }')"
  [[ "${available_kb}" =~ ^[0-9]+$ ]] || {
    errors+=("cannot read backup disk free space")
    return
  }
  (( available_kb >= MIN_BACKUP_FREE_MB * 1024 )) || errors+=("backup disk free space is below ${MIN_BACKUP_FREE_MB} MB")
}

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

write_webhook_curl_config() {
  printf 'url = "%s"\n' "$(curl_config_escape "${ALERT_WEBHOOK_URL}")"
  printf 'header = "Content-Type: application/json"\n'
  if [ -n "${ALERT_WEBHOOK_BEARER_TOKEN}" ]; then
    printf 'header = "Authorization: Bearer %s"\n' \
      "$(curl_config_escape "${ALERT_WEBHOOK_BEARER_TOKEN}")"
  fi
}

send_webhook() {
  local status="$1"
  local message="$2"
  [ -n "${ALERT_WEBHOOK_URL}" ] || return 0
  local payload
  printf -v payload '{"service":"paper-mes","host":"%s","status":"%s","message":"%s"}' \
    "$(json_escape "$(hostname)")" "$(json_escape "${status}")" "$(json_escape "${message}")"
  if ! printf '%s' "${payload}" | curl --fail --silent --show-error \
    --max-time "${HTTP_TIMEOUT_SECONDS}" --config <(write_webhook_curl_config) \
    --data-binary @- >/dev/null; then
    echo "failed to send monitoring webhook" >&2
    return 1
  fi
}

write_state() {
  install -d -m 0750 "$(dirname "${STATE_FILE}")"
  printf '%s\n' "$1" > "${STATE_FILE}"
}

record_failure() {
  local previous_state="$1"
  local message="$2"
  case "${previous_state}" in
    FAILED|RECOVERY_PENDING)
      write_state FAILED
      ;;
    *)
      if send_webhook FAILED "${message}"; then
        write_state FAILED
      else
        write_state ALERT_PENDING
      fi
      ;;
  esac
}

record_success() {
  local previous_state="$1"
  case "${previous_state}" in
    FAILED|RECOVERY_PENDING)
      if send_webhook RECOVERED "all checks are healthy"; then
        write_state UP
      else
        write_state RECOVERY_PENDING
        return 1
      fi
      ;;
    *)
      write_state UP
      ;;
  esac
}

require_positive_integer MAX_BACKUP_AGE_HOURS "${MAX_BACKUP_AGE_HOURS}"
require_positive_integer MIN_BACKUP_FREE_MB "${MIN_BACKUP_FREE_MB}"
require_positive_integer HTTP_TIMEOUT_SECONDS "${HTTP_TIMEOUT_SECONDS}"
check_health
check_public_url
check_backup
check_disk

previous_state="$(cat "${STATE_FILE}" 2>/dev/null || true)"
if (( ${#errors[@]} > 0 )); then
  message="$(IFS='; '; echo "${errors[*]}")"
  record_failure "${previous_state}" "${message}"
  echo "paper-mes monitor failed: ${message}" >&2
  exit 1
fi

record_success "${previous_state}" || exit 1
echo "paper-mes monitor ok"
