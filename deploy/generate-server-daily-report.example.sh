#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

SCRIPT_DIR="${SCRIPT_DIR:-/usr/local/lib/server-daily-report}"
STATE_DIR="${STATE_DIR:-/var/lib/server-daily-report}"
RETENTION_DAYS="${RETENTION_DAYS:-35}"
REPORT_DATE="${REPORT_DATE:-$(date -d yesterday +%F)}"
[[ "${RETENTION_DAYS}" =~ ^[1-9][0-9]*$ ]] || { echo "RETENTION_DAYS must be positive" >&2; exit 2; }
[[ "${REPORT_DATE}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]] || { echo "invalid REPORT_DATE" >&2; exit 2; }
[[ "${STATE_DIR}" = /* && "${STATE_DIR}" != / && "${STATE_DIR}" != /var ]] || { echo "unsafe STATE_DIR" >&2; exit 2; }

json_file="${STATE_DIR}/server-daily-report-${REPORT_DATE}.json"
html_file="${STATE_DIR}/server-daily-report-${REPORT_DATE}.html"
json_temp="$(mktemp "${STATE_DIR}/.report-json.XXXXXX")"
html_temp="$(mktemp "${STATE_DIR}/.report-html.XXXXXX")"
cleanup() { rm -f -- "${json_temp}" "${html_temp}"; }
trap cleanup EXIT

REPORT_DATE="${REPORT_DATE}" python3 "${SCRIPT_DIR}/collect-server-daily-report.py" > "${json_temp}"
python3 -m json.tool "${json_temp}" >/dev/null
python3 "${SCRIPT_DIR}/render-server-daily-report.py" "${json_temp}" "${html_temp}"
grep -Fq '<html' "${html_temp}" || { echo "HTML report validation failed" >&2; exit 1; }
mv -f -- "${json_temp}" "${json_file}"
mv -f -- "${html_temp}" "${html_file}"
find "${STATE_DIR}" -maxdepth 1 -type f -name 'server-daily-report-*.*' -mtime "+${RETENTION_DAYS}" -delete
printf '%s\n' "${html_file}"
