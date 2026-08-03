#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

MIGRATION_ENV_FILE="${MIGRATION_ENV_FILE:-/etc/paper-mes/migration.env}"
if [ -r "${MIGRATION_ENV_FILE}" ]; then
  set -a
  # shellcheck disable=SC1090
  . "${MIGRATION_ENV_FILE}"
  set +a
fi

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-paper_processing}"
DB_USER="${DB_USER:-paper_mes_migrator}"
DB_PASSWORD="${DB_PASSWORD:?set DB_PASSWORD or MIGRATION_ENV_FILE before stage 1 audit}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"
OUTPUT_DIR="${OUTPUT_DIR:-./stage1-readonly-audit-$(date +%Y%m%d-%H%M%S)}"

mysql_cnf="$(mktemp)"
report_tmp="$(mktemp)"
conflict_checks=0
cleanup() { rm -f "${mysql_cnf}" "${report_tmp}"; }
trap cleanup EXIT

fail() {
  echo "stage 1 read-only audit failed: $1" >&2
  exit 1
}

require_identifier() {
  [[ "$2" =~ ^[A-Za-z0-9_]+$ ]] || fail "invalid $1"
}

mysql_query() {
  "${MYSQL_BIN}" --defaults-extra-file="${mysql_cnf}" --batch --skip-column-names \
    "${DB_NAME}" -e "$1"
}

record_count() {
  local name="$1"
  local query="$2"
  local value
  value="$(mysql_query "${query}")" || fail "${name} query failed"
  [[ "${value}" =~ ^[0-9]+$ ]] || fail "${name} returned a non-integer count"
  printf '%s\t%s\n' "${name}" "${value}" >> "${report_tmp}"
  printf '%s=%s\n' "${name}" "${value}"
}

require_zero() {
  local name="$1"
  local result
  result="$(record_count "${name}" "$2")"
  local value="${result##*=}"
  if [ "${value}" != "0" ]; then
    conflict_checks=$((conflict_checks + 1))
    echo "stage 1 audit conflict: ${name}=${value}" >&2
  fi
}

audit_baseline_counts() {
  record_count historical_workstation_rows "/* audit: historical_workstation_rows */ SELECT COUNT(*) FROM sys_machine WHERE resource_kind = 'WORKSTATION'"
  record_count nonempty_team_group_orders "/* audit: nonempty_team_group_orders */ SELECT COUNT(*) FROM biz_process_order WHERE team_group IS NOT NULL AND TRIM(team_group) <> ''"
  record_count applied_issue_versions "/* audit: applied_issue_versions */ SELECT COUNT(*) FROM biz_process_order_issue_version WHERE is_deleted = 0 AND status = 'APPLIED'"
  record_count snap_print_without_applied_issue_version "/* audit: snap_print_without_applied_issue_version */ SELECT COUNT(*) FROM biz_process_order o WHERE o.is_deleted = 0 AND o.snap_print IS NOT NULL AND NOT EXISTS (SELECT 1 FROM biz_process_order_issue_version v WHERE v.order_uuid = o.uuid AND v.is_deleted = 0 AND v.status = 'APPLIED')"
  record_count processing_print_count_zero "/* audit: processing_print_count_zero */ SELECT COUNT(*) FROM biz_process_order WHERE is_deleted = 0 AND order_status = 2 AND print_count = 0"
  record_count active_stage_outputs "/* audit: active_stage_outputs */ SELECT COUNT(*) FROM biz_process_stage_output WHERE is_deleted = 0"
  record_count active_stage_inputs "/* audit: active_stage_inputs */ SELECT COUNT(*) FROM biz_process_stage_input_rel WHERE is_deleted = 0"
  record_count active_chain_steps "/* audit: active_chain_steps */ SELECT COUNT(*) FROM biz_process_step WHERE is_deleted = 0 AND (input_type = 2 OR input_output_uuid IS NOT NULL OR parent_step_uuid IS NOT NULL OR stage_level > 1)"
  record_count active_finish_original_lineage "/* audit: active_finish_original_lineage */ SELECT COUNT(*) FROM biz_finish_original_rel WHERE is_deleted = 0"
  record_count deleted_process_config_drafts "/* audit: deleted_process_config_drafts */ SELECT COUNT(*) FROM biz_process_config_draft WHERE is_deleted = 1"
  record_count deleted_warehouses_with_location "/* audit: deleted_warehouses_with_location */ SELECT COUNT(*) FROM sys_warehouse WHERE is_deleted = 1 AND NULLIF(TRIM(location), '') IS NOT NULL"
}

