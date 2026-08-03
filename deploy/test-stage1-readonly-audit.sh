#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
audit_script="${script_dir}/stage1-readonly-audit.example.sh"
temp_dir="$(mktemp -d)"
cleanup() { rm -rf "${temp_dir}"; }
trap cleanup EXIT

mkdir -p "${temp_dir}/bin"
cat > "${temp_dir}/bin/mysql" <<'EOF'
#!/usr/bin/env bash
query="$*"
if [ -n "${STAGE1_FAIL_CHECK:-}" ] && [[ "${query}" == *"audit: ${STAGE1_FAIL_CHECK}"* ]]; then
  printf '1\n'
else
  printf '0\n'
fi
EOF
chmod 0700 "${temp_dir}/bin/mysql"

run_audit() {
  local output_dir="$1"
  PATH="${temp_dir}/bin:${PATH}" \
  DB_PASSWORD=test-only \
  DB_NAME=paper_mes_stage1_test \
  OUTPUT_DIR="${output_dir}" \
  MYSQL_BIN=mysql \
  bash "${audit_script}"
}

healthy_dir="${temp_dir}/healthy"
run_audit "${healthy_dir}" > "${temp_dir}/healthy.out"
grep -q 'stage 1 read-only audit passed' "${temp_dir}/healthy.out"
grep -q $'^orphan_stage_output_step\t0$' "${healthy_dir}/stage1-readonly-audit.tsv"
grep -q $'^soft_delete_no_rule_risk\t0$' "${healthy_dir}/stage1-readonly-audit.tsv"

if STAGE1_FAIL_CHECK=orphan_stage_input_output run_audit "${temp_dir}/conflict" \
    > "${temp_dir}/conflict.out" 2>&1; then
  echo "read-only audit unexpectedly accepted an orphan stage input" >&2
  exit 1
fi
grep -q 'orphan_stage_input_output' "${temp_dir}/conflict.out"
grep -q $'^orphan_stage_input_output\t1$' "${temp_dir}/conflict/stage1-readonly-audit.tsv"
grep -q $'^soft_delete_no_rule_risk\t0$' "${temp_dir}/conflict/stage1-readonly-audit.tsv"

if grep -Eiq '\b(INSERT|UPDATE|DELETE|DROP|ALTER|TRUNCATE|CREATE)\b' "${audit_script}"; then
  echo "read-only audit contains a mutation statement" >&2
  exit 1
fi

echo "stage 1 read-only audit behavior test passed"
