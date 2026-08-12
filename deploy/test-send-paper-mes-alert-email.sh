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
  local source="${4:-paper-mes}"
  local subject_prefix='Paper MES'
  local status_cn
  case "${status}" in
    FAILED) status_cn='故障告警' ;;
    WARNING) status_cn='预警通知' ;;
    CRITICAL) status_cn='紧急告警' ;;
    RECOVERED) status_cn='恢复通知' ;;
    TEST) status_cn='测试通知' ;;
  esac
  if [ "${source}" = server ] || [ "${source}" = monitor-internal ]; then
    MOCK_ARGS_FILE="${temp_dir}/args" MOCK_MESSAGE_FILE="${temp_dir}/message" \
    EMAIL_ALERT_ENV_FILE="${temp_dir}/email-alert.env" MSMTP_BIN="${temp_dir}/msmtp" \
      bash "${email_script}" "${status}" "${message}" "${source}" "${detail_cn}"
  else
    MOCK_ARGS_FILE="${temp_dir}/args" MOCK_MESSAGE_FILE="${temp_dir}/message" \
    EMAIL_ALERT_ENV_FILE="${temp_dir}/email-alert.env" MSMTP_BIN="${temp_dir}/msmtp" \
      bash "${email_script}" "${status}" "${message}"
  fi

  grep -Fx -- '--' "${temp_dir}/args"
  grep -Fx 'receiver@example.com' "${temp_dir}/args"
  local encoded_status
  [ "${source}" != server ] || subject_prefix='Server Monitor'
  [ "${source}" != monitor-internal ] || subject_prefix='Monitor Watchdog'
  encoded_status="$(sed -n "s/^Subject: \[${subject_prefix}\] =?UTF-8?B?\([^?]*\)?= \/ ${status} - .*$/\1/p" "${temp_dir}/message")"
  [ -n "${encoded_status}" ]
  printf '%s' "${encoded_status}" | base64 --decode | grep -Fx "${status_cn}"
  grep -F "状态：${status_cn} / ${status}" "${temp_dir}/message"
  grep -Fx "详情：${detail_cn}" "${temp_dir}/message"
  grep -Fx -- '--------------------------------------------------' "${temp_dir}/message"
  grep -Fx "Message: ${message}" "${temp_dir}/message"
}

assert_email TEST 'SMTP integration check' '邮件通知通道测试'
assert_email FAILED 'backend health is not UP' '后端健康状态不是 UP'
assert_email WARNING 'Backblaze B2 storage usage warning: 8100000000 bytes used; warning threshold is 8000000000 bytes' \
  'Backblaze B2 存储空间预警：已使用 8100000000 字节；预警阈值为 8000000000 字节。'
assert_email CRITICAL 'Backblaze B2 storage usage critical: 9100000000 bytes used; critical threshold is 9000000000 bytes' \
  'Backblaze B2 存储空间紧急告警：已使用 9100000000 字节；紧急阈值为 9000000000 字节。'
assert_email RECOVERED 'all checks are healthy' '所有检查均正常'
assert_email FAILED 'server monitor detected 1 issue(s): nginx is down' \
  '服务器统一监控发现 1 项异常：Nginx 未运行' server
assert_email CRITICAL 'server monitor unit server-monitor.service failed; inspect its systemd journal' \
  '服务器统一监控器自身运行失败，请检查 server-monitor.service 的 systemd 日志。' monitor-internal

echo "email alert test passed"
