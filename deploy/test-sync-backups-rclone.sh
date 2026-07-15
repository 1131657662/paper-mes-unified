#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
sync_script="${script_dir}/sync-backups-rclone.example.sh"
temp_dir="$(mktemp -d)"

cleanup() {
  rm -rf "${temp_dir}"
}
trap cleanup EXIT

mkdir -p "${temp_dir}/bin" "${temp_dir}/backups/20260715-023500"
cat > "${temp_dir}/bin/rclone" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$@" > "${RCLONE_ARGS_FILE}"
exit "${RCLONE_TEST_EXIT_CODE:-0}"
EOF
chmod 0700 "${temp_dir}/bin/rclone"

run_sync() {
  PATH="${temp_dir}/bin:${PATH}" \
  BACKUP_ENV_FILE=/dev/null \
  BACKUP_ROOT="${temp_dir}/backups" \
  RCLONE_REMOTE=test_remote \
  RCLONE_PATH=paper-mes-backups \
  RCLONE_ARGS_FILE="${temp_dir}/rclone.args" \
  RCLONE_TEST_EXIT_CODE="$1" \
  "${sync_script}"
}

assert_status() {
  local expected="$1"
  grep -Fx "status=${expected}" "${temp_dir}/backups/.remote-sync-status" >/dev/null
  grep -Fx "remote_name=test_remote" "${temp_dir}/backups/.remote-sync-status" >/dev/null
}

run_sync 0 >/dev/null
assert_status SUCCESS
grep -Fx copy "${temp_dir}/rclone.args" >/dev/null
grep -Fx 'test_remote:paper-mes-backups' "${temp_dir}/rclone.args" >/dev/null
grep -Fx -- '--checksum' "${temp_dir}/rclone.args" >/dev/null
grep -Fx -- '--include' "${temp_dir}/rclone.args" >/dev/null

if run_sync 7 >/dev/null 2>&1; then
  echo "failed rclone execution unexpectedly succeeded" >&2
  exit 1
fi
assert_status FAILED

echo "off-site backup sync test passed"
