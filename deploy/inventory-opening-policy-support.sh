#!/usr/bin/env bash

# Approval and evidence-integrity policy for inventory opening cutover.
cutover_manifest_value() {
  local key="$1"
  local count
  count="$(awk -F= -v key="${key}" '$1 == key { count++ } END { print count + 0 }' "${CUTOVER_MANIFEST}")"
  [ "${count}" = "1" ] || fail "cutover manifest must contain exactly one ${key}"
  awk -F= -v key="${key}" '$1 == key { sub(/^[^=]*=/, ""); sub(/\r$/, ""); print }' "${CUTOVER_MANIFEST}"
}

require_control_free_text() {
  local label="$1"
  local value="$2"
  [ -n "${value}" ] && [ "${#value}" -le 200 ] || fail "invalid ${label}"
  if printf '%s' "${value}" | LC_ALL=C awk '/[[:cntrl:]]/ { found=1 } END { exit !found }'; then
    fail "${label} contains a control character"
  fi
}

require_approval_time() {
  [[ "$2" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(Z|[+-][0-9]{2}:[0-9]{2})$ ]] \
    || fail "$1 must be ISO-8601 with timezone"
  date -d "$2" >/dev/null 2>&1 || fail "$1 is not a valid timestamp"
}

validate_cutover_manifest() {
  manifest_format="$(cutover_manifest_value format)"
  manifest_switch_uuid="$(cutover_manifest_value switch_uuid)"
  manifest_occurred_at="$(cutover_manifest_value occurred_at)"
  manifest_timezone="$(cutover_manifest_value timezone)"
  manifest_reason="$(cutover_manifest_value reason)"
  manifest_approved_by="$(cutover_manifest_value approved_by)"
  manifest_approved_at="$(cutover_manifest_value approved_at)"
  freeze_confirmed_by="$(cutover_manifest_value write_freeze_confirmed_by)"
  freeze_confirmed_at="$(cutover_manifest_value write_freeze_confirmed_at)"
  [ "${manifest_format}" = "paper-mes-inventory-opening-v1" ] || fail "unsupported cutover manifest format"
  [ "${manifest_switch_uuid}" = "${SWITCH_UUID}" ] || fail "cutover manifest switch_uuid mismatch"
  [ "${manifest_occurred_at}" = "${OCCURRED_AT}" ] || fail "cutover manifest occurred_at mismatch"
  [ "${manifest_timezone}" = "Asia/Shanghai" ] || fail "cutover timezone must be Asia/Shanghai"
  [ "${manifest_reason}" = "切换日开账" ] || fail "cutover reason must match the controlled opening command"
  require_control_free_text approved_by "${manifest_approved_by}"
  require_control_free_text write_freeze_confirmed_by "${freeze_confirmed_by}"
  require_approval_time approved_at "${manifest_approved_at}"
  require_approval_time write_freeze_confirmed_at "${freeze_confirmed_at}"
}

