#!/usr/bin/env bash
set -euo pipefail
umask 077

DIAG_ENV_FILE="${DIAG_ENV_FILE:-${MIGRATION_ENV_FILE:-/etc/paper-mes/migration.env}}"
if [ -r "${DIAG_ENV_FILE}" ]; then
  set -a
  . "${DIAG_ENV_FILE}"
  set +a
fi

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-paper_processing}"
DB_USER="${DB_USER:-paper_mes_app}"
DB_PASSWORD="${DB_PASSWORD:?set DB_PASSWORD or DIAG_ENV_FILE before diagnosis}"
LIMIT_SIZE="${LIMIT_SIZE:-20}"
DATE_FROM="${DATE_FROM:-2026-01-01}"
DATE_TO="${DATE_TO:-2026-12-31}"
KEYWORD="${KEYWORD:-}"

mysql_cnf="$(mktemp)"

cleanup() { rm -f "${mysql_cnf}"; }
trap cleanup EXIT

fail() {
  echo "$1" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

require_safe_identifier() {
  local name="$1"
  local value="$2"
  [[ "${value}" =~ ^[A-Za-z0-9_]+$ ]] || fail "invalid ${name}: ${value}"
}

require_positive_int() {
  local name="$1"
  local value="$2"
  [[ "${value}" =~ ^[1-9][0-9]*$ ]] || fail "${name} must be a positive integer"
}

require_non_negative_int() {
  local name="$1"
  local value="$2"
  [[ "${value}" =~ ^[0-9]+$ ]] || fail "${name} must be a non-negative integer"
}

require_safe_date() {
  local name="$1"
  local value="$2"
  [[ "${value}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]] || fail "invalid ${name}: ${value}"
}

sql_escape() { printf "%s" "$1" | sed "s/'/''/g"; }
quote() { printf "'%s'" "$(sql_escape "$1")"; }
mysql_exec() { mysql --defaults-extra-file="${mysql_cnf}" "${DB_NAME}" --batch --raw "$@"; }
mysql_scalar() { mysql_exec --skip-column-names -e "$1" | head -n 1; }
section() { printf "\n## %s\n" "$1"; }

setup_mysql_cnf() {
  cat > "${mysql_cnf}" <<EOF
[client]
host=${DB_HOST}
port=${DB_PORT}
user=${DB_USER}
password=${DB_PASSWORD}
default-character-set=utf8mb4
EOF
  chmod 600 "${mysql_cnf}"
}

run_sql() {
  local title="$1"
  local sql="$2"
  section "${title}"
  mysql_exec -e "${sql}"
}

explain_sql() {
  local title="$1"
  local sql="$2"
  section "${title}"
  mysql_exec -e "EXPLAIN FORMAT=JSON ${sql}"
}

sample_value() {
  local sql="$1"
  local fallback="$2"
  local value
  value="$(mysql_scalar "${sql}" || true)"
  printf "%s" "${value:-${fallback}}"
}

init_samples() {
  PROCESS_ORDER_UUID="${ORDER_UUID:-$(sample_value "SELECT uuid FROM biz_process_order WHERE is_deleted = 0 AND order_status <> 6 ORDER BY create_time DESC LIMIT 1" "__sample_order__")}"
  PROCESS_CUSTOMER_UUID="${CUSTOMER_UUID:-$(sample_value "SELECT customer_uuid FROM biz_process_order WHERE is_deleted = 0 AND customer_uuid <> '' ORDER BY create_time DESC LIMIT 1" "__sample_customer__")}"
  DELIVERY_CUSTOMER_UUID="${CUSTOMER_UUID:-$(sample_value "SELECT customer_uuid FROM biz_delivery_order WHERE is_deleted = 0 AND customer_uuid <> '' ORDER BY create_time DESC LIMIT 1" "${PROCESS_CUSTOMER_UUID}")}"
  SETTLE_CUSTOMER_UUID="${CUSTOMER_UUID:-$(sample_value "SELECT customer_uuid FROM biz_settle_order WHERE is_deleted = 0 AND customer_uuid <> '' ORDER BY create_time DESC LIMIT 1" "${PROCESS_CUSTOMER_UUID}")}"
  ORDER_STATUS="${ORDER_STATUS:-$(sample_value "SELECT order_status FROM biz_process_order WHERE is_deleted = 0 AND order_status <> 6 ORDER BY create_time DESC LIMIT 1" "1")}"
  DELIVERY_STATUS="${DELIVERY_STATUS:-$(sample_value "SELECT delivery_status FROM biz_delivery_order WHERE is_deleted = 0 ORDER BY create_time DESC LIMIT 1" "1")}"
  SETTLE_STATUS="${SETTLE_STATUS:-$(sample_value "SELECT settle_status FROM biz_settle_order WHERE is_deleted = 0 ORDER BY create_time DESC LIMIT 1" "1")}"
  require_non_negative_int "ORDER_STATUS" "${ORDER_STATUS}"
  require_non_negative_int "DELIVERY_STATUS" "${DELIVERY_STATUS}"
  require_non_negative_int "SETTLE_STATUS" "${SETTLE_STATUS}"
}

show_table_sizes() {
  run_sql "Table sizes" "SELECT TABLE_NAME, TABLE_ROWS, ROUND(DATA_LENGTH / 1024 / 1024, 2) AS DATA_MB, ROUND(INDEX_LENGTH / 1024 / 1024, 2) AS INDEX_MB FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('biz_process_order', 'biz_original_roll', 'biz_process_step', 'biz_finish_roll', 'biz_delivery_order', 'biz_delivery_detail', 'biz_settle_order', 'biz_settle_detail', 'sys_customer', 'sys_paper', 'sys_machine', 'sys_warehouse') ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC;"
}

show_indexes() {
  for table in biz_process_order biz_delivery_order biz_settle_order biz_finish_roll; do
    run_sql "Indexes: ${table}" "SHOW INDEX FROM \`${table}\`;"
  done
}

explain_process_lists() {
  explain_sql "Process order active list" "SELECT uuid, order_no, customer_uuid, order_status, order_date, create_time FROM biz_process_order WHERE is_deleted = 0 AND order_status <> 6 ORDER BY create_time DESC LIMIT ${LIMIT_SIZE};"
  explain_sql "Process order filtered list" "SELECT uuid, order_no, customer_uuid, order_status, order_date, create_time FROM biz_process_order WHERE is_deleted = 0 AND customer_uuid = $(quote "${PROCESS_CUSTOMER_UUID}") AND order_status = ${ORDER_STATUS} AND order_date >= $(quote "${DATE_FROM}") AND order_date <= $(quote "${DATE_TO}") ORDER BY create_time DESC LIMIT ${LIMIT_SIZE};"
}

explain_delivery_list() {
  explain_sql "Delivery filtered list" "SELECT uuid, delivery_no, customer_uuid, delivery_status, delivery_date, create_time FROM biz_delivery_order WHERE is_deleted = 0 AND customer_uuid = $(quote "${DELIVERY_CUSTOMER_UUID}") AND delivery_status = ${DELIVERY_STATUS} AND delivery_date >= $(quote "${DATE_FROM}") AND delivery_date <= $(quote "${DATE_TO}") ORDER BY create_time DESC LIMIT ${LIMIT_SIZE};"
}

explain_settle_list() {
  explain_sql "Settle filtered list" "SELECT uuid, settle_no, customer_uuid, settle_status, settle_date, create_time FROM biz_settle_order WHERE is_deleted = 0 AND customer_uuid = $(quote "${SETTLE_CUSTOMER_UUID}") AND settle_status = ${SETTLE_STATUS} AND settle_date >= $(quote "${DATE_FROM}") AND settle_date <= $(quote "${DATE_TO}") ORDER BY create_time DESC LIMIT ${LIMIT_SIZE};"
}

explain_keyword_lists() {
  [ -n "${KEYWORD}" ] || return
  explain_sql "Process order keyword list" "SELECT uuid, order_no, customer_name, create_time FROM biz_process_order WHERE is_deleted = 0 AND order_status <> 6 AND (order_no LIKE $(quote "%${KEYWORD}%") OR customer_name LIKE $(quote "%${KEYWORD}%")) ORDER BY create_time DESC LIMIT ${LIMIT_SIZE};"
}

explain_child_loaders() {
  explain_sql "Original rolls by order" "SELECT uuid, order_uuid, roll_no, row_sort FROM biz_original_roll WHERE is_deleted = 0 AND order_uuid = $(quote "${PROCESS_ORDER_UUID}") ORDER BY order_uuid, row_sort;"
  explain_sql "Finish rolls by order" "SELECT uuid, order_uuid, finish_roll_no, row_sort FROM biz_finish_roll WHERE is_deleted = 0 AND order_uuid = $(quote "${PROCESS_ORDER_UUID}") ORDER BY order_uuid, row_sort;"
}

main() {
  require_command mysql
  require_command mktemp
  require_command sed
  require_command head
  require_safe_identifier "DB_NAME" "${DB_NAME}"
  require_safe_identifier "DB_USER" "${DB_USER}"
  require_positive_int "LIMIT_SIZE" "${LIMIT_SIZE}"
  require_safe_date "DATE_FROM" "${DATE_FROM}"
  require_safe_date "DATE_TO" "${DATE_TO}"
  setup_mysql_cnf
  init_samples
  show_table_sizes
  show_indexes
  explain_process_lists
  explain_delivery_list
  explain_settle_list
  explain_keyword_lists
  explain_child_loaders
}

main "$@"
