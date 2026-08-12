#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

RCLONE_CONFIG="${RCLONE_CONFIG:-/etc/rclone/paper-mes.conf}"
RCLONE_REMOTE="${RCLONE_REMOTE:-paper_mes_archive}"
RCLONE_PATHS="${RCLONE_PATHS:-paper-mes business-projects}"
OFFSITE_RETENTION_APPLY="${OFFSITE_RETENTION_APPLY:-false}"
RETENTION_NOW="${RETENTION_NOW:-now}"
DAILY_RETENTION_YEARS="${DAILY_RETENTION_YEARS:-3}"
WEEKLY_RETENTION_YEARS="${WEEKLY_RETENTION_YEARS:-5}"
MAX_DELETE_COUNT="${MAX_DELETE_COUNT:-50}"
STATE_DIR="${STATE_DIR:-/var/lib/paper-mes}"
PLAN_FILE="${PLAN_FILE:-${STATE_DIR}/offsite-retention-plan.tsv}"
LOCK_FILE="${LOCK_FILE:-/run/paper-mes-offsite-retention.lock}"

fail() {
  echo "$1" >&2
  exit 1
}

require_positive_integer() {
  [[ "$2" =~ ^[1-9][0-9]*$ ]] || fail "$1 must be a positive integer"
}

validate_config() {
  [[ "$RCLONE_REMOTE" =~ ^[A-Za-z0-9._-]+$ ]] || fail "invalid RCLONE_REMOTE"
  [[ "$OFFSITE_RETENTION_APPLY" =~ ^(true|false)$ ]] || fail "invalid OFFSITE_RETENTION_APPLY"
  require_positive_integer DAILY_RETENTION_YEARS "$DAILY_RETENTION_YEARS"
  require_positive_integer WEEKLY_RETENTION_YEARS "$WEEKLY_RETENTION_YEARS"
  require_positive_integer MAX_DELETE_COUNT "$MAX_DELETE_COUNT"
  (( WEEKLY_RETENTION_YEARS > DAILY_RETENTION_YEARS )) || fail "weekly retention must exceed daily retention"
  command -v rclone >/dev/null 2>&1 || fail "required command not found: rclone"
  command -v flock >/dev/null 2>&1 || fail "required command not found: flock"
}

record_plan() {
  printf '%s\t%s\t%s\t%s\n' "$1" "$current_path" "$2" "$3" >> "$plan_temp"
}

load_backup_ids() {
  local raw id listing
  backup_ids=()
  listing="$(rclone lsf "${RCLONE_REMOTE}:${current_path}" --dirs-only --max-depth 1)" || \
    fail "cannot list remote backups in ${current_path}"
  while IFS= read -r raw; do
    id="${raw%/}"
    if [[ "$id" =~ ^[0-9]{8}-[0-9]{6}$ ]]; then
      backup_ids+=("$id")
    elif [ -n "$id" ]; then
      record_plan SKIP "$id" unexpected-name
    fi
  done < <(LC_ALL=C sort <<< "$listing")
  ((${#backup_ids[@]} > 0)) || fail "no valid backups found in ${current_path}"
}

select_periodic_backups() {
  local id key
  declare -gA weekly_keep=() monthly_keep=()
  for id in "${backup_ids[@]}"; do
    if [[ "$id" < "$weekly_cutoff" ]]; then
      key="${id:0:6}"
      monthly_keep["$key"]="$id"
    elif [[ "$id" < "$daily_cutoff" ]]; then
      key="$(date -d "${id:0:4}-${id:4:2}-${id:6:2}" +%G-W%V)"
      weekly_keep["$key"]="$id"
    fi
  done
}

classify_backup() {
  local id="$1" key
  if [[ "$id" > "$daily_cutoff" || "$id" == "$daily_cutoff" ]]; then
    record_plan KEEP "$id" daily
  elif [[ "$id" > "$weekly_cutoff" || "$id" == "$weekly_cutoff" ]]; then
    key="$(date -d "${id:0:4}-${id:4:2}-${id:6:2}" +%G-W%V)"
    [ "${weekly_keep[$key]}" = "$id" ] && record_plan KEEP "$id" weekly || queue_delete "$id" weekly
  else
    key="${id:0:6}"
    [ "${monthly_keep[$key]}" = "$id" ] && record_plan KEEP "$id" monthly-permanent || queue_delete "$id" monthly
  fi
}

queue_delete() {
  local id="$1"
  [ "$id" != "$newest_id" ] || fail "refusing to delete newest backup"
  delete_targets+=("${current_path}/${id}")
  record_plan DELETE "$id" "$2"
}

plan_path() {
  local id
  current_path="$1"
  [[ "$current_path" =~ ^[A-Za-z0-9._-]+$ ]] || fail "invalid remote path"
  load_backup_ids
  newest_id="${backup_ids[${#backup_ids[@]}-1]}"
  select_periodic_backups
  for id in "${backup_ids[@]}"; do classify_backup "$id"; done
}

apply_plan() {
  local target
  [ "$OFFSITE_RETENTION_APPLY" = true ] || return 0
  ((${#delete_targets[@]} <= MAX_DELETE_COUNT)) || fail "delete count exceeds safety limit"
  for target in "${delete_targets[@]}"; do
    [[ "$target" =~ ^[A-Za-z0-9._-]+/[0-9]{8}-[0-9]{6}$ ]] || fail "unsafe delete target"
    rclone purge "${RCLONE_REMOTE}:${target}" --b2-hard-delete
  done
}

validate_config
export RCLONE_CONFIG
install -d -m 0750 "$STATE_DIR"
exec 9>"$LOCK_FILE"
flock -n 9 || fail "another off-site retention task is running"
daily_cutoff="$(date -d "${RETENTION_NOW} - ${DAILY_RETENTION_YEARS} years" +%Y%m%d-000000)"
weekly_cutoff="$(date -d "${RETENTION_NOW} - ${WEEKLY_RETENTION_YEARS} years" +%Y%m%d-000000)"
plan_temp="$(mktemp "${STATE_DIR}/.offsite-retention-plan.XXXXXX")"
trap 'rm -f "${plan_temp:-}"' EXIT
printf 'action\tpath\tbackup_id\ttier\n' > "$plan_temp"
delete_targets=()
for current_path in $RCLONE_PATHS; do plan_path "$current_path"; done
install -m 0600 "$plan_temp" "$PLAN_FILE"
apply_plan
printf 'off-site retention completed: mode=%s delete_count=%s plan=%s\n' \
  "$OFFSITE_RETENTION_APPLY" "${#delete_targets[@]}" "$PLAN_FILE"
