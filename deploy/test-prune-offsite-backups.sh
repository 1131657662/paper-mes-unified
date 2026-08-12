#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
prune_script="${script_dir}/prune-offsite-backups.example.sh"
temp_dir="$(mktemp -d)"
trap 'rm -rf "${temp_dir}"' EXIT
mkdir -p "${temp_dir}/bin" "${temp_dir}/state"

cat > "${temp_dir}/bin/rclone" <<'EOF'
#!/usr/bin/env bash
case "$1" in
  lsf)
    printf '%s\n' \
      '20260812-023515/' \
      '20230812-023515/' \
      '20230811-023515/' \
      '20230810-023515/' \
      '20220109-023515/' \
      '20220103-023515/' \
      '20210731-023515/' \
      '20210701-023515/' \
      '20201231-023515/' \
      '20201201-023515/' \
      'unexpected/'
    ;;
  purge) printf '%s\n' "$2" >> "${RCLONE_PURGE_FILE}" ;;
  *) exit 2 ;;
esac
EOF
chmod 0700 "${temp_dir}/bin/rclone"

run_prune() {
  PATH="${temp_dir}/bin:${PATH}" \
  RCLONE_CONFIG=/dev/null \
  RCLONE_REMOTE=test_remote \
  RCLONE_PATHS='paper-mes business-projects' \
  RETENTION_NOW='2026-08-12 12:00:00 UTC' \
  STATE_DIR="${temp_dir}/state" \
  PLAN_FILE="${temp_dir}/plan.tsv" \
  LOCK_FILE="${temp_dir}/retention.lock" \
  RCLONE_PURGE_FILE="${temp_dir}/purged" \
  OFFSITE_RETENTION_APPLY="$1" \
  MAX_DELETE_COUNT="${2:-50}" \
    "${prune_script}"
}

run_prune false >/dev/null
[ ! -e "${temp_dir}/purged" ]
grep -F $'KEEP\tpaper-mes\t20230812-023515\tdaily' "${temp_dir}/plan.tsv"
grep -F $'KEEP\tpaper-mes\t20230811-023515\tweekly' "${temp_dir}/plan.tsv"
grep -F $'DELETE\tpaper-mes\t20230810-023515\tweekly' "${temp_dir}/plan.tsv"
grep -F $'KEEP\tpaper-mes\t20210731-023515\tmonthly-permanent' "${temp_dir}/plan.tsv"
grep -F $'DELETE\tpaper-mes\t20210701-023515\tmonthly' "${temp_dir}/plan.tsv"
grep -F $'SKIP\tpaper-mes\tunexpected\tunexpected-name' "${temp_dir}/plan.tsv"

run_prune true >/dev/null
[ "$(wc -l < "${temp_dir}/purged")" -eq 8 ]
grep -Fx 'test_remote:paper-mes/20230810-023515' "${temp_dir}/purged"
grep -Fx 'test_remote:business-projects/20201201-023515' "${temp_dir}/purged"

purge_count_before_limit="$(wc -l < "${temp_dir}/purged")"
if run_prune true 1 >/dev/null 2>&1; then
  echo "retention safety limit unexpectedly allowed deletion" >&2
  exit 1
fi
[ "$(wc -l < "${temp_dir}/purged")" -eq "$purge_count_before_limit" ]

echo "off-site retention test passed"
