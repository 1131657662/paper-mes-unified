#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
root_mysql_cnf=/etc/mysql/debian.cnf

fail() {
  echo "backup verification wrapper refused: $1" >&2
  exit 1
}

[ "$(id -u)" = 0 ] || fail "root privileges are required"
[ "$#" = 1 ] || fail "exactly one backup id is required"
backup_id="$1"
[[ "${backup_id}" =~ ^[0-9]{8}-[0-9]{6}$ ]] || fail "invalid backup id"

wrapper_name="$(basename "$0")"
case "${wrapper_name}" in
  verify-paper-mes-backup-root)
    backup_env=/etc/paper-mes/backup-restore.env
    backup_root=/opt/backups/paper-mes
    verifier=/usr/local/bin/verify-paper-mes-backup
    restore_db=paper_mes_restore_check
    service_user=paper-mes
    ;;
  verify-paper-mes-test-backup-root)
    backup_env=/etc/paper-mes-test/backup-restore.env
    backup_root=/opt/paper-mes-test/backups
    verifier=/usr/local/bin/verify-paper-mes-test-backup
    restore_db=paper_mes_test_restore_check
    service_user=paper-mes-test
    ;;
  *)
    fail "unsupported wrapper name"
    ;;
esac

[ -f "${backup_env}" ] || fail "root-only restore configuration is missing"
[ "$(stat -c '%U:%G:%a' "${backup_env}")" = "root:root:600" ] \
  || fail "root-only restore configuration must be root:root 0600"
[ -f "${verifier}" ] || fail "installed verifier is missing"
[ "$(stat -c '%U:%G:%a' "${verifier}")" = "root:root:700" ] \
  || fail "installed verifier must be root:root 0700"
[ -x /usr/sbin/runuser ] || fail "runuser is required"
[ -x /usr/bin/tee ] || fail "tee is required"
[ -x /usr/bin/mysql ] || fail "mysql is required"
[ -f "${root_mysql_cnf}" ] || fail "root MySQL client configuration is missing"
[ "$(stat -c '%U:%G:%a' "${root_mysql_cnf}")" = "root:root:600" ] \
  || fail "root MySQL client configuration must be root:root 0600"
[ -d "${backup_root}" ] || fail "fixed backup root is missing"
[ ! -L "${backup_root}" ] || fail "fixed backup root must not be a symlink"

set -a
. "${backup_env}"
set +a
: "${DB_ADMIN_PASSWORD:?set DB_ADMIN_PASSWORD in the root-only restore configuration}"
DB_ADMIN_HOST="${DB_ADMIN_HOST:-127.0.0.1}"
DB_ADMIN_PORT="${DB_ADMIN_PORT:-3306}"
DB_ADMIN_USER="${DB_ADMIN_USER:-paper_mes_restore}"
SOURCE_DB_NAME="${SOURCE_DB_NAME:-paper_processing}"

backup_dir="${backup_root}/${backup_id}"
[ -d "${backup_dir}" ] || fail "backup directory is missing"
[ ! -L "${backup_dir}" ] || fail "backup directory must not be a symlink"
for required_file in "${SOURCE_DB_NAME}.sql.gz" SHA256SUMS; do
  [ -f "${backup_dir}/${required_file}" ] || fail "required backup file is missing: ${required_file}"
  [ ! -L "${backup_dir}/${required_file}" ] || fail "backup files must not be symlinks"
done
status_file="${backup_dir}/restore-check.txt"
[ ! -L "${status_file}" ] || fail "restore status file must not be a symlink"

original_trust="$(/usr/bin/mysql --defaults-extra-file="${root_mysql_cnf}" -N -B \
  -e 'SELECT @@GLOBAL.log_bin_trust_function_creators')"
[[ "${original_trust}" =~ ^[01]$ ]] || fail "invalid global binlog trust value"

cleanup_wrapper() {
  local exit_code=$?
  trap - EXIT
  set +e
  /usr/bin/mysql --defaults-extra-file="${root_mysql_cnf}" \
    -e "SET GLOBAL log_bin_trust_function_creators=${original_trust}"
  local restore_code=$?
  rm -f "${report_tmp:-}"
  if [ "${exit_code}" -eq 0 ] && [ "${restore_code}" -ne 0 ]; then
    echo "failed to restore log_bin_trust_function_creators" >&2
    exit_code=${restore_code}
  fi
  exit "${exit_code}"
}

report_tmp="$(mktemp /run/paper-mes-restore-report.XXXXXX)"
trap cleanup_wrapper EXIT
/usr/bin/mysql --defaults-extra-file="${root_mysql_cnf}" \
  -e 'SET GLOBAL log_bin_trust_function_creators=1'
/usr/bin/env -i \
  PATH=/usr/bin:/bin \
  BACKUP_ENV_FILE=/dev/null \
  BACKUP_ROOT="${backup_root}" \
  BACKUP_DIR="${backup_dir}" \
  SOURCE_DB_NAME="${SOURCE_DB_NAME}" \
  RESTORE_DB_NAME="${restore_db}" \
  DB_ADMIN_HOST="${DB_ADMIN_HOST}" \
  DB_ADMIN_PORT="${DB_ADMIN_PORT}" \
  DB_ADMIN_USER="${DB_ADMIN_USER}" \
  DB_ADMIN_PASSWORD="${DB_ADMIN_PASSWORD}" \
  DROP_AFTER_VERIFY=true \
  VERIFY_LOCK_FILE=/run/lock/paper-mes-backup-verify.lock \
  VERIFY_REPORT_FILE="${report_tmp}" \
  /usr/bin/bash "${verifier}"

[ -s "${report_tmp}" ] || fail "verification report was not generated"
/usr/sbin/runuser -u "${service_user}" -- \
  /usr/bin/tee "${status_file}" >/dev/null < "${report_tmp}"
