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

start_server
run_monitor http://127.0.0.1:18992/
assert_state UP
[ ! -f "${temp_dir}/alerts" ]

kill "${server_pid}"
wait "${server_pid}" 2>/dev/null || true
server_pid=""
! run_monitor http://127.0.0.1:18992/ >/dev/null 2>&1
assert_state FAILED
[ "$(wc -l < "${temp_dir}/alerts")" -eq 1 ]
grep -F $'FAILED\tserver monitor detected 1 issue(s): Test API request failed\tserver\t服务器统一监控发现 1 项异常：Test API 请求失败' "${temp_dir}/alerts"

! run_monitor http://127.0.0.1:18992/ >/dev/null 2>&1
[ "$(wc -l < "${temp_dir}/alerts")" -eq 1 ]

start_server
run_monitor http://127.0.0.1:18992/
assert_state UP
[ "$(wc -l < "${temp_dir}/alerts")" -eq 2 ]
grep -F $'RECOVERED\tall server checks are healthy\tserver\t服务器全部检查项均已恢复正常' "${temp_dir}/alerts"

echo "server monitor state transition test passed"
