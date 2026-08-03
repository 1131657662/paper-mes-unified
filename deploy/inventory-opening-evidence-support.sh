#!/usr/bin/env bash

# Read-only evidence collection for inventory opening cutover.
projection_sql() {
  cat <<'SQL'
SELECT f.uuid, f.finish_roll_no,
       CASE WHEN f.finish_status = 2 THEN COALESCE(f.remaining_weight, f.actual_weight) ELSE 0 END,
       COALESCE(r.reserved_weight, 0),
       CASE WHEN f.finish_status = 2 THEN COALESCE(f.remaining_weight, f.actual_weight) ELSE 0 END
         + COALESCE(r.reserved_weight, 0),
       COALESCE(l.ledger_event_count, 0),
       CASE
         WHEN f.finish_status = 2 AND COALESCE(f.remaining_weight, f.actual_weight) IS NULL
           THEN 'INVALID_MISSING_WEIGHT'
         WHEN f.finish_status = 2 AND COALESCE(f.remaining_weight, f.actual_weight) < 0
           THEN 'INVALID_NEGATIVE_WEIGHT'
         WHEN COALESCE(r.reserved_weight, 0) < 0 THEN 'INVALID_NEGATIVE_RESERVATION'
         WHEN f.finish_status <> 2 AND COALESCE(r.reserved_weight, 0) > 0
           THEN 'INVALID_RESERVATION_WITHOUT_STOCK'
         ELSE 'OK' END
FROM biz_finish_roll f
LEFT JOIN (
  SELECT finish_uuid, SUM(out_weight) AS reserved_weight FROM biz_delivery_detail
  WHERE is_deleted = 0 AND stock_lock_status = 1 AND finish_uuid IS NOT NULL GROUP BY finish_uuid
) r ON r.finish_uuid = f.uuid
LEFT JOIN (
  SELECT finish_roll_uuid, COUNT(*) AS ledger_event_count FROM biz_inventory_transaction
  GROUP BY finish_roll_uuid
) l ON l.finish_roll_uuid = f.uuid
WHERE f.is_deleted = 0
ORDER BY f.uuid;
SQL
}

