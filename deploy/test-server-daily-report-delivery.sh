#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
temp_dir="$(mktemp -d)"
trap 'rm -rf -- "${temp_dir}"' EXIT

printf '%s\n' 'REPORT_EMAIL_FROM=sender@example.com' 'REPORT_EMAIL_TO=receiver@example.com' \
  'RETRY_DELAY_SECONDS=0' > "${temp_dir}/report.env"
printf '<html><body>中文日报</body></html>\n' > "${temp_dir}/report.html"
cat > "${temp_dir}/msmtp" <<'EOF'
#!/usr/bin/env bash
cat > "${MOCK_MESSAGE_FILE}"
EOF
chmod 700 "${temp_dir}/msmtp"
MOCK_MESSAGE_FILE="${temp_dir}/message" REPORT_ENV_FILE="${temp_dir}/report.env" \
  MSMTP_BIN="${temp_dir}/msmtp" bash "${script_dir}/send-server-daily-report-email.example.sh" \
  "${temp_dir}/report.html" 2026-08-12
grep -Fq 'Content-Type: text/html; charset=UTF-8' "${temp_dir}/message"
grep -Fq '中文日报' "${temp_dir}/message"
grep -Fq 'receiver@example.com' "${temp_dir}/message"
! grep -Fq 'password' "${temp_dir}/message"

cat > "${temp_dir}/generate" <<EOF
#!/usr/bin/env bash
printf '%s\n' '${temp_dir}/report.html'
EOF
cat > "${temp_dir}/send" <<'EOF'
#!/usr/bin/env bash
count="$(cat "${ATTEMPT_FILE}" 2>/dev/null || printf 0)"
count=$((count + 1))
printf '%s' "${count}" > "${ATTEMPT_FILE}"
(( count >= 2 ))
EOF
chmod 700 "${temp_dir}/generate" "${temp_dir}/send"
ATTEMPT_FILE="${temp_dir}/attempts" REPORT_ENV_FILE="${temp_dir}/report.env" \
  GENERATE_COMMAND="${temp_dir}/generate" SEND_COMMAND="${temp_dir}/send" \
  bash "${script_dir}/run-server-daily-report.example.sh"
[ "$(cat "${temp_dir}/attempts")" = 2 ]

printf '%s\n' 'server daily report delivery tests passed'
