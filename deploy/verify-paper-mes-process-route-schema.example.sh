#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

MIGRATION_ENV_FILE="${MIGRATION_ENV_FILE:-/etc/paper-mes-test/migration.env}"
if [ -r "${MIGRATION_ENV_FILE}" ]; then
  set -a
  # shellcheck disable=SC1090
  . "${MIGRATION_ENV_FILE}"
  set +a
fi

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-paper_processing}"
DB_PASSWORD="${DB_PASSWORD:?set DB_PASSWORD or MIGRATION_ENV_FILE before route schema verification}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"

required_tables=(
  biz_process_order biz_original_roll biz_process_config_draft biz_process_step
  biz_process_stage_output biz_process_stage_input_rel biz_process_param
  biz_finish_roll biz_finish_original_rel sys_roll_no_sequence
)
required_columns=(
  "biz_process_order:uuid" "biz_process_order:order_status"
  "biz_process_order:customer_uuid" "biz_process_order:warehouse_uuid"
  "biz_original_roll:uuid" "biz_original_roll:order_uuid" "biz_original_roll:roll_weight"
  "biz_original_roll:original_width" "biz_original_roll:actual_width"
  "biz_original_roll:original_diameter" "biz_original_roll:machine_uuid"
  "biz_process_config_draft:uuid" "biz_process_config_draft:order_uuid"
  "biz_process_config_draft:original_uuid" "biz_process_config_draft:config_json"
  "biz_process_config_draft:config_status"
  "biz_process_step:uuid" "biz_process_step:order_uuid" "biz_process_step:original_uuid"
  "biz_process_step:input_type" "biz_process_step:input_output_uuid"
  "biz_process_step:stage_level" "biz_process_step:parent_step_uuid"
  "biz_process_step:step_sort" "biz_process_step:step_type" "biz_process_step:step_name"
  "biz_process_step:machine_uuid" "biz_process_step:machine_name_snap"
  "biz_process_step:is_main" "biz_process_step:knife_count" "biz_process_step:process_weight"
  "biz_process_step:unit_price" "biz_process_step:step_amount"
  "biz_process_step:width_difference_policy" "biz_process_step:planned_loss_width"
  "biz_process_step:planned_loss_weight"
  "biz_process_stage_output:uuid" "biz_process_stage_output:order_uuid"
  "biz_process_stage_output:original_uuid" "biz_process_stage_output:step_uuid"
  "biz_process_stage_output:parent_output_uuid" "biz_process_stage_output:stage_level"
  "biz_process_stage_output:output_sort" "biz_process_stage_output:output_type"
  "biz_process_stage_output:output_status" "biz_process_stage_output:output_no"
  "biz_process_stage_output:finish_roll_uuid" "biz_process_stage_output:paper_name"
  "biz_process_stage_output:gram_weight" "biz_process_stage_output:finish_width"
  "biz_process_stage_output:finish_diameter" "biz_process_stage_output:finish_core_diameter"
  "biz_process_stage_output:estimate_weight" "biz_process_stage_output:actual_weight"
  "biz_process_stage_output:source_step_type" "biz_process_stage_output:source_summary"
  "biz_process_stage_output:remark"
  "biz_process_stage_input_rel:uuid" "biz_process_stage_input_rel:order_uuid"
  "biz_process_stage_input_rel:original_uuid" "biz_process_stage_input_rel:step_uuid"
  "biz_process_stage_input_rel:input_output_uuid" "biz_process_stage_input_rel:source_step_uuid"
  "biz_process_stage_input_rel:input_sort" "biz_process_stage_input_rel:stage_level"
  "biz_process_param:uuid" "biz_process_param:order_uuid" "biz_process_param:original_uuid"
  "biz_process_param:step_uuid" "biz_process_param:param_mode" "biz_process_param:layer_sort"
  "biz_process_param:out_diameter" "biz_process_param:core_diameter"
  "biz_process_param:layer_width" "biz_process_param:area_value"
  "biz_process_param:area_ratio" "biz_process_param:param_json" "biz_process_param:remark"
  "biz_finish_roll:uuid" "biz_finish_roll:order_uuid" "biz_finish_roll:row_sort"
  "biz_finish_roll:finish_roll_no" "biz_finish_roll:roll_no_status" "biz_finish_roll:is_spare"
  "biz_finish_roll:is_remain" "biz_finish_roll:source_type" "biz_finish_roll:finish_status"
  "biz_finish_roll:warehouse_uuid" "biz_finish_roll:original_roll_nos"
  "biz_finish_roll:paper_name" "biz_finish_roll:gram_weight" "biz_finish_roll:finish_width"
  "biz_finish_roll:finish_diameter" "biz_finish_roll:finish_core_diameter"
  "biz_finish_roll:estimate_weight" "biz_finish_roll:estimate_weight_snap"
  "biz_finish_roll:remaining_weight" "biz_finish_roll:remain_own_weight"
  "biz_finish_roll:ownership_status" "biz_finish_roll:remain_transfer_state"
  "biz_finish_roll:remark"
  "biz_finish_original_rel:uuid" "biz_finish_original_rel:order_uuid"
  "biz_finish_original_rel:finish_uuid" "biz_finish_original_rel:original_uuid"
  "biz_finish_original_rel:share_ratio" "biz_finish_original_rel:share_weight"
  "biz_finish_original_rel:remark" "sys_roll_no_sequence:sequence_key"
)

mysql_cnf="$(mktemp)"
cleanup() { rm -f "${mysql_cnf}"; }
trap cleanup EXIT

fail() {
  echo "process route schema verification failed: $1" >&2
  exit 1
}

require_identifier() {
  [[ "$2" =~ ^[A-Za-z0-9_]+$ ]] || fail "invalid $1"
}

mysql_query() {
  "${MYSQL_BIN}" --defaults-extra-file="${mysql_cnf}" --batch --skip-column-names \
    "${DB_NAME}" -e "$1"
}

check_table() {
  local table="$1" count
  count="$(mysql_query "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='${table}'")"
  [ "${count}" = "1" ] || missing+=("table:${table}")
}

check_column() {
  local table="$1" column="$2" count
  count="$(mysql_query "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='${table}' AND column_name='${column}'")"
  [ "${count}" = "1" ] || missing+=("column:${table}.${column}")
}

main() {
  local table column
  local -a missing=()

  require_identifier DB_NAME "${DB_NAME}"
  require_identifier DB_USER "${DB_USER:-paper_mes_migrator}"
  command -v "${MYSQL_BIN}" >/dev/null 2>&1 || fail "required command not found: ${MYSQL_BIN}"
  cat > "${mysql_cnf}" <<EOF
[client]
host=${DB_HOST}
port=${DB_PORT}
user=${DB_USER:-paper_mes_migrator}
password=${DB_PASSWORD}
default-character-set=utf8mb4
EOF
  chmod 600 "${mysql_cnf}"

  for table in "${required_tables[@]}"; do check_table "${table}"; done
  for column in "${required_columns[@]}"; do
    table="${column%%:*}"
    column="${column#*:}"
    check_column "${table}" "${column}"
  done
  if [ "${#missing[@]}" -gt 0 ]; then
    printf 'missing process route structures:\n' >&2
    printf '  %s\n' "${missing[@]}" >&2
    exit 1
  fi
  echo "process route schema verification passed"
}

main "$@"
