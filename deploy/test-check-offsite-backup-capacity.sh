#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
capacity_script="${script_dir}/check-offsite-backup-capacity.example.sh"
temp_dir="$(mktemp -d)"
trap 'rm -rf "${temp_dir}"' EXIT
mkdir -p "${temp_dir}/bin" "${temp_dir}/state"

cat > "${temp_dir}/bin/rclone" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$@" > "${RCLONE_ARGS_FILE}"
printf '{"count":1,"bytes":%s}\n' "${MOCK_REMOTE_BYTES}"
EOF
cat > "${temp_dir}/email" <<'EOF'
#!/usr/bin/env bash
printf '%s\t%s\n' "$1" "$2" >> "${EMAIL_CALLS_FILE}"
EOF
chmod 0700 "${temp_dir}/bin/rclone" "${temp_dir}/email"

run_check() {
  PATH="${temp_dir}/bin:${PATH}" \
  RCLONE_CONFIG=/dev/null \
  RCLONE_REMOTE=test_remote \
  CAPACITY_REMOTE='b2_remote:test-bucket/encrypted' \
  INCLUDE_B2_VERSIONS=true \
  WARNING_BYTES=8000000000 \
  CRITICAL_BYTES=9000000000 \
  STATE_DIR="${temp_dir}/state" \
  STATE_FILE="${temp_dir}/state/capacity.state" \
  EMAIL_COMMAND="${temp_dir}/email" \
  EMAIL_CALLS_FILE="${temp_dir}/email.calls" \
  RCLONE_ARGS_FILE="${temp_dir}/rclone.args" \
  MOCK_REMOTE_BYTES="$1" \
    bash "${capacity_script}"
}

run_check 100000000 >/dev/null
[ ! -e "${temp_dir}/email.calls" ]
grep -Fx -- '--b2-versions' "${temp_dir}/rclone.args"
run_check 8100000000 >/dev/null
grep -F $'WARNING\tBackblaze B2 storage usage warning:' "${temp_dir}/email.calls"
before="$(wc -l < "${temp_dir}/email.calls")"
run_check 8200000000 >/dev/null
[ "$(wc -l < "${temp_dir}/email.calls")" -eq "$before" ]
run_check 9100000000 >/dev/null
grep -F $'CRITICAL\tBackblaze B2 storage usage critical:' "${temp_dir}/email.calls"
run_check 100000000 >/dev/null
grep -F $'RECOVERED\tBackblaze B2 storage usage recovered below warning threshold:' "${temp_dir}/email.calls"
grep -Fx 'level=NORMAL' "${temp_dir}/state/capacity.state"

echo "off-site capacity test passed"
