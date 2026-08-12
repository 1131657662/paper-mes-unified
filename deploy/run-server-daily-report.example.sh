#!/usr/bin/env bash
set -Eeuo pipefail

REPORT_ENV_FILE="${REPORT_ENV_FILE:-/etc/paper-mes/daily-report.env}"
GENERATE_COMMAND="${GENERATE_COMMAND:-/usr/local/sbin/generate-server-daily-report}"
SEND_COMMAND="${SEND_COMMAND:-/usr/local/sbin/send-server-daily-report-email}"
[ -r "${REPORT_ENV_FILE}" ] || { echo "daily report configuration is not readable" >&2; exit 2; }
set -a
# shellcheck disable=SC1090
. "${REPORT_ENV_FILE}"
set +a

RETRY_DELAY_SECONDS="${RETRY_DELAY_SECONDS:-600}"
REPORT_DATE="${REPORT_DATE:-$(date -d yesterday +%F)}"
[[ "${RETRY_DELAY_SECONDS}" =~ ^[0-9]+$ ]] && (( RETRY_DELAY_SECONDS <= 3600 )) \
  || { echo "RETRY_DELAY_SECONDS must be between 0 and 3600" >&2; exit 2; }

echo "daily report run started: report_date=${REPORT_DATE}"
html_file="$(REPORT_DATE="${REPORT_DATE}" "${GENERATE_COMMAND}")"
if REPORT_ENV_FILE="${REPORT_ENV_FILE}" "${SEND_COMMAND}" "${html_file}" "${REPORT_DATE}"; then
  echo "daily report sent: report_date=${REPORT_DATE} attempt=1"
  exit 0
fi
echo "daily report send failed: report_date=${REPORT_DATE} attempt=1; retry_in=${RETRY_DELAY_SECONDS}s" >&2
sleep "${RETRY_DELAY_SECONDS}"
REPORT_ENV_FILE="${REPORT_ENV_FILE}" "${SEND_COMMAND}" "${html_file}" "${REPORT_DATE}"
echo "daily report sent: report_date=${REPORT_DATE} attempt=2"
