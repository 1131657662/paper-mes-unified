#!/usr/bin/env bash

backup_runtime_artifacts() {
  local stamp
  stamp=$(date -u +%Y%m%d-%H%M%S)
  release_id="${target_sha:0:7}-${stamp}-$$"
  release_dir="${RELEASE_ROOT}/deploy-${stamp}-${target_sha:0:12}-$$"
  install -d -o root -g root -m 0700 \
    "${release_dir}/old/frontend-dist" "${release_dir}/new/frontend-dist"
  old_env="${release_dir}/old/paper-mes.env"
  old_jar="${release_dir}/old/paper-mes.jar"
  old_frontend="${release_dir}/old/frontend-dist"
  old_frontend_link="$(readlink "${FRONTEND_ROOT}/dist" || true)"
  cp -p "${APP}" "${old_jar}"
  cp -p "${ENV_FILE}" "${old_env}"
  rsync -a "${FRONTEND_ROOT}/dist/" "${old_frontend}/"
}

backup_runtime_templates() {
  local template_name template_path
  template_backup_dir="${release_dir}/old/runtime"
  install -d -o root -g root -m 0700 "${template_backup_dir}"
  for template_name in service preflight migration-guard migration-runner migration-lock migration-state rollback deploy; do
    case "${template_name}" in
      service) template_path=/etc/systemd/system/paper-mes.service ;;
      preflight) template_path=/usr/local/bin/preflight-paper-mes-release ;;
      migration-guard) template_path=/usr/local/bin/verify-paper-mes-migration-state ;;
      migration-runner) template_path=/usr/local/bin/apply-paper-mes-migrations ;;
      migration-lock) template_path=/usr/local/bin/migration-lock-support.sh ;;
      migration-state) template_path=/usr/local/bin/migration-state-support.sh ;;
      rollback) template_path=/usr/local/sbin/paper-mes-runtime-rollback.sh ;;
      deploy) template_path=/usr/local/sbin/deploy-paper-mes.sh ;;
    esac
    [ -f "${template_path}" ] || fail "installed runtime template is missing: ${template_path}"
    cp -p "${template_path}" "${template_backup_dir}/${template_name}"
  done
}

backup_runtime() {
  backup_runtime_artifacts
  backup_runtime_templates
  printf 'target_sha=%s\ncreated_at=%s\n' "${target_sha}" "$(date --iso-8601=seconds)" \
    > "${release_dir}/RELEASE_INFO"
}

restore_source() {
  if [ "${source_updated}" = 1 ] && [ "${schema_migrated}" = 0 ]; then
    git -C "${SOURCE}" reset --hard "${previous_source_sha}" >/dev/null 2>&1 || true
    git -C "${SOURCE}" update-ref refs/remotes/origin/main "${previous_remote_sha}" >/dev/null 2>&1 || true
  fi
}

restore_templates() {
  if [ "${templates_switched}" = 1 ] && [ "${schema_migrated}" = 0 ]; then
    install -o root -g root -m 0644 "${template_backup_dir}/service" /etc/systemd/system/paper-mes.service
    install -o root -g root -m 0755 "${template_backup_dir}/preflight" /usr/local/bin/preflight-paper-mes-release
    install -o root -g root -m 0755 "${template_backup_dir}/migration-guard" /usr/local/bin/verify-paper-mes-migration-state
    install -o root -g root -m 0755 "${template_backup_dir}/migration-runner" /usr/local/bin/apply-paper-mes-migrations
    install -o root -g root -m 0755 "${template_backup_dir}/migration-lock" /usr/local/bin/migration-lock-support.sh
    install -o root -g root -m 0755 "${template_backup_dir}/migration-state" /usr/local/bin/migration-state-support.sh
    install -o root -g root -m 0700 "${template_backup_dir}/rollback" /usr/local/sbin/paper-mes-runtime-rollback.sh
    install -o root -g root -m 0700 "${template_backup_dir}/deploy" /usr/local/sbin/deploy-paper-mes.sh
    systemctl daemon-reload >/dev/null 2>&1 || true
  fi
}

restore_frontend() {
  if [ "${frontend_switched}" = 1 ]; then
    rm -rf "${FRONTEND_ROOT}/dist"
    if [[ "${old_frontend_link}" == releases/* ]]; then
      ln -s "${old_frontend_link}" "${FRONTEND_ROOT}/dist"
    else
      cp -a "${old_frontend}" "${FRONTEND_ROOT}/dist"
    fi
  fi
}

restore_backend() {
  if [ "${jar_switched}" = 1 ]; then
    install -o paper-mes -g paper-mes -m 0640 "${old_jar}" "${APP}.rollback"
    mv -f "${APP}.rollback" "${APP}"
  fi
  if [ "${env_switched}" = 1 ]; then
    install -o root -g paper-mes -m 0640 "${old_env}" "${ENV_FILE}.rollback"
    mv -f "${ENV_FILE}.rollback" "${ENV_FILE}"
  fi
}

restore_runtime() {
  set +e
  [ -n "${release_dir}" ] || return 0
  restore_source
  restore_templates
  restore_frontend
  restore_backend
  if [ "${schema_migrated}" = 1 ]; then
    ensure_schema_env
  fi
  if [ "${jar_switched}" = 1 ] || [ "${env_switched}" = 1 ] || [ "${schema_migrated}" = 1 ]; then
    systemctl restart "${SERVICE}" >/dev/null 2>&1 || true
    wait_health || true
  fi
}
