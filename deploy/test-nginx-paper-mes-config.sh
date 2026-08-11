#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
templates=(
  "${script_dir}/nginx-paper-mes.example.conf"
  "${script_dir}/nginx-paper-mes-https.example.conf"
)

fail() {
  echo "nginx template test failed: $1" >&2
  exit 1
}

assert_exact_line_once() {
  local template="$1"
  local directive="$2"
  local count

  count="$(awk -v expected="${directive}" '{ sub(/\r$/, "", $0) } $0 == expected { count++ } END { print count + 0 }' "${template}")"
  [ "${count}" = "1" ] || fail "${template} must contain exactly one: ${directive}"
}

for template in "${templates[@]}"; do
  awk '
    { sub(/\r$/, "", $0) }
    $0 == "server {" { in_server = 1; token_count = 0; server_count++; next }
    in_server && $0 == "    server_tokens off;" { token_count++ }
    in_server && $0 == "}" {
      if (token_count != 1) exit 1
      in_server = 0
    }
    END { if (in_server || server_count == 0) exit 1 }
  ' "${template}" || fail "${template} must disable server tokens in every server block"

  actuator_count="$(awk '{ sub(/\r$/, "", $0) } $0 == "    location ^~ /actuator {" { count++ } END { print count + 0 }' "${template}")"
  [ "${actuator_count}" = "1" ] || fail "${template} must block /actuator exactly once"

  actuator_line="$(awk '{ sub(/\r$/, "", $0) } $0 == "    location ^~ /actuator {" { print NR }' "${template}")"
  fallback_line="$(awk '{ sub(/\r$/, "", $0) } $0 == "    location / {" { print NR }' "${template}")"
  [ -n "${fallback_line}" ] || fail "${template} has no SPA fallback"
  [ "${actuator_line}" -lt "${fallback_line}" ] || fail "${template} blocks /actuator after SPA fallback"

  awk '
    { sub(/\r$/, "", $0) }
    $0 == "    location ^~ /actuator {" { in_actuator = 1; next }
    in_actuator && $0 == "        return 404;" { blocked = 1 }
    in_actuator && $0 == "    }" { exit blocked ? 0 : 1 }
    END { if (!blocked) exit 1 }
  ' "${template}" || fail "${template} must return 404 for /actuator"
done

https_template="${script_dir}/nginx-paper-mes-https.example.conf"
assert_exact_line_once "${https_template}" "    ssl_session_timeout 1d;"
assert_exact_line_once "${https_template}" "    ssl_session_tickets off;"

echo "nginx template tests passed"
