#!/usr/bin/env bash
set -Eeuo pipefail
umask 027

deploy_sha="${1:-}"
[[ "${deploy_sha}" =~ ^[0-9a-f]{40}$ ]] || {
  echo "usage: $0 <40-character commit sha>" >&2
  exit 2
}

source_root=/opt/paper-mes-test/source
frontend_root=/opt/paper-mes-test/frontend
app_root=/opt/paper-mes-test/app
env_file=/etc/paper-mes-test/paper-mes-test.env
restore_env_file=/etc/paper-mes-test/backup-restore.env
service=paper-mes-test.service
backup_root=/opt/paper-mes-test/backups
migration_env_file="${MIGRATION_ENV_FILE:-}"
backend_version=""
frontend_version=""
build_time=""
release_id=""
schema_version=""
previous_jar_backup=""
previous_env_backup=""

fail() {
  echo "MES test deployment failed: $1" >&2
  exit 1
}

require_root() {
  [ "$(id -u)" = 0 ] || fail "root privileges are required"
  [ -d "${source_root}/.git" ] || fail "test source repository is missing"
  [ -r "${env_file}" ] || fail "test environment file is missing"
}

checkout_ci_commit() {
  [ -z "$(git -C "${source_root}" status --porcelain --untracked-files=all)" ] \
    || fail "refusing deployment over a dirty test source tree"
  git -C "${source_root}" fetch --prune origin main
  [ "$(git -C "${source_root}" rev-parse origin/main)" = "${deploy_sha}" ] \
    || fail "origin/main does not match the CI-tested commit"
  git -C "${source_root}" checkout --quiet main
  git -C "${source_root}" pull --ff-only origin main
  [ "$(git -C "${source_root}" rev-parse HEAD)" = "${deploy_sha}" ] \
    || fail "test source checkout does not match the CI-tested commit"
}

prepare_runtime_scripts() {
  local script_path
  for script_path in \
    "${source_root}/deploy/backup-paper-mes.example.sh" \
    "${source_root}/deploy/verify-backup-restore.example.sh"; do
    [ -f "${script_path}" ] || fail "runtime backup script is missing: ${script_path}"
    chown root:paper-mes-test "${script_path}"
    chmod 0640 "${script_path}"
  done
}

install_restore_runtime() {
  [ -f "${restore_env_file}" ] || fail "root-only restore configuration is missing"
  [ "$(stat -c '%U:%G:%a' "${restore_env_file}")" = "root:root:600" ] \
    || fail "root-only restore configuration must be root:root 0600"
  install -o root -g root -m 0700 \
    "${source_root}/deploy/verify-backup-restore.example.sh" \
    /usr/local/bin/verify-paper-mes-test-backup
  install -o root -g root -m 0700 \
    "${source_root}/deploy/verify-paper-mes-backup-root.example.sh" \
    /usr/local/sbin/verify-paper-mes-test-backup-root
  local sudoers_tmp
  sudoers_tmp="$(mktemp)"
  install -o root -g root -m 0440 \
    "${source_root}/deploy/paper-mes-test-backup-verify.sudoers.example" \
    "${sudoers_tmp}"
  if ! visudo -cf "${sudoers_tmp}"; then
    rm -f "${sudoers_tmp}"
    fail "backup verification sudoers validation failed"
  fi
  mv -f "${sudoers_tmp}" /etc/sudoers.d/paper-mes-test-backup-verify
}

