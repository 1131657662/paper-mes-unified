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
service=paper-mes-test.service
backup_root=/opt/paper-mes-test/backups

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

backup_runtime() {
  local release_time backup_dir
  release_time="$(date -u +%Y%m%d-%H%M%S)"
  backup_dir="${backup_root}/pre-${deploy_sha:0:7}-${release_time}"
  install -d -m 750 "${backup_dir}"
  cp -a "${app_root}/paper-mes.jar" "${backup_dir}/paper-mes.jar"
  cp -a "${env_file}" "${backup_dir}/paper-mes-test.env"
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

  local env_tmp
  env_tmp="$(mktemp "${env_file}.XXXXXX")"
  awk -v sha="${deploy_sha}" '
    BEGIN { updated = 0 }
    /^[[:space:]]*PAPER_MES_GIT_SHA=/ {
      if (!updated) { print "PAPER_MES_GIT_SHA=" sha; updated = 1 }
      next
    }
    { print }
    END { if (!updated) print "PAPER_MES_GIT_SHA=" sha }
  ' "${env_file}" > "${env_tmp}"
  chown root:root "${env_tmp}"
  chmod 600 "${env_tmp}"
  mv -f "${env_tmp}" "${env_file}"
}

restart_and_check() {
  systemctl restart "${service}"
  local healthy=0
  for _ in $(seq 1 30); do
    if curl --fail --silent --show-error --max-time 5 \
      http://127.0.0.1:8082/actuator/health | grep -q '"status":"UP"'; then
      healthy=1
      break
    fi
    sleep 2
  done
  [ "${healthy}" = 1 ] || {
    systemctl status "${service}" --no-pager >&2 || true
    journalctl -u "${service}" -n 120 --no-pager >&2 || true
    fail "test backend health check failed"
  }
}

publish_frontend() {
  local release_time release_id
  release_time="$(date -u +%Y%m%d-%H%M%S)"
  release_id="${deploy_sha:0:7}-${release_time}"
  FRONTEND_ROOT="${frontend_root}" KEEP_RELEASES=3 MIN_RETENTION_HOURS=72 \
    bash "${source_root}/deploy/publish-paper-mes-frontend.example.sh" \
    publish "${source_root}/frontend/dist" "${release_id}"
  [ "$(readlink -f "${frontend_root}/dist")" = "${frontend_root}/releases/${release_id}" ] \
    || fail "frontend release activation could not be verified"
}

require_root
checkout_ci_commit
backup_runtime
build_artifacts
update_runtime
restart_and_check
publish_frontend
[ "$(git -C "${source_root}" rev-parse HEAD)" = "${deploy_sha}" ]
[ "$(grep -E '^PAPER_MES_GIT_SHA=' "${env_file}")" = "PAPER_MES_GIT_SHA=${deploy_sha}" ]
systemctl is-active --quiet "${service}"
echo "MES test deployment passed: ${deploy_sha}"
