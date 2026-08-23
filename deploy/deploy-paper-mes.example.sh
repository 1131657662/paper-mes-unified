#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
SOURCE=/opt/paper-mes/source
APP=/opt/paper-mes/app/paper-mes.jar
ENV_FILE=/etc/paper-mes/paper-mes.env
FRONTEND_ROOT=/opt/paper-mes/frontend
SERVICE=paper-mes
RELEASE_ROOT=/opt/paper-mes/releases
BACKUP_SCRIPT=${SOURCE}/deploy/backup-paper-mes.example.sh
ROLLBACK_HELPER=/usr/local/sbin/paper-mes-runtime-rollback.sh
LOCK=/run/deploy-paper-mes.lock
target_sha=${1:-}
[[ $# = 1 && ${target_sha} =~ ^[0-9a-f]{40}$ ]] || {
  echo "usage: $0 <40-character main commit sha>" >&2
  exit 2
}
release_dir=''
release_id=''
old_env=''
old_jar=''
old_frontend=''
old_frontend_link=''
template_backup_dir=''
previous_source_sha=''
previous_remote_sha=''
jar_switched=0
frontend_switched=0
env_switched=0
templates_switched=0
source_updated=0
schema_migrated=0
fail() { echo "Paper MES deployment failed: $1" >&2; exit 1; }
[ -r "${ROLLBACK_HELPER}" ] || fail "runtime rollback helper is missing"
# shellcheck disable=SC1090
. "${ROLLBACK_HELPER}"
require_commands() {
  local command_name
  for command_name in awk bash chmod chown cp curl date flock git grep install ln mktemp mv mvn nginx npm readlink rm rsync seq sleep systemctl tr; do
    command -v "${command_name}" >/dev/null 2>&1 || fail "required command not found: ${command_name}"
  done
}
check_source_state() {
  [ -d "${SOURCE}/.git" ] || fail "MES source repository not found"
  [ "$(git -C "${SOURCE}" branch --show-current)" = main ] || fail "MES source is not on main"
  [ -z "$(git -C "${SOURCE}" status --porcelain --untracked-files=all)" ] || fail "MES source working tree is not clean"
}
check_current_runtime() {
  systemctl is-active --quiet "${SERVICE}" || fail "MES service is not active"
  [ -s "${APP}" ] || fail "MES JAR is missing"
  [ -s "${FRONTEND_ROOT}/dist/index.html" ] || fail "MES frontend is missing"
  /usr/local/bin/preflight-paper-mes-release >/dev/null
}
wait_health() {
  local response
  for _ in $(seq 1 60); do
    response=$(curl --fail --silent --max-time 5 http://127.0.0.1:8081/actuator/health 2>/dev/null || true)
    grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' <<< "${response}" && return 0
    sleep 2
  done
  return 1
}
ensure_schema_env() {
  local output_file schema_version
  schema_version=$(tr -d '[:space:]' < "${SOURCE}/sql/schema-baseline.version")
  [[ ${schema_version} =~ ^[0-9]+(\.[0-9]+)*$ ]] || fail "invalid schema baseline version"
  output_file=$(mktemp "${ENV_FILE}.XXXXXX")
  awk -v schema="${schema_version}" 'BEGIN { FS="="; OFS="=" }
    { key=$1; sub(/^[[:space:]]*export[[:space:]]+/, "", key); gsub(/^[[:space:]]+|[[:space:]]+$/, "", key)
      if (key == "PAPER_MES_EXPECTED_SCHEMA_VERSION") { if (!seen) print key, schema; seen=1; next }
      print }
    END { if (!seen) print "PAPER_MES_EXPECTED_SCHEMA_VERSION", schema }' "${ENV_FILE}" > "${output_file}"
  chown root:paper-mes "${output_file}"; chmod 0640 "${output_file}"
  mv -f "${output_file}" "${ENV_FILE}"
}
render_runtime_env() {
  local output_file="$1" schema_version backend_version build_time
  schema_version=$(tr -d '[:space:]' < "${SOURCE}/sql/schema-baseline.version")
  [[ ${schema_version} =~ ^[0-9]+(\.[0-9]+)*$ ]] || fail "invalid schema baseline version"
  backend_version=${target_sha:0:7}
  build_time=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  awk -v sha="${target_sha}" -v backend="${backend_version}" \
    -v frontend="${release_id}" -v built="${build_time}" \
    -v schema="${schema_version}" '
    BEGIN { FS="="; OFS="="
      values["PAPER_MES_GIT_SHA"]=sha
      values["PAPER_MES_BACKEND_VERSION"]=backend
      values["PAPER_MES_FRONTEND_VERSION"]=frontend
      values["PAPER_MES_BUILD_TIME"]=built
      values["PAPER_MES_EXPECTED_SCHEMA_VERSION"]=schema }
    { key=$1; sub(/^[[:space:]]*export[[:space:]]+/, "", key); gsub(/^[[:space:]]+|[[:space:]]+$/, "", key)
      if (key in values) { if (!seen[key]) print key, values[key]; seen[key]=1; next }
      print }
    END { for (key in values) if (!seen[key]) print key, values[key] }
  ' "${ENV_FILE}" > "${output_file}"
}
install_runtime_templates() {
  templates_switched=1
  install -o root -g root -m 0700 "${SOURCE}/deploy/deploy-paper-mes.example.sh" /usr/local/sbin/deploy-paper-mes.sh
  install -o root -g root -m 0700 "${SOURCE}/deploy/paper-mes-runtime-rollback.example.sh" /usr/local/sbin/paper-mes-runtime-rollback.sh
  install -o root -g root -m 0644 "${SOURCE}/deploy/paper-mes.service.example" /etc/systemd/system/paper-mes.service
  install -o root -g root -m 0755 "${SOURCE}/deploy/preflight-paper-mes-release.example.sh" /usr/local/bin/preflight-paper-mes-release
  install -o root -g root -m 0755 "${SOURCE}/deploy/verify-paper-mes-migration-state.example.sh" /usr/local/bin/verify-paper-mes-migration-state
  install -o root -g root -m 0755 "${SOURCE}/deploy/apply-paper-mes-migrations.example.sh" /usr/local/bin/apply-paper-mes-migrations
  install -o root -g root -m 0755 "${SOURCE}/deploy/migration-lock-support.sh" /usr/local/bin/migration-lock-support.sh
  install -o root -g root -m 0755 "${SOURCE}/deploy/migration-state-support.sh" /usr/local/bin/migration-state-support.sh
  systemctl daemon-reload
}
build_release() {
  mvn -f "${SOURCE}/pom.xml" -B -DskipTests clean package
  npm --prefix "${SOURCE}/frontend" ci --no-audit --no-fund
  npm --prefix "${SOURCE}/frontend" run build
  [ -s "${SOURCE}/target/paper-mes-0.0.1-SNAPSHOT.jar" ] || fail "Maven build produced no JAR"
  [ -s "${SOURCE}/frontend/dist/index.html" ] || fail "frontend build produced no index.html"
  [ -z "$(git -C "${SOURCE}" status --porcelain --untracked-files=all)" ] || fail "build changed source files"
  cp -p "${SOURCE}/target/paper-mes-0.0.1-SNAPSHOT.jar" "${release_dir}/new/paper-mes.jar"
  rsync -a "${SOURCE}/frontend/dist/" "${release_dir}/new/frontend-dist/"
}

on_exit() {
  local status=$?
  trap - EXIT
  [ "${status}" = 0 ] || restore_runtime
  exit "${status}"
}

deploy() {
  exec 9>"${LOCK}"
  flock -n 9 || fail "another MES deployment is running"
  [ "$(id -u)" = 0 ] || fail "root privileges are required"
  require_commands
  check_source_state
  previous_source_sha="$(git -C "${SOURCE}" rev-parse HEAD)"
  previous_remote_sha="$(git -C "${SOURCE}" rev-parse refs/remotes/origin/main)"
  check_current_runtime
  [ -f "${BACKUP_SCRIPT}" ] || fail "MES backup script is missing"
  /usr/bin/bash "${BACKUP_SCRIPT}"
  git -C "${SOURCE}" fetch --prune origin main
  [ "$(git -C "${SOURCE}" rev-parse origin/main)" = "${target_sha}" ] || fail "target is not the current origin/main"
  backup_runtime
  git -C "${SOURCE}" merge --ff-only origin/main
  source_updated=1
  build_release
  install_runtime_templates
  . "${ROLLBACK_HELPER}"
  MIGRATION_ENV_FILE=/etc/paper-mes/migration.env \
    MIGRATION_DIR="${SOURCE}/sql" \
    /usr/local/bin/apply-paper-mes-migrations
  schema_migrated=1
  env_tmp=$(mktemp "${ENV_FILE}.XXXXXX")
  render_runtime_env "${env_tmp}"
  chown root:paper-mes "${env_tmp}"; chmod 0640 "${env_tmp}"; mv -f "${env_tmp}" "${ENV_FILE}"
  env_switched=1
  install -o paper-mes -g paper-mes -m 0640 "${release_dir}/new/paper-mes.jar" "${APP}.next"
  mv -f "${APP}.next" "${APP}"; jar_switched=1
  systemctl restart "${SERVICE}"; wait_health || fail "MES health check did not become UP"
  /usr/local/bin/preflight-paper-mes-release
  frontend_switched=1
  FRONTEND_ROOT="${FRONTEND_ROOT}" KEEP_RELEASES=3 MIN_RETENTION_HOURS=72 \
    bash "${SOURCE}/deploy/publish-paper-mes-frontend.example.sh" \
    publish "${release_dir}/new/frontend-dist" "${release_id}"
  nginx -t
  curl --fail --silent --show-error --max-time 15 -A 'Mozilla/5.0' \
    https://mes.nbsmzwl.cn/ | grep -Fq '<div id="root"></div>'
  /usr/local/sbin/verify-production-hardening.sh
  echo "Paper MES deployment completed: ${target_sha}"
}

trap on_exit EXIT
deploy