audit_production_relationships() {
  require_zero orphan_original_order "/* audit: orphan_original_order */ SELECT COUNT(*) FROM biz_original_roll original LEFT JOIN biz_process_order o ON o.uuid = original.order_uuid AND o.is_deleted = 0 WHERE original.is_deleted = 0 AND o.uuid IS NULL"
  require_zero orphan_step_order "/* audit: orphan_step_order */ SELECT COUNT(*) FROM biz_process_step step LEFT JOIN biz_process_order o ON o.uuid = step.order_uuid AND o.is_deleted = 0 WHERE step.is_deleted = 0 AND o.uuid IS NULL"
  require_zero orphan_step_original "/* audit: orphan_step_original */ SELECT COUNT(*) FROM biz_process_step step LEFT JOIN biz_original_roll original ON original.uuid = step.original_uuid AND original.is_deleted = 0 WHERE step.is_deleted = 0 AND original.uuid IS NULL"
  require_zero step_cross_order "/* audit: step_cross_order */ SELECT COUNT(*) FROM biz_process_step step JOIN biz_original_roll original ON original.uuid = step.original_uuid AND original.is_deleted = 0 WHERE step.is_deleted = 0 AND step.order_uuid <> original.order_uuid"
  require_zero orphan_finish_order "/* audit: orphan_finish_order */ SELECT COUNT(*) FROM biz_finish_roll finish LEFT JOIN biz_process_order o ON o.uuid = finish.order_uuid AND o.is_deleted = 0 WHERE finish.is_deleted = 0 AND o.uuid IS NULL"
}

