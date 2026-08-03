#!/usr/bin/env bash
set -euo pipefail
umask 077

# This script is evidence tooling only. It never writes inventory rows and it
# never calls POST /api/inventory/ledger/opening. The operator freezes writes,
# captures the authenticated preview and checksum, runs preflight, calls the
# controlled opening endpoint separately, then runs postcheck.
MODE="${MODE:-preflight}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-paper_processing}"
DB_USER="${DB_USER:-paper_mes_app}"
DB_PASSWORD="${DB_PASSWORD:?set DB_PASSWORD before running cutover evidence checks}"
SWITCH_UUID="${SWITCH_UUID:?set SWITCH_UUID to the approved cutover batch UUID}"
OCCURRED_AT="${OCCURRED_AT:?set OCCURRED_AT to the approved cutover timestamp}"
CUTOVER_MANIFEST="${CUTOVER_MANIFEST:?set CUTOVER_MANIFEST to the approved cutover manifest}"
CUTOVER_MANIFEST_SHA256="${CUTOVER_MANIFEST_SHA256:?set CUTOVER_MANIFEST_SHA256 from the approval record}"
PREVIEW_RESPONSE_FILE="${PREVIEW_RESPONSE_FILE:-}"
PREVIEW_RESPONSE_SHA256="${PREVIEW_RESPONSE_SHA256:?set PREVIEW_RESPONSE_SHA256 after capturing the preview API response}"
OUTPUT_DIR="${OUTPUT_DIR:-./inventory-opening-evidence/${SWITCH_UUID}}"
PREFLIGHT_FILE="${OUTPUT_DIR}/preflight.tsv"
APPROVED_MANIFEST_FILE="${OUTPUT_DIR}/cutover-manifest.approved"
APPROVED_PREVIEW_FILE="${OUTPUT_DIR}/opening-preview-response.json"
EVIDENCE_MANIFEST="${OUTPUT_DIR}/SHA256SUMS"
MYSQL_BIN="${MYSQL_BIN:-mysql}"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
POLICY_SUPPORT="${SCRIPT_DIR}/inventory-opening-policy-support.sh"
EVIDENCE_SUPPORT="${SCRIPT_DIR}/inventory-opening-evidence-support.sh"
PREVIEW_VALIDATOR="${SCRIPT_DIR}/inventory-opening-preview-verify.mjs"

fail() { echo "inventory opening evidence check failed: $1" >&2; exit 1; }
[ -r "${POLICY_SUPPORT}" ] || fail "missing cutover policy support"
. "${POLICY_SUPPORT}"
[ -r "${EVIDENCE_SUPPORT}" ] || fail "missing cutover evidence support"
. "${EVIDENCE_SUPPORT}"
sql_escape() { printf "%s" "$1" | sed "s/'/''/g"; }
require_command() { command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"; }
for command_name in "${MYSQL_BIN}" awk basename cat chmod cmp cp cut date diff mkdir mktemp node rm sed sha256sum sort tail uniq; do
  require_command "${command_name}"
done

validate_cutover_inputs
mkdir -p -- "$OUTPUT_DIR"
chmod 700 "$OUTPUT_DIR"

mysql_cnf="$(mktemp)"
manifest_snapshot="$(mktemp)"
cleanup() { rm -f -- "$mysql_cnf" "$manifest_snapshot"; }
trap cleanup EXIT
snapshot_cutover_manifest
cat > "$mysql_cnf" <<EOF
[client]
host=${DB_HOST}
port=${DB_PORT}
user=${DB_USER}
password=${DB_PASSWORD}
default-character-set=utf8mb4
EOF
chmod 600 "$mysql_cnf"

mysql_query() {
  "$MYSQL_BIN" --defaults-extra-file="$mysql_cnf" --batch --raw --skip-column-names \
    "$DB_NAME" -e "$1"
}

if [[ "$MODE" == "preflight" ]]; then
  run_preflight
else
  run_postcheck
fi
