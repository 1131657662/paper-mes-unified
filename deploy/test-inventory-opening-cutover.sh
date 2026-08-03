#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cutover_script="${script_dir}/inventory-opening-cutover.example.sh"
temp_dir="$(mktemp -d)"
switch_uuid="12345678-1234-4234-8234-1234567890ab"
occurred_at="2026-08-03T22:00:00"

cleanup() { rm -rf -- "${temp_dir}"; }
trap cleanup EXIT
mkdir -p "${temp_dir}/bin"

cat > "${temp_dir}/bin/mysql" <<'EOF'
#!/usr/bin/env bash
set -eu
query="${!#}"
case "${query}" in
  *"FROM biz_finish_roll f"*)
    if [ "${FULLY_RESERVED:-0}" = "1" ]; then
      printf 'finish-1\tF-001\t0.000\t2.000\t2.000\t0\tOK\n'
    else
      printf 'finish-1\tF-001\t10.000\t2.000\t12.000\t0\tOK\n'
    fi
    ;;
  *"FROM biz_delivery_detail d"*)
    ;;
  *"SELECT finish_roll_uuid"*)
    if [ "${FULLY_RESERVED:-0}" = "1" ]; then
      printf 'finish-1\t1\t0.000\t2.000\n'
    else
      printf 'finish-1\t1\t10.000\t2.000\n'
    fi
    ;;
  *"occurred_at <> STR_TO_DATE"*)
    printf '0\n'
    ;;
  *"NOT (reason <=>"*)
    printf '%s\n' "${REASON_MISMATCH:-0}"
    ;;
  *"WHERE NOT (event_type="*)
    printf '%s\n' "${NON_OPENING_ACTIVITY:-0}"
    ;;
  *)
    echo "unexpected query" >&2
    exit 1
    ;;
esac
EOF
chmod 0700 "${temp_dir}/bin/mysql"

manifest="${temp_dir}/cutover.manifest"
cat > "${manifest}" <<EOF
format=paper-mes-inventory-opening-v1
switch_uuid=${switch_uuid}
occurred_at=${occurred_at}
timezone=Asia/Shanghai
reason=切换日开账
approved_by=business-owner
approved_at=2026-08-03T20:00:00+08:00
write_freeze_confirmed_by=warehouse-owner
write_freeze_confirmed_at=2026-08-03T21:55:00+08:00
EOF
manifest_checksum="$(sha256sum "${manifest}" | awk '{print $1}')"

preview="${temp_dir}/opening-preview.json"
cat > "${preview}" <<EOF
{"code":200,"message":"success","data":{"switchUuid":"${switch_uuid}","preview":true,"matched":true,"projectedQuantityTotal":1,"openingQuantityTotal":1,"projectedWeightTotal":10,"openingWeightTotal":10,"lines":[{"finishRollUuid":"finish-1","projectedQuantity":1,"openingQuantity":1,"projectedWeight":10,"openingWeight":10,"quantityDifference":0,"weightDifference":0}]}}
EOF

reserved_preview="${temp_dir}/opening-preview-fully-reserved.json"
cat > "${reserved_preview}" <<EOF
{"code":200,"message":"success","data":{"switchUuid":"${switch_uuid}","preview":true,"matched":true,"projectedQuantityTotal":1,"openingQuantityTotal":1,"projectedWeightTotal":0,"openingWeightTotal":0,"lines":[{"finishRollUuid":"finish-1","projectedQuantity":1,"openingQuantity":1,"projectedWeight":0,"openingWeight":0,"quantityDifference":0,"weightDifference":0}]}}
EOF

run_cutover() {
  local mode="$1"
  local output="$2"
  local preview_file="$3"
  local preview_checksum
  preview_checksum="$(sha256sum "${preview_file}" | awk '{print $1}')"
  MODE="${mode}" DB_PASSWORD=test-only DB_NAME=paper_processing DB_USER=test_reader \
  SWITCH_UUID="${switch_uuid}" OCCURRED_AT="${occurred_at}" OUTPUT_DIR="${output}" \
  CUTOVER_MANIFEST="${manifest}" CUTOVER_MANIFEST_SHA256="${manifest_checksum}" \
  PREVIEW_RESPONSE_FILE="${preview_file}" PREVIEW_RESPONSE_SHA256="${preview_checksum}" \
  MYSQL_BIN="${temp_dir}/bin/mysql" bash "${cutover_script}"
}

success_dir="${temp_dir}/success"
run_cutover preflight "${success_dir}" "${preview}" >/dev/null
run_cutover postcheck "${success_dir}" "${preview}" >/dev/null
grep -q '^projection_match=true$' "${success_dir}/postcheck-summary.txt"
grep -q '^non_opening_activity_count=0$' "${success_dir}/postcheck-summary.txt"
(cd "${success_dir}" && sha256sum -c SHA256SUMS >/dev/null)

reserved_dir="${temp_dir}/fully-reserved"
FULLY_RESERVED=1 run_cutover preflight "${reserved_dir}" "${reserved_preview}" >/dev/null
FULLY_RESERVED=1 run_cutover postcheck "${reserved_dir}" "${reserved_preview}" >/dev/null
grep -q '^physical_opening_weight=2.000$' "${reserved_dir}/preflight-summary.txt"

tamper_dir="${temp_dir}/tamper"
run_cutover preflight "${tamper_dir}" "${preview}" >/dev/null
printf 'tampered\n' >> "${tamper_dir}/preflight.tsv"
if run_cutover postcheck "${tamper_dir}" "${preview}" >"${temp_dir}/tamper.out" 2>&1; then
  echo "tampered preflight evidence was unexpectedly accepted" >&2
  exit 1
fi
grep -q 'preflight evidence checksum mismatch' "${temp_dir}/tamper.out"

preview_tamper_dir="${temp_dir}/preview-tamper"
run_cutover preflight "${preview_tamper_dir}" "${preview}" >/dev/null
printf '\n' >> "${preview_tamper_dir}/opening-preview-response.json"
if run_cutover postcheck "${preview_tamper_dir}" "${preview}" >"${temp_dir}/preview-tamper.out" 2>&1; then
  echo "tampered opening preview was unexpectedly accepted" >&2
  exit 1
fi
grep -q 'approved opening preview response checksum mismatch' "${temp_dir}/preview-tamper.out"

reason_dir="${temp_dir}/reason"
run_cutover preflight "${reason_dir}" "${preview}" >/dev/null
if REASON_MISMATCH=1 run_cutover postcheck "${reason_dir}" "${preview}" >"${temp_dir}/reason.out" 2>&1; then
  echo "opening reason mismatch was unexpectedly accepted" >&2
  exit 1
fi
grep -q 'opening reason differs from approved cutover reason' "${temp_dir}/reason.out"

activity_dir="${temp_dir}/activity"
run_cutover preflight "${activity_dir}" "${preview}" >/dev/null
if NON_OPENING_ACTIVITY=1 run_cutover postcheck "${activity_dir}" "${preview}" >"${temp_dir}/activity.out" 2>&1; then
  echo "non-opening cutover activity was unexpectedly accepted" >&2
  exit 1
fi
grep -q 'non-opening ledger activity detected during cutover' "${temp_dir}/activity.out"

echo "inventory opening cutover behavior test passed"