audit_stage_relationships() {
  require_zero orphan_stage_output_order "/* audit: orphan_stage_output_order */ SELECT COUNT(*) FROM biz_process_stage_output output LEFT JOIN biz_process_order o ON o.uuid = output.order_uuid AND o.is_deleted = 0 WHERE output.is_deleted = 0 AND o.uuid IS NULL"
  require_zero orphan_stage_output_original "/* audit: orphan_stage_output_original */ SELECT COUNT(*) FROM biz_process_stage_output output LEFT JOIN biz_original_roll r ON r.uuid = output.original_uuid AND r.is_deleted = 0 WHERE output.is_deleted = 0 AND r.uuid IS NULL"
  require_zero orphan_stage_output_step "/* audit: orphan_stage_output_step */ SELECT COUNT(*) FROM biz_process_stage_output output LEFT JOIN biz_process_step step ON step.uuid = output.step_uuid AND step.is_deleted = 0 WHERE output.is_deleted = 0 AND step.uuid IS NULL"
  require_zero orphan_stage_output_parent "/* audit: orphan_stage_output_parent */ SELECT COUNT(*) FROM biz_process_stage_output output LEFT JOIN biz_process_stage_output parent ON parent.uuid = output.parent_output_uuid AND parent.is_deleted = 0 WHERE output.is_deleted = 0 AND output.parent_output_uuid IS NOT NULL AND parent.uuid IS NULL"
  require_zero orphan_stage_output_finish "/* audit: orphan_stage_output_finish */ SELECT COUNT(*) FROM biz_process_stage_output output LEFT JOIN biz_finish_roll finish ON finish.uuid = output.finish_roll_uuid AND finish.is_deleted = 0 WHERE output.is_deleted = 0 AND output.finish_roll_uuid IS NOT NULL AND finish.uuid IS NULL"
  require_zero stage_output_cross_order "/* audit: stage_output_cross_order */ SELECT COUNT(*) FROM biz_process_stage_output output JOIN biz_process_step step ON step.uuid = output.step_uuid AND step.is_deleted = 0 JOIN biz_original_roll original ON original.uuid = output.original_uuid AND original.is_deleted = 0 WHERE output.is_deleted = 0 AND (output.order_uuid <> step.order_uuid OR output.original_uuid <> step.original_uuid OR output.order_uuid <> original.order_uuid)"
  require_zero stage_output_parent_cross_order "/* audit: stage_output_parent_cross_order */ SELECT COUNT(*) FROM biz_process_stage_output output JOIN biz_process_stage_output parent ON parent.uuid = output.parent_output_uuid AND parent.is_deleted = 0 WHERE output.is_deleted = 0 AND output.parent_output_uuid IS NOT NULL AND (output.order_uuid <> parent.order_uuid OR output.original_uuid <> parent.original_uuid OR output.stage_level <> parent.stage_level + 1)"
  require_zero stage_output_finish_cross_order "/* audit: stage_output_finish_cross_order */ SELECT COUNT(*) FROM biz_process_stage_output output JOIN biz_finish_roll finish ON finish.uuid = output.finish_roll_uuid AND finish.is_deleted = 0 WHERE output.is_deleted = 0 AND output.finish_roll_uuid IS NOT NULL AND output.order_uuid <> finish.order_uuid"
  require_zero orphan_stage_input_order "/* audit: orphan_stage_input_order */ SELECT COUNT(*) FROM biz_process_stage_input_rel input_rel LEFT JOIN biz_process_order o ON o.uuid = input_rel.order_uuid AND o.is_deleted = 0 WHERE input_rel.is_deleted = 0 AND o.uuid IS NULL"
  require_zero orphan_stage_input_original "/* audit: orphan_stage_input_original */ SELECT COUNT(*) FROM biz_process_stage_input_rel input_rel LEFT JOIN biz_original_roll original ON original.uuid = input_rel.original_uuid AND original.is_deleted = 0 WHERE input_rel.is_deleted = 0 AND original.uuid IS NULL"
  require_zero orphan_stage_input_output "/* audit: orphan_stage_input_output */ SELECT COUNT(*) FROM biz_process_stage_input_rel input_rel LEFT JOIN biz_process_stage_output output ON output.uuid = input_rel.input_output_uuid AND output.is_deleted = 0 WHERE input_rel.is_deleted = 0 AND output.uuid IS NULL"
  require_zero orphan_stage_input_step "/* audit: orphan_stage_input_step */ SELECT COUNT(*) FROM biz_process_stage_input_rel input_rel LEFT JOIN biz_process_step step ON step.uuid = input_rel.step_uuid AND step.is_deleted = 0 WHERE input_rel.is_deleted = 0 AND step.uuid IS NULL"
  require_zero orphan_stage_input_source_step "/* audit: orphan_stage_input_source_step */ SELECT COUNT(*) FROM biz_process_stage_input_rel input_rel LEFT JOIN biz_process_step source_step ON source_step.uuid = input_rel.source_step_uuid AND source_step.is_deleted = 0 WHERE input_rel.is_deleted = 0 AND input_rel.source_step_uuid IS NOT NULL AND source_step.uuid IS NULL"
  require_zero stage_input_cross_order "/* audit: stage_input_cross_order */ SELECT COUNT(*) FROM biz_process_stage_input_rel input_rel JOIN biz_process_stage_output output ON output.uuid = input_rel.input_output_uuid AND output.is_deleted = 0 JOIN biz_process_step step ON step.uuid = input_rel.step_uuid AND step.is_deleted = 0 JOIN biz_original_roll original ON original.uuid = input_rel.original_uuid AND original.is_deleted = 0 WHERE input_rel.is_deleted = 0 AND (input_rel.order_uuid <> output.order_uuid OR input_rel.original_uuid <> output.original_uuid OR input_rel.order_uuid <> step.order_uuid OR input_rel.original_uuid <> step.original_uuid OR input_rel.order_uuid <> original.order_uuid OR input_rel.stage_level <> step.stage_level OR input_rel.stage_level <> output.stage_level + 1 OR output.step_uuid <> COALESCE(input_rel.source_step_uuid, output.step_uuid))"
}

