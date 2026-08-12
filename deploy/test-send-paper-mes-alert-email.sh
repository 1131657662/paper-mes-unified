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

MOCK_ARGS_FILE="${temp_dir}/args" \
MOCK_MESSAGE_FILE="${temp_dir}/message" \
EMAIL_ALERT_ENV_FILE="${temp_dir}/email-alert.env" \
MSMTP_BIN="${temp_dir}/msmtp" \
  "${email_script}" TEST "SMTP integration check"

grep -Fx -- '--' "${temp_dir}/args"
grep -Fx 'receiver@example.com' "${temp_dir}/args"
grep -F 'Subject: [Paper MES] TEST on ' "${temp_dir}/message"
grep -Fx 'Message: SMTP integration check' "${temp_dir}/message"

echo "email alert test passed"
