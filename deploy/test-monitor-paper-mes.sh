#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
monitor_script="${script_dir}/monitor-paper-mes.example.sh"
temp_dir="$(mktemp -d)"
server_pid=""

cleanup() {
  [ -z "${server_pid}" ] || kill "${server_pid}" 2>/dev/null || true
  rm -rf "${temp_dir}"
}
trap cleanup EXIT

mkdir -p "${temp_dir}/backups/20260715-000000"
touch "${temp_dir}/backups/20260715-000000/SHA256SUMS"

python3 -c 'from http.server import BaseHTTPRequestHandler, HTTPServer
class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(b"{\"status\":\"UP\"}")
    def do_POST(self):
        self.rfile.read(int(self.headers.get("Content-Length", "0")))
        self.send_response(500 if self.path == "/fail" else 200)
        self.end_headers()
    def log_message(self, *_):
        pass
HTTPServer(("127.0.0.1", 18991), Handler).serve_forever()' &
server_pid="$!"
sleep 0.5

run_monitor() {
  MONITOR_ENV_FILE=/dev/null \
  BACKUP_ROOT="${temp_dir}/backups" \
  STATE_FILE="${temp_dir}/state" \
  MAX_BACKUP_AGE_HOURS=48 \
  MIN_BACKUP_FREE_MB=1 \
  HTTP_TIMEOUT_SECONDS=2 \
  HEALTH_URL="$1" \
  ALERT_WEBHOOK_URL="$2" \
  "${monitor_script}"
}

assert_state() {
  local expected="$1"
  local actual
  actual="$(cat "${temp_dir}/state")"
  [ "${actual}" = "${expected}" ] || {
    echo "expected state ${expected}, got ${actual}" >&2
    exit 1
  }
}

run_monitor http://127.0.0.1:18991/health ''
assert_state UP
! run_monitor http://127.0.0.1:1/health http://127.0.0.1:18991/fail >/dev/null 2>&1
assert_state ALERT_PENDING
! run_monitor http://127.0.0.1:1/health http://127.0.0.1:18991/ok >/dev/null 2>&1
assert_state FAILED
! run_monitor http://127.0.0.1:18991/health http://127.0.0.1:18991/fail >/dev/null 2>&1
assert_state RECOVERY_PENDING
run_monitor http://127.0.0.1:18991/health http://127.0.0.1:18991/ok
assert_state UP

echo "monitor state transition test passed"
