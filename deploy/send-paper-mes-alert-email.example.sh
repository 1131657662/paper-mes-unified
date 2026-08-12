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
  FAILED|RECOVERED|TEST) ;;
  *) echo "unsupported alert status" >&2; exit 2 ;;
esac

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

{
  printf 'From: Paper MES Monitor <%s>\n' "${ALERT_EMAIL_FROM}"
  printf 'To: %s\n' "${ALERT_EMAIL_TO}"
  printf 'Subject: [Paper MES] %s on %s\n' "${status}" "$(hostname)"
  printf 'Date: %s\n' "$(LC_ALL=C date -R)"
  printf 'Content-Type: text/plain; charset=UTF-8\n'
  printf 'Content-Transfer-Encoding: 8bit\n\n'
  printf 'Service: paper-mes\nStatus: %s\nHost: %s\nTime: %s\nMessage: %s\n' \
    "${status}" "$(hostname)" "$(date --iso-8601=seconds)" "${message}"
} | "${MSMTP_BIN}" -- "${ALERT_EMAIL_TO}"