audit_lineage_relationships() {
  require_zero orphan_process_param_order "/* audit: orphan_process_param_order */ SELECT COUNT(*) FROM biz_process_param param LEFT JOIN biz_process_order o ON o.uuid = param.order_uuid AND o.is_deleted = 0 WHERE param.is_deleted = 0 AND o.uuid IS NULL"
  require_zero orphan_process_param_original "/* audit: orphan_process_param_original */ SELECT COUNT(*) FROM biz_process_param param LEFT JOIN biz_original_roll r ON r.uuid = param.original_uuid AND r.is_deleted = 0 WHERE param.is_deleted = 0 AND r.uuid IS NULL"
  require_zero orphan_process_param_step "/* audit: orphan_process_param_step */ SELECT COUNT(*) FROM biz_process_param param LEFT JOIN biz_process_step step ON step.uuid = param.step_uuid AND step.is_deleted = 0 WHERE param.is_deleted = 0 AND param.step_uuid IS NOT NULL AND step.uuid IS NULL"
  require_zero process_param_cross_order "/* audit: process_param_cross_order */ SELECT COUNT(*) FROM biz_process_param param JOIN biz_original_roll original ON original.uuid = param.original_uuid AND original.is_deleted = 0 LEFT JOIN biz_process_step step ON step.uuid = param.step_uuid AND step.is_deleted = 0 WHERE param.is_deleted = 0 AND (param.order_uuid <> original.order_uuid OR (param.step_uuid IS NOT NULL AND (step.uuid IS NULL OR param.order_uuid <> step.order_uuid OR param.original_uuid <> step.original_uuid)))"
  require_zero orphan_lineage_finish "/* audit: orphan_lineage_finish */ SELECT COUNT(*) FROM biz_finish_original_rel rel LEFT JOIN biz_finish_roll finish ON finish.uuid = rel.finish_uuid AND finish.is_deleted = 0 WHERE rel.is_deleted = 0 AND finish.uuid IS NULL"
  require_zero orphan_lineage_original "/* audit: orphan_lineage_original */ SELECT COUNT(*) FROM biz_finish_original_rel rel LEFT JOIN biz_original_roll original ON original.uuid = rel.original_uuid AND original.is_deleted = 0 WHERE rel.is_deleted = 0 AND original.uuid IS NULL"
  require_zero orphan_lineage_order "/* audit: orphan_lineage_order */ SELECT COUNT(*) FROM biz_finish_original_rel rel LEFT JOIN biz_process_order o ON o.uuid = rel.order_uuid AND o.is_deleted = 0 WHERE rel.is_deleted = 0 AND o.uuid IS NULL"
  require_zero lineage_cross_order "/* audit: lineage_cross_order */ SELECT COUNT(*) FROM biz_finish_original_rel rel JOIN biz_finish_roll finish ON finish.uuid = rel.finish_uuid AND finish.is_deleted = 0 JOIN biz_original_roll original ON original.uuid = rel.original_uuid AND original.is_deleted = 0 WHERE rel.is_deleted = 0 AND (rel.order_uuid <> finish.order_uuid OR rel.order_uuid <> original.order_uuid OR finish.order_uuid <> original.order_uuid)"
  require_zero duplicate_active_finish_original_pair "/* audit: duplicate_active_finish_original_pair */ SELECT COUNT(*) FROM (SELECT finish_uuid, original_uuid FROM biz_finish_original_rel WHERE is_deleted = 0 GROUP BY finish_uuid, original_uuid HAVING COUNT(*) > 1) conflicts"
  require_zero deleted_warehouse_finish_reference "/* audit: deleted_warehouse_finish_reference */ SELECT COUNT(*) FROM biz_finish_roll finish JOIN sys_warehouse warehouse ON warehouse.uuid = finish.warehouse_uuid AND warehouse.is_deleted = 1 WHERE finish.is_deleted = 0 AND finish.warehouse_uuid IS NOT NULL"
  require_zero deleted_warehouse_order_reference "/* audit: deleted_warehouse_order_reference */ SELECT COUNT(*) FROM biz_process_order o JOIN sys_warehouse warehouse ON warehouse.uuid = o.warehouse_uuid AND warehouse.is_deleted = 1 WHERE o.is_deleted = 0 AND o.warehouse_uuid IS NOT NULL"
}

