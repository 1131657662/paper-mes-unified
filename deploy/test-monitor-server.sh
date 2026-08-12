#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
monitor_script="${script_dir}/monitor-server.example.sh"
temp_dir="$(mktemp -d)"
server_pid=""

cleanup() {
  [ -z "${server_pid}" ] || kill "${server_pid}" 2>/dev/null || true
  rm -rf "${temp_dir}"
}
trap cleanup EXIT

mkdir -p "${temp_dir}/bin" "${temp_dir}/lib" "${temp_dir}/backup/20260812-000000"
cp "${script_dir}/server-monitor-checks.example.sh" "${temp_dir}/lib/server-monitor-checks.sh"
cp "${script_dir}/server-monitor-state.example.sh" "${temp_dir}/lib/server-monitor-state.sh"
cp "${script_dir}/server-monitor-heartbeat.example.sh" "${temp_dir}/lib/server-monitor-heartbeat.sh"
touch "${temp_dir}/backup/20260812-000000/SHA256SUMS" "${temp_dir}/certificate.pem"
printf 'Production hardening check passed.\n' > "${temp_dir}/hardening.log"
printf 'status=SUCCESS\ncompleted_at=%s\n' "$(date --iso-8601=seconds)" > "${temp_dir}/remote.state"

write_mock() {
  printf '%s\n' '#!/usr/bin/env bash' "$2" > "${temp_dir}/bin/$1"
  chmod 700 "${temp_dir}/bin/$1"
}

write_mock systemctl 'exit 0'
write_mock docker 'printf "%s\n" "running healthy"'
write_mock mysqladmin 'exit 0'
write_mock openssl 'exit 0'
write_mock timedatectl 'printf "%s\n" yes'
write_mock getconf 'printf "%s\n" 4'
write_mock df 'if [[ "$*" == *-Pi* ]]; then printf "Filesystem Inodes IUsed IFree IUse%% Mounted on\nmock 100 10 90 10%% /\n"; else printf "Filesystem 1024-blocks Used Available Capacity Mounted on\nmock 100 10 90 10%% /\n"; fi'
write_mock alert 'printf "%s\t%s\t%s\t%s\n" "$1" "$2" "$3" "$4" >> "${ALERT_CALLS}"'
write_mock curl 'if [[ "$*" == *--config* && "$*" != *--data-binary* ]]; then printf "ping\n" >> "${HEARTBEAT_CALLS}"; exit 0; fi; exec /usr/bin/curl "$@"'

start_server() {
  python3 -c 'from http.server import BaseHTTPRequestHandler, HTTPServer
class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200); self.end_headers(); self.wfile.write(b"{\"status\":\"UP\"}")
    def log_message(self, *_): pass
HTTPServer(("127.0.0.1", 18992), Handler).serve_forever()' &
  server_pid="$!"
  sleep 0.5
}

run_monitor() {
  PATH="${temp_dir}/bin:/usr/bin:/bin" MONITOR_ENV_FILE=/dev/null \
  MONITOR_LIB_DIR="${temp_dir}/lib" STATE_FILE="${temp_dir}/monitor.state" \
  ALERT_EMAIL_COMMAND="${temp_dir}/bin/alert" ALERT_CALLS="${temp_dir}/alerts" \
  INTERNAL_FAILURE_COMMAND= HEARTBEAT_CALLS="${temp_dir}/heartbeats" \
  SYSTEMD_UNITS=mock.service SYSTEMD_TIMERS=mock.timer DOCKER_CONTAINERS=mock \
  HTTP_PROBES="Test API|json-up|$1|200" \
  CERTIFICATES="Test|${temp_dir}/certificate.pem" \
  BACKUP_ROOTS="Test|${temp_dir}/backup" \
  REMOTE_STATUS_FILES="Test|${temp_dir}/remote.state" \
  FRESH_CHECK_FILES="Test|${temp_dir}/hardening.log|Production hardening check passed." \
  MAX_LOAD_PER_CPU=100 bash "${monitor_script}"
}

assert_state() {
  grep -Fx "status=$1" "${temp_dir}/monitor.state"
}

state_value() {
  sed -n "s/^$1=//p" "${temp_dir}/monitor.state"
}

