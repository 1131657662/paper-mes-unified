#!/usr/bin/env bash

heartbeat_curl_escape() {
  local value="$1"
  [[ "${value}" != *$'\n'* && "${value}" != *$'\r'* ]] || return 1
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  printf '%s' "${value}"
}

validate_dead_man_switch_url() {
  [ -z "${DEAD_MAN_SWITCH_URL}" ] && return 0
  [[ "${DEAD_MAN_SWITCH_URL}" == https://* ]] || {
    echo "DEAD_MAN_SWITCH_URL must use HTTPS" >&2
    return 1
  }
  heartbeat_curl_escape "${DEAD_MAN_SWITCH_URL}" >/dev/null
}

write_heartbeat_config() {
  printf 'url = "%s"\n' "$(heartbeat_curl_escape "${DEAD_MAN_SWITCH_URL}")"
  printf 'proto = "=https"\n'
}

send_dead_man_switch() {
  [ -n "${DEAD_MAN_SWITCH_URL}" ] || return 0
  validate_dead_man_switch_url
  curl --fail --silent --show-error --max-time "${HTTP_TIMEOUT_SECONDS}" \
    --config <(write_heartbeat_config) --output /dev/null
}
