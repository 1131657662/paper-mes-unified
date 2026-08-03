#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
support="${script_dir}/historical-fixture-support.sh"
root_dir="$(mktemp -d)"

cleanup() { rm -rf -- "${root_dir}"; }
trap cleanup EXIT
fail() { echo "historical fixture support test failed: $1" >&2; exit 1; }
. "${support}"

FIXTURE_MANIFEST="${root_dir}/fixture.manifest"
cat > "${FIXTURE_MANIFEST}" <<'EOF'
format=paper-mes-historical-fixture-v1
fixture_version=1.3
fixture_sha256=0000000000000000000000000000000000000000000000000000000000000000
sanitization=approved
approved_by=business-owner
approved_at=2026-08-03T20:00:00+08:00
EOF
validate_manifest
[ "${fixture_version}" = "1.3" ] || fail "valid manifest version was not loaded"

MIGRATION_DIR="${root_dir}/source-migrations"
mkdir "${MIGRATION_DIR}"
printf 'SELECT 12;\n' > "${MIGRATION_DIR}/V1.2__first.sql"
printf 'SELECT 13;\n' > "${MIGRATION_DIR}/V1.3__second.sql"
printf 'SELECT 20;\n' > "${MIGRATION_DIR}/V2.0__third.sql"
temp_dir="${root_dir}/known-boundary"
mkdir "${temp_dir}"
prepare_pending_migrations
[ "${pending_count}" = "1" ] || fail "known boundary selected the wrong migration count"
[ -f "${temp_dir}/migrations/V2.0__third.sql" ] || fail "newer migration was not selected"

if (
  fixture_version=1.4
  temp_dir="${root_dir}/unknown-boundary"
  mkdir "${temp_dir}"
  prepare_pending_migrations
) >"${root_dir}/boundary.out" 2>&1; then
  fail "unknown migration boundary was unexpectedly accepted"
fi
grep -q 'fixture_version is not a known migration boundary' "${root_dir}/boundary.out"

FIXTURE_DUMP="${root_dir}/safe.sql"
printf 'CREATE TABLE safe_fixture (id INT);\n' > "${FIXTURE_DUMP}"
validate_dump_scope

printf 'USE another_database;\n' > "${root_dir}/unsafe.sql"
if (
  FIXTURE_DUMP="${root_dir}/unsafe.sql"
  validate_dump_scope
) >"${root_dir}/scope.out" 2>&1; then
  fail "out-of-scope dump was unexpectedly accepted"
fi
grep -q 'dump contains an out-of-scope statement' "${root_dir}/scope.out"

echo "historical fixture support behavior test passed"
