#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
sync_script="${script_dir}/sync-backups-rclone.example.sh"
temp_dir="$(mktemp -d)"
status_group="$(id -gn)"
real_chgrp="$(command -v chgrp)"
chgrp_groups="${temp_dir}/chgrp.groups"

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
cat > "${temp_dir}/bin/chgrp" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "${2:-}" >> "${CHGRP_GROUPS_FILE}"
[ "${CHGRP_TEST_FAIL:-false}" != true ] || exit 1
if [ -n "${CHGRP_TEST_MAP_GROUP:-}" ] && [ "${2:-}" = root ]; then
  set -- "${1}" "${CHGRP_TEST_MAP_GROUP}" "${3}"
fi
exec "${REAL_CHGRP_COMMAND}" "$@"
EOF
chmod 0700 "${temp_dir}/bin/chgrp"

run_sync() {
  local exit_code="$1"
  local group="${2-}"
  local mode="${3-}"
  local chgrp_fail="${CHGRP_TEST_FAIL:-false}"
  local map_group="${CHGRP_TEST_MAP_GROUP:-}"
  PATH="${temp_dir}/bin:${PATH}" \
  BACKUP_ENV_FILE=/dev/null \
  BACKUP_ROOT="${temp_dir}/backups" \
  RCLONE_REMOTE=test_remote \
  RCLONE_PATH=paper-mes-backups \
  STATUS_FILE_GROUP="${group}" \
  STATUS_FILE_MODE="${mode}" \
  RCLONE_ARGS_FILE="${temp_dir}/rclone.args" \
  RCLONE_TEST_EXIT_CODE="${exit_code}" \
  REAL_CHGRP_COMMAND="${real_chgrp}" \
  CHGRP_GROUPS_FILE="${chgrp_groups}" \
  CHGRP_TEST_FAIL="${chgrp_fail}" \
  CHGRP_TEST_MAP_GROUP="${map_group}" \
  bash "${sync_script}"
}

assert_status() {
  local expected="$1"
  local expected_mode="${2:-640}"
  local status_file="${temp_dir}/backups/.remote-sync-status"
  grep -Fx "status=${expected}" "${temp_dir}/backups/.remote-sync-status" >/dev/null
  grep -Fx "remote_name=test_remote" "${temp_dir}/backups/.remote-sync-status" >/dev/null
  [ "$(stat -c %a "${status_file}")" = "${expected_mode}" ]
  [ "$(stat -c %G "${status_file}")" = "${status_group}" ]
}

: > "${chgrp_groups}"
CHGRP_TEST_MAP_GROUP="${status_group}" run_sync 0 >/dev/null
assert_status SUCCESS 600
grep -Fx root "${chgrp_groups}" >/dev/null
grep -Fx copy "${temp_dir}/rclone.args" >/dev/null
grep -Fx 'test_remote:paper-mes-backups' "${temp_dir}/rclone.args" >/dev/null
grep -Fx -- '--checksum' "${temp_dir}/rclone.args" >/dev/null
grep -Fx -- '--include' "${temp_dir}/rclone.args" >/dev/null

run_sync 0 "${status_group}" 0640 >/dev/null
assert_status SUCCESS 640

if run_sync 7 "${status_group}" 0640 >/dev/null 2>&1; then
  echo "failed rclone execution unexpectedly succeeded" >&2
  exit 1
fi
assert_status FAILED 640

status_before="$(cat "${temp_dir}/backups/.remote-sync-status")"
if run_sync 0 'invalid/group' 0640 >/dev/null 2>&1; then
  echo "invalid status group unexpectedly succeeded" >&2
  exit 1
fi
[ "$(cat "${temp_dir}/backups/.remote-sync-status")" = "${status_before}" ]

if run_sync 0 "${status_group}" 0660 >/dev/null 2>&1; then
  echo "unsafe status mode unexpectedly succeeded" >&2
  exit 1
fi
[ "$(cat "${temp_dir}/backups/.remote-sync-status")" = "${status_before}" ]

set +e
CHGRP_TEST_FAIL=true run_sync 0 "${status_group}" 0640 >/dev/null 2>&1
status_write_code=$?
set -e
[ "${status_write_code}" -eq 74 ]
[ "$(cat "${temp_dir}/backups/.remote-sync-status")" = "${status_before}" ]
if find "${temp_dir}/backups" -maxdepth 1 -name '.remote-sync-status.tmp.*' -print -quit | grep -q .; then
  echo "failed status update left a temporary file" >&2
  exit 1
fi

echo "off-site backup sync test passed"
