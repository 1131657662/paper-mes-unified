#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
sync_script="${script_dir}/sync-offsite-backups.example.sh"
temp_dir="$(mktemp -d)"
trap 'rm -rf "${temp_dir}"' EXIT
mkdir -p "${temp_dir}/commands" "${temp_dir}/state"

for name in business mes retention capacity; do
  cat > "${temp_dir}/commands/${name}" <<'EOF'
#!/usr/bin/env bash
name="${0##*/}"
printf '%s:%s:%s\n' "$name" "${STATUS_FILE_GROUP:--}" "${STATUS_FILE_MODE:--}" >> "${CALLS_FILE}"
[ "${FAIL_COMMAND:-}" != "$name" ]
EOF
  chmod 0700 "${temp_dir}/commands/${name}"
done
cat > "${temp_dir}/commands/email" <<'EOF'
#!/usr/bin/env bash
printf 'email:%s\n' "$1" >> "${CALLS_FILE}"
[ "${FAIL_EMAIL:-false}" != true ]
EOF
chmod 0700 "${temp_dir}/commands/email"

run_sync() {
  RCLONE_CONFIG=/dev/null \
  EMAIL_COMMAND="${temp_dir}/commands/email" \
  STATE_DIR="${temp_dir}/state" \
  STATE_FILE="${temp_dir}/state/sync.state" \
  LOCK_FILE="${temp_dir}/sync.lock" \
  BUSINESS_SYNC_COMMAND="${temp_dir}/commands/business" \
  MES_SYNC_COMMAND="${temp_dir}/commands/mes" \
  RETENTION_COMMAND="${temp_dir}/commands/retention" \
  CAPACITY_COMMAND="${temp_dir}/commands/capacity" \
  CALLS_FILE="${temp_dir}/calls" \
  FAIL_COMMAND="${1:-}" \
  FAIL_EMAIL="${2:-false}" \
    bash "${sync_script}"
}

run_sync
grep -Fx SUCCESS "${temp_dir}/state/sync.state"
printf '%s\n' business:root:0600 mes:paper-mes:0640 retention:-:- capacity:-:- \
  | diff - "${temp_dir}/calls"

printf 'FAILED\n' > "${temp_dir}/state/sync.state"
: > "${temp_dir}/calls"
run_sync
grep -Fx 'email:RECOVERED' "${temp_dir}/calls"

: > "${temp_dir}/calls"
if run_sync retention; then
  echo "failed retention task unexpectedly succeeded" >&2
  exit 1
fi
grep -Fx FAILED "${temp_dir}/state/sync.state"
grep -Fx 'email:FAILED' "${temp_dir}/calls"

printf 'ALERT_PENDING\n' > "${temp_dir}/state/sync.state"
: > "${temp_dir}/calls"
if run_sync retention true; then
  echo "failed email unexpectedly succeeded" >&2
  exit 1
fi
grep -Fx ALERT_PENDING "${temp_dir}/state/sync.state"

: > "${temp_dir}/calls"
if run_sync retention; then
  echo "failed retention task unexpectedly succeeded" >&2
  exit 1
fi
grep -Fx FAILED "${temp_dir}/state/sync.state"
grep -Fx 'email:FAILED' "${temp_dir}/calls"

echo "off-site backup orchestration test passed"
