#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
guard_script="${script_dir}/verify-paper-mes-migration-state.example.sh"
temp_dir="$(mktemp -d)"

cleanup() { rm -rf "${temp_dir}"; }
trap cleanup EXIT

mkdir -p "${temp_dir}/bin" "${temp_dir}/migrations"
printf 'CREATE TABLE migration_guard_test (id INT PRIMARY KEY);\n' \
  > "${temp_dir}/migrations/V1.0__migration_guard_test.sql"
checksum="$(sha256sum "${temp_dir}/migrations/V1.0__migration_guard_test.sql" | awk '{print $1}')"

cat > "${temp_dir}/bin/mysql" <<'EOF'
#!/usr/bin/env bash
case "$*" in
  *information_schema.tables*) printf '1\n' ;;
  *"status IS NULL OR status <> 'applied'"*) printf '%s\n' "${MIGRATION_STATUS_COUNT:-0}" ;;
  *"SELECT status, checksum"*)
    if [ "${MIGRATION_ROW_PRESENT:-1}" = "1" ]; then
      printf 'applied\t%s\n' "${MIGRATION_TEST_CHECKSUM}"
    fi
    ;;
  *) exit 1 ;;
esac
EOF
chmod 0700 "${temp_dir}/bin/mysql"

run_guard() {
  PATH="${temp_dir}/bin:${PATH}" \
  DB_PASSWORD=test-only \
  DB_NAME=paper_mes_guard_test \
  MIGRATION_DIR="${temp_dir}/migrations" \
  MIGRATION_TEST_CHECKSUM="${MIGRATION_TEST_CHECKSUM:-${checksum}}" \
  bash "${guard_script}"
}

run_guard >/dev/null
if MIGRATION_STATUS_COUNT=1 run_guard >/dev/null 2>&1; then
  echo "migration state guard unexpectedly accepted a non-applied migration" >&2
  exit 1
fi
if MIGRATION_TEST_CHECKSUM=bad-checksum run_guard >/dev/null 2>&1; then
  echo "migration state guard unexpectedly accepted a checksum mismatch" >&2
  exit 1
fi
if MIGRATION_ROW_PRESENT=0 run_guard >/dev/null 2>&1; then
  echo "migration state guard unexpectedly accepted a missing migration record" >&2
  exit 1
fi

echo "migration state guard behavior test passed"
