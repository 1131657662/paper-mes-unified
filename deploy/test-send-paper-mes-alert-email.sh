#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
email_script="${script_dir}/send-paper-mes-alert-email.example.sh"
temp_dir="$(mktemp -d)"
trap 'rm -rf "${temp_dir}"' EXIT

printf '%s\n' \
  'ALERT_EMAIL_FROM=sender@example.com' \
  'ALERT_EMAIL_TO=receiver@example.com' > "${temp_dir}/email-alert.env"
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'printf "%s\n" "$@" > "${MOCK_ARGS_FILE}"' \
  'cat > "${MOCK_MESSAGE_FILE}"' > "${temp_dir}/msmtp"
chmod 700 "${temp_dir}/msmtp"

assert_email() {
  local status="$1"
  local message="$2"
  local detail_cn="$3"
  local status_cn
  case "${status}" in
    FAILED) status_cn='故障告警' ;;
    RECOVERED) status_cn='恢复通知' ;;
    TEST) status_cn='测试通知' ;;
  esac
  MOCK_ARGS_FILE="${temp_dir}/args" \
  MOCK_MESSAGE_FILE="${temp_dir}/message" \
  EMAIL_ALERT_ENV_FILE="${temp_dir}/email-alert.env" \
  MSMTP_BIN="${temp_dir}/msmtp" \
    "${email_script}" "${status}" "${message}"

  grep -Fx -- '--' "${temp_dir}/args"
  grep -Fx 'receiver@example.com' "${temp_dir}/args"
  local encoded_status
  encoded_status="$(sed -n "s/^Subject: \[Paper MES\] =?UTF-8?B?\([^?]*\)?= \/ ${status} - .*$/\1/p" "${temp_dir}/message")"
  [ -n "${encoded_status}" ]
  printf '%s' "${encoded_status}" | base64 --decode | grep -Fx "${status_cn}"
  grep -F "状态：${status_cn} / ${status}" "${temp_dir}/message"
  grep -Fx "详情：${detail_cn}" "${temp_dir}/message"
  grep -Fx -- '--------------------------------------------------' "${temp_dir}/message"
  grep -Fx "Message: ${message}" "${temp_dir}/message"
}

assert_email TEST 'SMTP integration check' '邮件通知通道测试'
assert_email FAILED 'backend health is not UP' '后端健康状态不是 UP'
assert_email RECOVERED 'all checks are healthy' '所有检查均正常'

echo "email alert test passed"
