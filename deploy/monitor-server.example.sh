#!/usr/bin/env bash
set -Eeuo pipefail

MONITOR_ENV_FILE="${MONITOR_ENV_FILE:-/etc/paper-mes/server-monitor.env}"
MONITOR_LIB_DIR="${MONITOR_LIB_DIR:-/usr/local/lib/paper-mes-monitor}"

if [ -r "${MONITOR_ENV_FILE}" ]; then
  set -a
  # shellcheck disable=SC1090
  . "${MONITOR_ENV_FILE}"
  set +a
fi

# shellcheck disable=SC1091
. "${MONITOR_LIB_DIR}/server-monitor-checks.sh"
# shellcheck disable=SC1091
. "${MONITOR_LIB_DIR}/server-monitor-state.sh"
# shellcheck disable=SC1091
. "${MONITOR_LIB_DIR}/server-monitor-heartbeat.sh"

HTTP_TIMEOUT_SECONDS="${HTTP_TIMEOUT_SECONDS:-10}"
HTTP_MAX_SECONDS="${HTTP_MAX_SECONDS:-5}"
CERT_MIN_DAYS="${CERT_MIN_DAYS:-21}"
MAX_LOAD_PER_CPU="${MAX_LOAD_PER_CPU:-2}"
MIN_MEMORY_AVAILABLE_PERCENT="${MIN_MEMORY_AVAILABLE_PERCENT:-10}"
MAX_DISK_USED_PERCENT="${MAX_DISK_USED_PERCENT:-85}"
MAX_INODE_USED_PERCENT="${MAX_INODE_USED_PERCENT:-85}"
MAX_BACKUP_AGE_HOURS="${MAX_BACKUP_AGE_HOURS:-48}"
MAX_REMOTE_SYNC_AGE_HOURS="${MAX_REMOTE_SYNC_AGE_HOURS:-48}"
MAX_CHECK_FILE_AGE_HOURS="${MAX_CHECK_FILE_AGE_HOURS:-48}"
REMINDER_HOURS="${REMINDER_HOURS:-2}"
STATE_FILE="${STATE_FILE:-/var/lib/server-monitor/state}"
ALERT_WEBHOOK_URL="${ALERT_WEBHOOK_URL:-}"
ALERT_WEBHOOK_BEARER_TOKEN="${ALERT_WEBHOOK_BEARER_TOKEN:-}"
ALERT_EMAIL_COMMAND="${ALERT_EMAIL_COMMAND:-}"
DEAD_MAN_SWITCH_URL="${DEAD_MAN_SWITCH_URL:-}"
INTERNAL_FAILURE_COMMAND="${INTERNAL_FAILURE_COMMAND-/usr/local/sbin/notify-server-monitor-internal}"

SYSTEMD_UNITS="${SYSTEMD_UNITS:-nginx.service mysql.service docker.service paper-mes.service paper-mes-test.service pm2-root.service}"
SYSTEMD_TIMERS="${SYSTEMD_TIMERS:-certbot.timer paper-mes-offsite-backup.timer}"
DOCKER_CONTAINERS="${DOCKER_CONTAINERS:-jimureport jimureport-mysql}"
HTTP_PROBES="${HTTP_PROBES:-MES production API|json-up|http://127.0.0.1:8081/actuator/health|200
MES test API|json-up|http://127.0.0.1:8082/actuator/health|200
WMS backend (erp.nbsmzwl.cn)|code|http://127.0.0.1:3000/api/health/status|401
Roll warehouse scanner API (wms.nbsmzwl.cn)|json-success|http://127.0.0.1:3001/api/health|200
JimuReport printing service|code|http://127.0.0.1:8085/|200
MES production website|code|https://mes.nbsmzwl.cn/|200
MES test website|code|https://mes-test.nbsmzwl.cn/|200
WMS website|code|https://erp.nbsmzwl.cn/|200
Roll warehouse scanner website|code|https://wms.nbsmzwl.cn/|200}"
CERTIFICATES="${CERTIFICATES:-MES production|/etc/letsencrypt/live/mes.nbsmzwl.cn/fullchain.pem
MES test|/etc/letsencrypt/live/mes-test.nbsmzwl.cn/fullchain.pem
WMS|/etc/letsencrypt/live/erp.nbsmzwl.cn/fullchain.pem
Roll warehouse scanner|/etc/letsencrypt/live/wms.nbsmzwl.cn/fullchain.pem}"
BACKUP_ROOTS="${BACKUP_ROOTS:-MES backup|/opt/backups/paper-mes
Business projects backup|/opt/backups/business-projects}"
REMOTE_STATUS_FILES="${REMOTE_STATUS_FILES:-MES backup|/opt/backups/paper-mes/.remote-sync-status
Business projects backup|/opt/backups/business-projects/.remote-sync-status}"
FRESH_CHECK_FILES="${FRESH_CHECK_FILES:-Production hardening check|/var/log/production-hardening-check.log|Production hardening check passed.
Business projects backup task|/var/log/project-independent-backup.log|independent backup completed:}"

for variable in HTTP_TIMEOUT_SECONDS CERT_MIN_DAYS MIN_MEMORY_AVAILABLE_PERCENT \
  MAX_DISK_USED_PERCENT MAX_INODE_USED_PERCENT MAX_BACKUP_AGE_HOURS \
  MAX_REMOTE_SYNC_AGE_HOURS MAX_CHECK_FILE_AGE_HOURS REMINDER_HOURS; do
  require_positive_integer "${variable}" "${!variable}"
done

issues_en=()
issues_zh=()
issue_keys=()
check_systemd_units
check_timers
check_failed_units
check_mysql
check_docker_containers
check_http_probes
check_certificates
check_host_resources
check_backups
check_remote_statuses
check_fresh_files

monitor_result=0
process_monitor_result || monitor_result=$?
if (( monitor_result != 0 && monitor_result != 3 )); then
  exit "${monitor_result}"
fi
if [ -n "${INTERNAL_FAILURE_COMMAND}" ]; then
  "${INTERNAL_FAILURE_COMMAND}" recovered
fi
send_dead_man_switch
exit "${monitor_result}"
