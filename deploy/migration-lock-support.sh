#!/usr/bin/env bash

start_lock_session() {
  lock_owner_name="paper_mes_migration_owner_${BASHPID}_${RANDOM}"
  "${MYSQL_BIN}" --defaults-extra-file="${mysql_cnf}" "${DB_NAME}" \
    --batch --skip-column-names --raw -e "
SELECT GET_LOCK('$(sql_escape "${MIGRATION_LOCK_NAME}")', ${MIGRATION_LOCK_TIMEOUT_SECONDS});
SELECT GET_LOCK('$(sql_escape "${lock_owner_name}")', 0);
SELECT SLEEP(31536000);" >/dev/null 2>"${lock_session_error}" &
  lock_session_pid="$!"
}

lock_owner_check() {
  mysql_query --batch --skip-column-names -e "
SELECT COALESCE(
  IS_USED_LOCK('$(sql_escape "${MIGRATION_LOCK_NAME}")') =
  IS_USED_LOCK('$(sql_escape "${lock_owner_name}")'), 0)"
}

release_lock_session() {
  if [ -n "${lock_session_pid}" ] && kill -0 "${lock_session_pid}" 2>/dev/null; then
    kill "${lock_session_pid}" 2>/dev/null || true
    wait "${lock_session_pid}" 2>/dev/null || true
  fi
  lock_acquired=0
  lock_session_pid=""
  lock_owner_name=""
}

acquire_lock() {
  local deadline
  local result
  start_lock_session
  deadline=$((SECONDS + MIGRATION_LOCK_TIMEOUT_SECONDS + 5))
  while [ "${SECONDS}" -le "${deadline}" ]; do
    kill -0 "${lock_session_pid}" 2>/dev/null \
      || fail "migration lock session failed: $(tr '\n' ' ' < "${lock_session_error}")"
    result="$(lock_owner_check 2>/dev/null || true)"
    if [ "${result}" = "1" ]; then
      lock_acquired=1
      return
    fi
    sleep 0.1
  done
  fail "could not acquire migration lock ${MIGRATION_LOCK_NAME}"
}

assert_lock_owned() {
  local result
  kill -0 "${lock_session_pid}" 2>/dev/null \
    || fail "migration lock session ended unexpectedly"
  result="$(lock_owner_check 2>/dev/null || true)"
  [ "${result}" = "1" ] || fail "migration lock ownership was lost"
}