validate_cutover_inputs() {
  [[ "${DB_NAME}" =~ ^[A-Za-z0-9_]+$ ]] || fail "unsafe database identifier"
  [[ "${DB_USER}" =~ ^[A-Za-z0-9_]+$ ]] || fail "unsafe database user"
  [[ "${DB_HOST}" =~ ^[A-Za-z0-9._:-]+$ ]] || fail "invalid DB_HOST"
  [[ "${DB_PORT}" =~ ^[0-9]+$ ]] && (( DB_PORT >= 1 && DB_PORT <= 65535 )) || fail "invalid DB_PORT"
  [[ "${DB_PASSWORD}" != *$'\n'* && "${DB_PASSWORD}" != *$'\r'* ]] || fail "DB_PASSWORD contains a line break"
  [[ "${SWITCH_UUID}" =~ ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$ ]] \
    || fail "SWITCH_UUID must be a canonical UUID"
  [[ "${OCCURRED_AT}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}$ ]] \
    || fail "OCCURRED_AT must be a local ISO-8601 timestamp without timezone"
  [ "$(date -d "${OCCURRED_AT}" '+%Y-%m-%dT%H:%M:%S' 2>/dev/null)" = "${OCCURRED_AT}" ] \
    || fail "OCCURRED_AT is not a valid timestamp"
  [[ "${CUTOVER_MANIFEST_SHA256}" =~ ^[0-9a-f]{64}$ ]] || fail "invalid CUTOVER_MANIFEST_SHA256"
  [[ "${PREVIEW_RESPONSE_SHA256}" =~ ^[0-9a-f]{64}$ ]] || fail "invalid PREVIEW_RESPONSE_SHA256"
  [[ "${MODE}" == "preflight" || "${MODE}" == "postcheck" ]] || fail "MODE must be preflight or postcheck"
  [ -f "${CUTOVER_MANIFEST}" ] || fail "CUTOVER_MANIFEST does not exist"
  [ "${MODE}" != "preflight" ] || [ -f "${PREVIEW_RESPONSE_FILE}" ] \
    || fail "PREVIEW_RESPONSE_FILE does not exist"
}

snapshot_cutover_manifest() {
  cp -- "${CUTOVER_MANIFEST}" "${manifest_snapshot}"
  CUTOVER_MANIFEST="${manifest_snapshot}"
  local actual_checksum
  actual_checksum="$(sha256sum "${CUTOVER_MANIFEST}" | awk '{print $1}')"
  [ "${actual_checksum}" = "${CUTOVER_MANIFEST_SHA256}" ] || fail "cutover manifest checksum mismatch"
  validate_cutover_manifest
}

write_approved_manifest() {
  [ ! -e "${APPROVED_MANIFEST_FILE}" ] || fail "refusing to overwrite approved cutover manifest"
  cp -- "${CUTOVER_MANIFEST}" "${APPROVED_MANIFEST_FILE}"
}

verify_approved_manifest() {
  [ -f "${APPROVED_MANIFEST_FILE}" ] || fail "approved cutover manifest is missing"
  [ "$(sha256sum "${APPROVED_MANIFEST_FILE}" | awk '{print $1}')" = "${CUTOVER_MANIFEST_SHA256}" ] \
    || fail "approved cutover manifest checksum mismatch"
  cmp -- "${CUTOVER_MANIFEST}" "${APPROVED_MANIFEST_FILE}" >/dev/null \
    || fail "cutover manifest differs from preflight approval"
}

write_approved_preview() {
  [ ! -e "${APPROVED_PREVIEW_FILE}" ] || fail "refusing to overwrite opening preview response"
  cp -- "${PREVIEW_RESPONSE_FILE}" "${APPROVED_PREVIEW_FILE}"
  [ "$(sha256sum "${APPROVED_PREVIEW_FILE}" | awk '{print $1}')" = "${PREVIEW_RESPONSE_SHA256}" ] \
    || fail "opening preview response checksum mismatch"
}

verify_approved_preview() {
  [ -f "${APPROVED_PREVIEW_FILE}" ] || fail "approved opening preview response is missing"
  [ "$(sha256sum "${APPROVED_PREVIEW_FILE}" | awk '{print $1}')" = "${PREVIEW_RESPONSE_SHA256}" ] \
    || fail "approved opening preview response checksum mismatch"
}

summary_value() {
  local key="$1"
  awk -F= -v key="${key}" '$1 == key { sub(/^[^=]*=/, ""); print }' \
    "${OUTPUT_DIR}/preflight-summary.txt"
}

verify_preflight_evidence() {
  [ -f "${EVIDENCE_MANIFEST}" ] || fail "preflight SHA256SUMS is missing"
  (cd "${OUTPUT_DIR}" && sha256sum -c "$(basename "${EVIDENCE_MANIFEST}")" >/dev/null) \
    || fail "preflight evidence checksum mismatch"
  [ "$(summary_value switch_uuid)" = "${SWITCH_UUID}" ] || fail "preflight switch_uuid mismatch"
  [ "$(summary_value occurred_at)" = "${OCCURRED_AT}" ] || fail "preflight occurred_at mismatch"
}
