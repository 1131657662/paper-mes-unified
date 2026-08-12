#!/usr/bin/env bash
set -Eeuo pipefail

EMAIL_ALERT_ENV_FILE="${EMAIL_ALERT_ENV_FILE:-/etc/paper-mes/email-alert.env}"
MSMTP_BIN="${MSMTP_BIN:-/usr/bin/msmtp}"

[ -r "${EMAIL_ALERT_ENV_FILE}" ] || {
  echo "email alert configuration is not readable" >&2
  exit 2
}

set -a
# shellcheck disable=SC1090
. "${EMAIL_ALERT_ENV_FILE}"
set +a

status="${1:-}"
message="${2:-}"
: "${ALERT_EMAIL_FROM:?set ALERT_EMAIL_FROM}"
: "${ALERT_EMAIL_TO:?set ALERT_EMAIL_TO}"

case "${status}" in
  FAILED|WARNING|CRITICAL|RECOVERED|TEST) ;;
  *) echo "unsupported alert status" >&2; exit 2 ;;
esac

status_zh() {
  case "$1" in
    FAILED) printf '%s' '故障告警' ;;
    WARNING) printf '%s' '容量预警' ;;
    CRITICAL) printf '%s' '容量紧急告警' ;;
    RECOVERED) printf '%s' '恢复通知' ;;
    TEST) printf '%s' '测试通知' ;;
  esac
}

summary_zh() {
  case "$1" in
    FAILED) printf '%s' '系统监控检测到异常，请查看下方英文技术详情并尽快处理。' ;;
    WARNING) printf '%s' '异地备份空间已达到预警阈值，请关注容量增长。' ;;
    CRITICAL) printf '%s' '异地备份空间已达到紧急阈值，请尽快处理。' ;;
    RECOVERED) printf '%s' '系统监控确认相关异常已经恢复，当前检查正常。' ;;
    TEST) printf '%s' '这是一封告警测试邮件，收到即表示邮件通知通道正常。' ;;
  esac
}

dynamic_message_zh() {
  if [[ "$1" =~ ^Backblaze\ B2\ storage\ usage\ warning:\ ([0-9]+)\ bytes\ used\;\ warning\ threshold\ is\ ([0-9]+)\ bytes$ ]]; then
    printf 'Backblaze B2 存储空间预警：已使用 %s 字节；预警阈值为 %s 字节。' "${BASH_REMATCH[1]}" "${BASH_REMATCH[2]}"
  elif [[ "$1" =~ ^Backblaze\ B2\ storage\ usage\ critical:\ ([0-9]+)\ bytes\ used\;\ critical\ threshold\ is\ ([0-9]+)\ bytes$ ]]; then
    printf 'Backblaze B2 存储空间紧急告警：已使用 %s 字节；紧急阈值为 %s 字节。' "${BASH_REMATCH[1]}" "${BASH_REMATCH[2]}"
  elif [[ "$1" =~ ^Backblaze\ B2\ storage\ usage\ recovered\ below\ warning\ threshold:\ ([0-9]+)\ bytes\ used$ ]]; then
    printf 'Backblaze B2 存储空间已恢复到预警阈值以下：当前使用 %s 字节。' "${BASH_REMATCH[1]}"
  elif [[ "$1" =~ ^Backblaze\ B2\ encrypted\ off-site\ backup\ sync\ failed\ with\ exit\ code\ ([0-9]+)$ ]]; then
    printf 'Backblaze B2 加密异地备份同步失败，退出码为 %s。' "${BASH_REMATCH[1]}"
  elif [[ "$1" =~ ^latest\ backup\ is\ ([0-9]+)\ hours\ old$ ]]; then
    printf '最新备份距今已有 %s 小时。' "${BASH_REMATCH[1]}"
  elif [[ "$1" =~ ^backup\ disk\ free\ space\ is\ below\ ([0-9]+)\ MB$ ]]; then
    printf '备份磁盘可用空间低于 %s MB。' "${BASH_REMATCH[1]}"
  else
    return 1
  fi
}

message_zh() {
  local translated
  if translated="$(dynamic_message_zh "$1")"; then
    printf '%s' "$translated"
    return 0
  fi
  translated="$1"
  translated="${translated//SMTP integration check/邮件通知通道测试}"
  translated="${translated//all checks are healthy/所有检查均正常}"
  translated="${translated//backend health request failed:/后端健康检查请求失败：}"
  translated="${translated//backend health is not UP/后端健康状态不是 UP}"
  translated="${translated//public URL request failed/公网地址请求失败}"
  translated="${translated//backup root not found/备份根目录不存在}"
  translated="${translated//no completed backup found/未找到已完成的备份}"
  translated="${translated//latest backup has no checksum manifest/最新备份缺少校验清单}"
  translated="${translated//Backblaze B2 encrypted off-site backup sync recovered/Backblaze B2 加密异地备份同步已恢复}"
  translated="${translated//Backblaze B2 encrypted off-site backup sync failed/Backblaze B2 加密异地备份同步失败}"
  translated="${translated//Backblaze B2 storage usage warning/Backblaze B2 存储空间预警}"
  translated="${translated//Backblaze B2 storage usage critical/Backblaze B2 存储空间紧急告警}"
  translated="${translated//Backblaze B2 storage usage recovered below warning threshold/Backblaze B2 存储空间已恢复到预警阈值以下}"
  [ "${translated}" != "$1" ] || translated='请查看下方英文技术详情。'
  printf '%s' "${translated}"
}

email_pattern='^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
[[ "${ALERT_EMAIL_FROM}" =~ ${email_pattern} ]] || {
  echo "invalid sender address" >&2
  exit 2
}
[[ "${ALERT_EMAIL_TO}" =~ ${email_pattern} ]] || {
  echo "invalid recipient address" >&2
  exit 2
}
[ -x "${MSMTP_BIN}" ] || {
  echo "msmtp is not installed" >&2
  exit 2
}

host="$(hostname)"
timestamp="$(date --iso-8601=seconds)"
status_cn="$(status_zh "${status}")"
encoded_status_cn="$(printf '%s' "${status_cn}" | base64 | tr -d '\n')"

{
  printf 'From: Paper MES Monitor <%s>\n' "${ALERT_EMAIL_FROM}"
  printf 'To: %s\n' "${ALERT_EMAIL_TO}"
  printf 'Subject: [Paper MES] =?UTF-8?B?%s?= / %s - %s\n' \
    "${encoded_status_cn}" "${status}" "${host}"
  printf 'Date: %s\n' "$(LC_ALL=C date -R)"
  printf 'Content-Type: text/plain; charset=UTF-8\n'
  printf 'Content-Transfer-Encoding: 8bit\n\n'
  printf 'Paper MES 系统监控通知\n\n'
  printf '状态：%s / %s\n服务器：%s\n时间：%s\n说明：%s\n' \
    "${status_cn}" "${status}" "${host}" "${timestamp}" "$(summary_zh "${status}")"
  printf '详情：%s\n' "$(message_zh "${message}")"
  printf '\n--------------------------------------------------\n\n'
  printf 'Paper MES monitoring notification\n\n'
  printf 'Service: paper-mes\nStatus: %s\nHost: %s\nTime: %s\nMessage: %s\n' \
    "${status}" "${host}" "${timestamp}" "${message}"
} | "${MSMTP_BIN}" -- "${ALERT_EMAIL_TO}"
