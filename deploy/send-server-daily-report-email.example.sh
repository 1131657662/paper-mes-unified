#!/usr/bin/env bash
set -Eeuo pipefail

REPORT_ENV_FILE="${REPORT_ENV_FILE:-/etc/paper-mes/daily-report.env}"
MSMTP_BIN="${MSMTP_BIN:-/usr/bin/msmtp}"
[ -r "${REPORT_ENV_FILE}" ] || { echo "daily report configuration is not readable" >&2; exit 2; }
set -a
# shellcheck disable=SC1090
. "${REPORT_ENV_FILE}"
set +a

: "${REPORT_EMAIL_FROM:?set REPORT_EMAIL_FROM}"
: "${REPORT_EMAIL_TO:?set REPORT_EMAIL_TO}"
html_file="${1:?HTML report path is required}"
report_date="${2:?report date is required}"
[ -r "${html_file}" ] || { echo "HTML report is not readable" >&2; exit 2; }
[ -x "${MSMTP_BIN}" ] || { echo "msmtp is not installed" >&2; exit 2; }
[[ "${report_date}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]] || { echo "invalid report date" >&2; exit 2; }
email_pattern='^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
[[ "${REPORT_EMAIL_FROM}" =~ ${email_pattern} ]] || { echo "invalid sender address" >&2; exit 2; }
[[ "${REPORT_EMAIL_TO}" =~ ${email_pattern} ]] || { echo "invalid recipient address" >&2; exit 2; }

host="$(hostname | tr -cd 'A-Za-z0-9._-')"
subject="服务器每日运营与安全日报 ${report_date} - ${host}"
encoded_subject="$(printf '%s' "${subject}" | base64 | tr -d '\n')"
{
  printf 'From: =?UTF-8?B?5pyN5Yqh5Zmo5pel5oql?= <%s>\n' "${REPORT_EMAIL_FROM}"
  printf 'To: %s\n' "${REPORT_EMAIL_TO}"
  printf 'Subject: =?UTF-8?B?%s?=\n' "${encoded_subject}"
  printf 'Date: %s\n' "$(LC_ALL=C date -R)"
  printf 'MIME-Version: 1.0\n'
  printf 'Content-Type: text/html; charset=UTF-8\n'
  printf 'Content-Transfer-Encoding: 8bit\n\n'
  cat "${html_file}"
} | "${MSMTP_BIN}" -- "${REPORT_EMAIL_TO}"
