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

for template in "${templates[@]}"; do
  actuator_count="$(grep -c '^    location \^~ /actuator {$' "${template}" || true)"
  [ "${actuator_count}" = "1" ] || fail "${template} must block /actuator exactly once"

  actuator_line="$(awk '$0 == "    location ^~ /actuator {" { print NR }' "${template}")"
  fallback_line="$(awk '$0 == "    location / {" { print NR }' "${template}")"
  [ -n "${fallback_line}" ] || fail "${template} has no SPA fallback"
  [ "${actuator_line}" -lt "${fallback_line}" ] || fail "${template} blocks /actuator after SPA fallback"

  awk '
    $0 == "    location ^~ /actuator {" { in_actuator = 1; next }
    in_actuator && $0 == "        return 404;" { blocked = 1 }
    in_actuator && $0 == "    }" { exit blocked ? 0 : 1 }
    END { if (!blocked) exit 1 }
  ' "${template}" || fail "${template} must return 404 for /actuator"
done

echo "nginx template tests passed"
