#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
notifier="${script_dir}/notify-server-monitor-internal.example.sh"
temp_dir="$(mktemp -d)"
trap 'rm -rf "${temp_dir}"' EXIT

mkdir -p "${temp_dir}/bin"
printf '%s\n' '#!/usr/bin/env bash' \
  'printf "%s\t%s\t%s\t%s\n" "$1" "$2" "$3" "$4" >> "${ALERT_CALLS}"' \
  > "${temp_dir}/bin/alert"
chmod 700 "${temp_dir}/bin/alert"
printf 'ALERT_EMAIL_COMMAND=%s\nINTERNAL_FAILURE_REMINDER_HOURS=2\n' \
  "${temp_dir}/bin/alert" > "${temp_dir}/monitor.env"

run_notifier() {
  ALERT_CALLS="${temp_dir}/alerts" MONITOR_ENV_FILE="${temp_dir}/monitor.env" \
    INTERNAL_FAILURE_STATE_FILE="${temp_dir}/internal.state" \
    bash "${notifier}" "$@"
}

run_notifier failed server-monitor.service
[ "$(wc -l < "${temp_dir}/alerts")" -eq 1 ]
grep -F $'CRITICAL\tserver monitor unit server-monitor.service failed; inspect its systemd journal\tmonitor-internal' \
  "${temp_dir}/alerts"

run_notifier failed server-monitor.service
[ "$(wc -l < "${temp_dir}/alerts")" -eq 1 ]

sed -i 's/^last_alert_epoch=.*/last_alert_epoch=0/' "${temp_dir}/internal.state"
run_notifier failed server-monitor.service
[ "$(wc -l < "${temp_dir}/alerts")" -eq 2 ]

run_notifier recovered
[ "$(wc -l < "${temp_dir}/alerts")" -eq 3 ]
grep -F $'RECOVERED\tserver monitor execution recovered\tmonitor-internal' "${temp_dir}/alerts"
[ ! -e "${temp_dir}/internal.state" ]

! run_notifier failed $'bad\nunit' >/dev/null 2>&1
echo "server monitor internal failure notifier test passed"