audit_document_relationships() {
  require_zero orphan_delivery_order "/* audit: orphan_delivery_order */ SELECT COUNT(*) FROM biz_delivery_detail detail LEFT JOIN biz_delivery_order delivery ON delivery.uuid = detail.delivery_uuid AND delivery.is_deleted = 0 WHERE detail.is_deleted = 0 AND delivery.uuid IS NULL"
  require_zero orphan_delivery_finish "/* audit: orphan_delivery_finish */ SELECT COUNT(*) FROM biz_delivery_detail detail LEFT JOIN biz_finish_roll finish ON finish.uuid = detail.finish_uuid AND finish.is_deleted = 0 WHERE detail.is_deleted = 0 AND finish.uuid IS NULL"
  require_zero orphan_delivery_source_order "/* audit: orphan_delivery_source_order */ SELECT COUNT(*) FROM biz_delivery_detail detail LEFT JOIN biz_process_order o ON o.uuid = detail.order_uuid AND o.is_deleted = 0 WHERE detail.is_deleted = 0 AND o.uuid IS NULL"
  require_zero delivery_detail_cross_order "/* audit: delivery_detail_cross_order */ SELECT COUNT(*) FROM biz_delivery_detail detail JOIN biz_finish_roll finish ON finish.uuid = detail.finish_uuid AND finish.is_deleted = 0 JOIN biz_process_order o ON o.uuid = detail.order_uuid AND o.is_deleted = 0 WHERE detail.is_deleted = 0 AND detail.order_uuid <> finish.order_uuid"
  require_zero orphan_settle_detail_order "/* audit: orphan_settle_detail_order */ SELECT COUNT(*) FROM biz_settle_detail detail LEFT JOIN biz_process_order o ON o.uuid = detail.order_uuid AND o.is_deleted = 0 WHERE detail.is_deleted = 0 AND o.uuid IS NULL"
  require_zero orphan_settle_detail_settle "/* audit: orphan_settle_detail_settle */ SELECT COUNT(*) FROM biz_settle_detail detail LEFT JOIN biz_settle_order settle ON settle.uuid = detail.settle_uuid AND settle.is_deleted = 0 WHERE detail.is_deleted = 0 AND settle.uuid IS NULL"
  require_zero orphan_receive_settle "/* audit: orphan_receive_settle */ SELECT COUNT(*) FROM biz_receive_record receive LEFT JOIN biz_settle_order settle ON settle.uuid = receive.settle_uuid AND settle.is_deleted = 0 WHERE receive.is_deleted = 0 AND settle.uuid IS NULL"
  require_zero orphan_settle_reminder_settle "/* audit: orphan_settle_reminder_settle */ SELECT COUNT(*) FROM biz_settle_collection_reminder reminder LEFT JOIN biz_settle_order settle ON settle.uuid = reminder.settle_uuid AND settle.is_deleted = 0 WHERE reminder.is_deleted = 0 AND settle.uuid IS NULL"
  require_zero orphan_discount_approval_settle "/* audit: orphan_discount_approval_settle */ SELECT COUNT(*) FROM biz_settle_discount_approval approval LEFT JOIN biz_settle_order settle ON settle.uuid = approval.settle_uuid AND settle.is_deleted = 0 WHERE approval.is_deleted = 0 AND settle.uuid IS NULL"
}