start_server
run_monitor http://127.0.0.1:18992/
assert_state UP
[ ! -f "${temp_dir}/alerts" ]
grep -Fx 'version=2' "${temp_dir}/monitor.state"
first_success="$(state_value last_success_epoch)"
[ "$(state_value last_run_epoch)" = "${first_success}" ]
[ "$(state_value last_alert_epoch)" = 0 ]

kill "${server_pid}"
wait "${server_pid}" 2>/dev/null || true
server_pid=""
set +e
run_monitor http://127.0.0.1:18992/ >/dev/null 2>&1
failure_exit=$?
set -e
[ "${failure_exit}" -eq 3 ]
assert_state FAILED
[ "$(wc -l < "${temp_dir}/alerts")" -eq 1 ]
failure_run="$(state_value last_run_epoch)"
[ "$(state_value last_success_epoch)" = "${first_success}" ]
first_alert="$(state_value last_alert_epoch)"
[[ "${first_alert}" =~ ^[1-9][0-9]*$ ]]
grep -F $'FAILED\tserver monitor detected 1 issue(s): Test API request failed\tserver\t服务器统一监控发现 1 项异常：Test API 请求失败' "${temp_dir}/alerts"

! run_monitor http://127.0.0.1:18992/ >/dev/null 2>&1
[ "$(wc -l < "${temp_dir}/alerts")" -eq 1 ]
[ "$(state_value last_success_epoch)" = "${first_success}" ]
[ "$(state_value last_alert_epoch)" = "${first_alert}" ]
(( $(state_value last_run_epoch) >= failure_run ))

# The same fault is not resent within two hours, then is resent after two hours.
fingerprint="$(state_value fingerprint)"
one_hour_ago=$(( $(date +%s) - 3600 ))
sed -i "s/^last_alert_epoch=.*/last_alert_epoch=${one_hour_ago}/" \
  "${temp_dir}/monitor.state"
! run_monitor http://127.0.0.1:18992/ >/dev/null 2>&1
[ "$(wc -l < "${temp_dir}/alerts")" -eq 1 ]

three_hours_ago=$(( $(date +%s) - 10800 ))
sed -i "s/^last_alert_epoch=.*/last_alert_epoch=${three_hours_ago}/" \
  "${temp_dir}/monitor.state"
! run_monitor http://127.0.0.1:18992/ >/dev/null 2>&1
[ "$(wc -l < "${temp_dir}/alerts")" -eq 2 ]

# A version 1 state is migrated without inventing prior successful-run data.
printf 'status=FAILED\nfingerprint=%s\nlast_alert_epoch=0\n' "${fingerprint}" \
  > "${temp_dir}/monitor.state"
! run_monitor http://127.0.0.1:18992/ >/dev/null 2>&1
[ "$(wc -l < "${temp_dir}/alerts")" -eq 3 ]
grep -Fx 'version=2' "${temp_dir}/monitor.state"
[ "$(state_value last_success_epoch)" = 0 ]

# A completed run pings the external dead man's switch.
! DEAD_MAN_SWITCH_URL=https://hc.example/ping run_monitor \
  http://127.0.0.1:18992/ >/dev/null 2>&1
[ "$(wc -l < "${temp_dir}/heartbeats")" -eq 1 ]

# Newlines are rejected before curl can parse the secret URL as configuration.
! DEAD_MAN_SWITCH_URL=$'https://hc.example/ping\noutput = "/tmp/injected"' \
  run_monitor http://127.0.0.1:18992/ >/dev/null 2>&1
[ "$(wc -l < "${temp_dir}/heartbeats")" -eq 1 ]

start_server
run_monitor http://127.0.0.1:18992/
assert_state UP
[ "$(wc -l < "${temp_dir}/alerts")" -eq 4 ]
grep -F $'RECOVERED\tall server checks are healthy\tserver\t服务器全部检查项均已恢复正常' "${temp_dir}/alerts"
[ "$(state_value last_run_epoch)" = "$(state_value last_success_epoch)" ]
[ "$(state_value last_alert_epoch)" = "$(state_value last_success_epoch)" ]

grep -Fq 'REMINDER_HOURS="${REMINDER_HOURS:-2}"' "${monitor_script}"

echo "server monitor state transition test passed"