prepare_release_metadata() {
  local release_time baseline_file
  release_time="$(date -u +%Y%m%d-%H%M%S)"
  baseline_file="${source_root}/sql/schema-baseline.version"
  [ -r "${baseline_file}" ] || fail "schema baseline version file is missing"
  schema_version="$(tr -d '[:space:]' < "${baseline_file}")"
  [[ "${schema_version}" =~ ^[0-9]+(\.[0-9]+)*$ ]] \
    || fail "schema baseline version is invalid: ${schema_version}"
  backend_version="${deploy_sha:0:7}"
  release_id="${backend_version}-${release_time}"
  frontend_version="${release_id}"
  build_time="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}

run_database_migrations() {
  local candidate migration_runner migration_guard route_guard
  if [ -z "${migration_env_file}" ]; then
    for candidate in /etc/paper-mes-test/migration.env /etc/paper-mes/migration.env; do
      if [ -r "${candidate}" ]; then
        migration_env_file="${candidate}"
        break
      fi
    done
  fi
  [ -r "${migration_env_file}" ] || fail "test migration environment file is missing"
  [ "$(stat -c '%U:%G:%a' "${migration_env_file}")" = "root:root:600" ] \
    || fail "test migration environment file must be root:root 0600"
  migration_runner="${source_root}/deploy/apply-paper-mes-migrations.example.sh"
  migration_guard="${source_root}/deploy/verify-paper-mes-migration-state.example.sh"
  route_guard="${source_root}/deploy/verify-paper-mes-process-route-schema.example.sh"
  [ -f "${migration_runner}" ] || fail "migration runner is missing"
  [ -f "${migration_guard}" ] || fail "migration state guard is missing"
  [ -f "${route_guard}" ] || fail "process route schema guard is missing"
  MIGRATION_ENV_FILE="${migration_env_file}" MIGRATION_DIR="${source_root}/sql" \
    bash "${migration_runner}"
  MIGRATION_ENV_FILE="${migration_env_file}" MIGRATION_DIR="${source_root}/sql" \
    bash "${migration_guard}"
  MIGRATION_ENV_FILE="${migration_env_file}" \
    bash "${route_guard}"
}

backup_runtime() {
  local release_time backup_dir
  release_time="$(date -u +%Y%m%d-%H%M%S)"
  backup_dir="${backup_root}/pre-${deploy_sha:0:7}-${release_time}"
  install -d -m 750 "${backup_dir}"
  cp -a "${app_root}/paper-mes.jar" "${backup_dir}/paper-mes.jar"
  cp -a "${env_file}" "${backup_dir}/paper-mes-test.env"
  previous_jar_backup="${backup_dir}/paper-mes.jar"
  previous_env_backup="${backup_dir}/paper-mes-test.env"
}

build_artifacts() {
  npm ci --prefix "${source_root}/frontend" --no-audit --no-fund
  npm run build --prefix "${source_root}/frontend"
  mvn -q -DskipTests package -f "${source_root}/pom.xml"
  local jar_path="${source_root}/target/paper-mes-0.0.1-SNAPSHOT.jar"
  [ -s "${jar_path}" ] || fail "backend jar was not produced"
}

update_runtime() {
  local jar_path="${source_root}/target/paper-mes-0.0.1-SNAPSHOT.jar"
  install -o paper-mes-test -g paper-mes-test -m 640 \
    "${jar_path}" "${app_root}/paper-mes.jar.next"
  mv -f "${app_root}/paper-mes.jar.next" "${app_root}/paper-mes.jar"

  update_runtime_metadata
}

render_runtime_env() {
  local output_file="$1"
  awk -v sha="${deploy_sha}" -v backend="${backend_version}" \
      -v frontend="${frontend_version}" -v built="${build_time}" \
      -v schema="${schema_version}" '
    BEGIN {
      FS = "="; OFS = "="
      values["PAPER_MES_GIT_SHA"] = sha
      values["PAPER_MES_BACKEND_VERSION"] = backend
      values["PAPER_MES_FRONTEND_VERSION"] = frontend
      values["PAPER_MES_BUILD_TIME"] = built
      values["PAPER_MES_EXPECTED_SCHEMA_VERSION"] = schema
      values["PAPER_MES_BACKUP_VERIFY_WRAPPER"] = "/usr/local/sbin/verify-paper-mes-test-backup-root"
    }
    {
      key = $1
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", key)
      if (key in values) {
        if (!seen[key]) print key, values[key]
        seen[key] = 1
        next
      }
      print
    }
    END {
      for (key in values) if (!seen[key]) print key, values[key]
    }
  ' "${env_file}" > "${output_file}"
}

update_runtime_metadata() {
  local env_tmp
  env_tmp="$(mktemp "${env_file}.XXXXXX")"
  render_runtime_env "${env_tmp}"
  chown root:root "${env_tmp}"
  chmod 600 "${env_tmp}"
  mv -f "${env_tmp}" "${env_file}"
}

report_service_failure() {
  systemctl status "${service}" --no-pager -l >&2 || true
  journalctl -u "${service}" -n 120 --no-pager >&2 || true
}

restore_previous_runtime() {
  [ -s "${previous_jar_backup}" ] || fail "previous test backend jar backup is missing"
  [ -s "${previous_env_backup}" ] || fail "previous test environment backup is missing"
  install -o paper-mes-test -g paper-mes-test -m 640 \
    "${previous_jar_backup}" "${app_root}/paper-mes.jar.rollback"
  mv -f "${app_root}/paper-mes.jar.rollback" "${app_root}/paper-mes.jar"
  install -o root -g root -m 600 \
    "${previous_env_backup}" "${env_file}.rollback"
  mv -f "${env_file}.rollback" "${env_file}"
  if ! systemctl restart "${service}"; then
    report_service_failure
    fail "new test backend failed and previous version could not be restored"
  fi
  systemctl is-active --quiet "${service}" \
    || fail "new test backend failed and previous version could not be restored"
}

restart_and_check() {
  if ! systemctl restart "${service}"; then
    report_service_failure
    restore_previous_runtime
    fail "test backend restart failed; previous version restored"
  fi
  local healthy=0
  for _ in $(seq 1 30); do
    if curl --fail --silent --show-error --max-time 5 \
      http://127.0.0.1:8082/actuator/health | grep -q '"status":"UP"'; then
      healthy=1
      break
    fi
    sleep 2
  done
  if [ "${healthy}" != 1 ]; then
    report_service_failure
    restore_previous_runtime
    fail "test backend health check failed; previous version restored"
  fi
}

publish_frontend() {
  FRONTEND_ROOT="${frontend_root}" KEEP_RELEASES=3 MIN_RETENTION_HOURS=72 \
    bash "${source_root}/deploy/publish-paper-mes-frontend.example.sh" \
    publish "${source_root}/frontend/dist" "${release_id}"
  [ "$(readlink -f "${frontend_root}/dist")" = "${frontend_root}/releases/${release_id}" ] \
    || fail "frontend release activation could not be verified"
}

require_root
checkout_ci_commit
prepare_runtime_scripts
install_restore_runtime
prepare_release_metadata
run_database_migrations
backup_runtime
build_artifacts
update_runtime
restart_and_check
publish_frontend
[ "$(git -C "${source_root}" rev-parse HEAD)" = "${deploy_sha}" ]
[ "$(grep -E '^PAPER_MES_GIT_SHA=' "${env_file}")" = "PAPER_MES_GIT_SHA=${deploy_sha}" ]
[ "$(grep -E '^PAPER_MES_BACKEND_VERSION=' "${env_file}")" = "PAPER_MES_BACKEND_VERSION=${backend_version}" ]
[ "$(grep -E '^PAPER_MES_FRONTEND_VERSION=' "${env_file}")" = "PAPER_MES_FRONTEND_VERSION=${frontend_version}" ]
[ "$(grep -E '^PAPER_MES_BUILD_TIME=' "${env_file}")" = "PAPER_MES_BUILD_TIME=${build_time}" ]
[ "$(grep -E '^PAPER_MES_EXPECTED_SCHEMA_VERSION=' "${env_file}")" = "PAPER_MES_EXPECTED_SCHEMA_VERSION=${schema_version}" ]
systemctl is-active --quiet "${service}"
echo "MES test deployment passed: ${deploy_sha}"