audit_soft_delete_lifecycles() {
  require_zero soft_delete_process_config_risk "/* audit: soft_delete_process_config_risk */ SELECT COUNT(*) FROM (SELECT order_uuid, original_uuid FROM biz_process_config_draft GROUP BY order_uuid, original_uuid HAVING SUM(CASE WHEN is_deleted = 0 THEN 1 ELSE 0 END) > 0 AND SUM(CASE WHEN is_deleted = 1 THEN 1 ELSE 0 END) > 0) conflicts"
  require_zero soft_delete_dict_item_risk "/* audit: soft_delete_dict_item_risk */ SELECT COUNT(*) FROM (SELECT dict_type, item_code FROM sys_dict_item GROUP BY dict_type, item_code HAVING SUM(CASE WHEN is_deleted = 0 THEN 1 ELSE 0 END) > 0 AND SUM(CASE WHEN is_deleted = 1 THEN 1 ELSE 0 END) > 0) conflicts"
  require_zero soft_delete_config_item_risk "/* audit: soft_delete_config_item_risk */ SELECT COUNT(*) FROM (SELECT config_key FROM sys_config_item GROUP BY config_key HAVING SUM(CASE WHEN is_deleted = 0 THEN 1 ELSE 0 END) > 0 AND SUM(CASE WHEN is_deleted = 1 THEN 1 ELSE 0 END) > 0) conflicts"
  require_zero soft_delete_no_rule_risk "/* audit: soft_delete_no_rule_risk */ SELECT COUNT(*) FROM (SELECT biz_type FROM sys_no_rule GROUP BY biz_type HAVING SUM(CASE WHEN is_deleted = 0 THEN 1 ELSE 0 END) > 0 AND SUM(CASE WHEN is_deleted = 1 THEN 1 ELSE 0 END) > 0) conflicts"
}

prepare_audit() {
  local command_name
  for command_name in "${MYSQL_BIN}" date mktemp rm mkdir mv cat chmod; do
    command -v "${command_name}" >/dev/null 2>&1 || fail "required command not found: ${command_name}"
  done
  require_identifier DB_NAME "${DB_NAME}"
  require_identifier DB_USER "${DB_USER}"
  [ ! -e "${OUTPUT_DIR}" ] || fail "refusing to overwrite output directory: ${OUTPUT_DIR}"
  mkdir -p "${OUTPUT_DIR}"
  cat > "${mysql_cnf}" <<EOF
[client]
host=${DB_HOST}
port=${DB_PORT}
user=${DB_USER}
password=${DB_PASSWORD}
default-character-set=utf8mb4
EOF
  chmod 600 "${mysql_cnf}"
  printf 'check_name\tcount\n' > "${report_tmp}"
}

main() {
  prepare_audit
  audit_baseline_counts
  audit_production_relationships
  audit_stage_relationships
  audit_lineage_relationships
  audit_document_relationships
  audit_soft_delete_lifecycles

  mv "${report_tmp}" "${OUTPUT_DIR}/stage1-readonly-audit.tsv"
  [ "${conflict_checks}" = "0" ] \
    || fail "${conflict_checks} relationship or lifecycle check(s) reported conflicts; review ${OUTPUT_DIR}/stage1-readonly-audit.tsv"
  echo "stage 1 read-only audit passed: ${OUTPUT_DIR}/stage1-readonly-audit.tsv"
}

main "$@"