write_manifest() {
  (cd "${OUTPUT_DIR}" && sha256sum ./cutover-manifest.approved ./*.json ./*.tsv ./*.txt 2>/dev/null | sort) \
    > "${EVIDENCE_MANIFEST}"
}

capture_preflight_rows() {
  local rows="$1"
  local orphans="$2"
  {
    printf 'finish_roll_uuid\tfinish_roll_no\tprojected_available_weight\treserved_weight\tphysical_opening_weight\tledger_event_count\tprojection_state\n'
    mysql_query "$(projection_sql)"
  } > "${rows}"
  {
    printf 'detail_uuid\tfinish_uuid\tout_weight\n'
    mysql_query "SELECT d.uuid, d.finish_uuid, d.out_weight
      FROM biz_delivery_detail d
      LEFT JOIN biz_finish_roll f ON f.uuid = d.finish_uuid AND f.is_deleted = 0
      WHERE d.is_deleted = 0 AND d.stock_lock_status = 1
        AND (d.finish_uuid IS NULL OR f.uuid IS NULL) ORDER BY d.uuid;"
  } > "${orphans}"
}

write_preflight_summary() {
  local rows="$1" orphans="$2" summary="$3"
  invalid="$(tail -n +2 "${rows}" | awk -F '\t' '$7 != "OK" {n++} END {print n+0}')"
  ledger="$(tail -n +2 "${rows}" | awk -F '\t' '$6 > 0 {n++} END {print n+0}')"
  orphan="$(tail -n +2 "${orphans}" | awk 'NF {n++} END {print n+0}')"
  rows_count="$(tail -n +2 "${rows}" | awk 'NF {n++} END {print n+0}')"
  projected_weight="$(tail -n +2 "${rows}" | awk -F '\t' '{s+=$3} END {printf "%.3f", s+0}')"
  reserved_weight="$(tail -n +2 "${rows}" | awk -F '\t' '{s+=$4} END {printf "%.3f", s+0}')"
  physical_weight="$(tail -n +2 "${rows}" | awk -F '\t' '{s+=$5} END {printf "%.3f", s+0}')"
  {
    printf 'switch_uuid=%s\n' "${SWITCH_UUID}"
    printf 'occurred_at=%s\n' "${OCCURRED_AT}"
    printf 'captured_at_utc=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    printf 'finish_roll_count=%s\n' "${rows_count}"
    printf 'projected_available_weight=%s\n' "${projected_weight}"
    printf 'reserved_weight=%s\n' "${reserved_weight}"
    printf 'physical_opening_weight=%s\n' "${physical_weight}"
    printf 'invalid_projection_count=%s\n' "${invalid}"
    printf 'existing_ledger_roll_count=%s\n' "${ledger}"
    printf 'orphan_reservation_count=%s\n' "${orphan}"
  } > "${summary}"
}

validate_preflight_counts() {
  (( invalid == 0 )) || fail "invalid finish-roll projection; review preflight.tsv"
  (( ledger == 0 )) || fail "ledger rows already exist; opening must be first event"
  (( orphan == 0 )) || fail "active reservations reference missing finish rolls"
}

run_preflight() {
  local rows="${OUTPUT_DIR}/preflight.tsv"
  local orphans="${OUTPUT_DIR}/orphan-reservations.tsv"
  local summary="${OUTPUT_DIR}/preflight-summary.txt"
  [[ ! -e "${rows}" && ! -e "${orphans}" && ! -e "${summary}" && ! -e "${EVIDENCE_MANIFEST}" ]] \
    || fail "refusing to overwrite existing preflight evidence"
  write_approved_manifest
  write_approved_preview
  capture_preflight_rows "${rows}" "${orphans}"
  node "${PREVIEW_VALIDATOR}" "${APPROVED_PREVIEW_FILE}" "${rows}" "${SWITCH_UUID}" >/dev/null
  write_preflight_summary "${rows}" "${orphans}" "${summary}"
  write_manifest
  validate_preflight_counts
  echo "preflight evidence written to ${OUTPUT_DIR}"
}

capture_opening_rows() {
  local rows="$1"
  {
    printf 'finish_roll_uuid\topening_quantity_after\topening_weight_after\topening_reserved_weight_after\n'
    mysql_query "SELECT finish_roll_uuid, available_quantity_after, available_weight_after,
        reserved_weight_after FROM biz_inventory_transaction
      WHERE event_type = 'OPENING_BALANCE' AND source_business_type = 'INVENTORY_SWITCH'
        AND source_business_uuid = '$(sql_escape "${SWITCH_UUID}")' ORDER BY finish_roll_uuid;"
  } > "${rows}"
}

compare_opening_projection() {
  local rows="$1"
  local pre_norm="${OUTPUT_DIR}/.pre.normalized" post_norm="${OUTPUT_DIR}/.post.normalized"
  tail -n +2 "${PREFLIGHT_FILE}" | awk -F '\t' '$7 == "OK" {printf "%s\t%d\t%.3f\t%.3f\n", $1, ($5 > 0 ? 1 : 0), $3, $4}' | sort > "${pre_norm}"
  tail -n +2 "${rows}" | awk -F '\t' '{printf "%s\t%d\t%.3f\t%.3f\n", $1, ($2 > 0 ? 1 : 0), $3, $4}' | sort > "${post_norm}"
  diff -u "${pre_norm}" "${post_norm}" > "${OUTPUT_DIR}/postcheck-diff.txt" \
    || fail "opening ledger does not match preflight projection"
  rm -f -- "${pre_norm}" "${post_norm}"
}

read_postcheck_counts() {
  local rows="$1"
  row_count="$(tail -n +2 "${rows}" | awk 'NF {n++} END {print n+0}')"
  duplicates="$(tail -n +2 "${rows}" | cut -f1 | sort | uniq -d | awk 'NF {n++} END {print n+0}')"
  time_mismatches="$(mysql_query "SELECT COUNT(*) FROM biz_inventory_transaction
    WHERE event_type='OPENING_BALANCE' AND source_business_type='INVENTORY_SWITCH'
      AND source_business_uuid='$(sql_escape "${SWITCH_UUID}")'
      AND occurred_at <> STR_TO_DATE('$(sql_escape "${OCCURRED_AT}")', '%Y-%m-%dT%H:%i:%s');")"
  reason_mismatches="$(mysql_query "SELECT COUNT(*) FROM biz_inventory_transaction
    WHERE event_type='OPENING_BALANCE' AND source_business_type='INVENTORY_SWITCH'
      AND source_business_uuid='$(sql_escape "${SWITCH_UUID}")'
      AND NOT (reason <=> '$(sql_escape "${manifest_reason}")');")"
  non_opening_activity="$(mysql_query "SELECT COUNT(*) FROM biz_inventory_transaction
    WHERE NOT (event_type='OPENING_BALANCE' AND source_business_type='INVENTORY_SWITCH'
      AND source_business_uuid='$(sql_escape "${SWITCH_UUID}")');")"
}

validate_postcheck_counts() {
  (( duplicates == 0 )) || fail "duplicate opening rows detected"
  (( time_mismatches == 0 )) || fail "opening occurred_at differs from approved cutover time"
  (( reason_mismatches == 0 )) || fail "opening reason differs from approved cutover reason"
  (( non_opening_activity == 0 )) || fail "non-opening ledger activity detected during cutover"
}

write_postcheck_summary() {
  local summary="$1"
  {
    printf 'switch_uuid=%s\n' "${SWITCH_UUID}"
    printf 'occurred_at=%s\n' "${OCCURRED_AT}"
    printf 'checked_at_utc=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    printf 'opening_row_count=%s\n' "${row_count}"
    printf 'duplicate_finish_roll_count=%s\n' "${duplicates}"
    printf 'occurred_at_mismatch_count=%s\n' "${time_mismatches}"
    printf 'reason_mismatch_count=%s\n' "${reason_mismatches}"
    printf 'non_opening_activity_count=%s\n' "${non_opening_activity}"
    printf 'projection_match=true\n'
  } > "${summary}"
}

run_postcheck() {
  local rows="${OUTPUT_DIR}/postcheck.tsv"
  local summary="${OUTPUT_DIR}/postcheck-summary.txt"
  [ -f "${PREFLIGHT_FILE}" ] || fail "PREFLIGHT_FILE does not exist"
  verify_approved_manifest
  verify_approved_preview
  verify_preflight_evidence
  [[ ! -e "${rows}" && ! -e "${summary}" && ! -e "${OUTPUT_DIR}/postcheck-diff.txt" ]] \
    || fail "refusing to overwrite existing postcheck evidence"
  capture_opening_rows "${rows}"
  compare_opening_projection "${rows}"
  read_postcheck_counts "${rows}"
  validate_postcheck_counts
  write_postcheck_summary "${summary}"
  write_manifest
  echo "postcheck evidence written to ${OUTPUT_DIR}"
}
