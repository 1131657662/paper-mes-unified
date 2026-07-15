#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
preflight_script="${script_dir}/preflight-paper-mes-release.example.sh"
temp_dir="$(mktemp -d)"

cleanup() { rm -rf "${temp_dir}"; }
trap cleanup EXIT

mkdir -p "${temp_dir}/bin" "${temp_dir}/backups/$(date +%Y%m%d-%H%M%S)"
backup_dir="$(find "${temp_dir}/backups" -mindepth 1 -maxdepth 1 -type d)"
printf 'backup-data' > "${backup_dir}/paper.sql.gz"
(cd "${backup_dir}" && sha256sum paper.sql.gz > SHA256SUMS)

cat > "${temp_dir}/bin/mysql" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "${MYSQL_TEST_RESULT:-0}"
EOF
cat > "${temp_dir}/bin/curl" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' '{"status":"UP"}'
EOF
chmod 0700 "${temp_dir}/bin/mysql" "${temp_dir}/bin/curl"

run_preflight() {
  PATH="${temp_dir}/bin:${PATH}" \
  BACKUP_ENV_FILE=/dev/null \
  MIGRATION_ENV_FILE=/dev/null \
  DB_PASSWORD=test-only \
  BACKUP_ROOT="${temp_dir}/backups" \
  MAX_BACKUP_AGE_HOURS=48 \
  MYSQL_TEST_RESULT="$1" \
  "${preflight_script}"
}

run_preflight 0 >/dev/null
if run_preflight 1 >/dev/null 2>&1; then
  echo "preflight unexpectedly accepted a database conflict" >&2
  exit 1
fi

echo "release preflight behavior test passed"
