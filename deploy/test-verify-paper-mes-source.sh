#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
verify_script="${script_dir}/verify-paper-mes-source.example.sh"
temp_dir="$(mktemp -d)"

cleanup() { rm -rf "${temp_dir}"; }
trap cleanup EXIT

source_root="${temp_dir}/source"
mkdir -p "${temp_dir}/bin" "${source_root}/.git" "${source_root}/deploy"
printf 'service-unit\n' > "${source_root}/deploy/paper-mes.service.example"
printf 'release-preflight\n' > "${source_root}/deploy/preflight-paper-mes-release.example.sh"
printf 'migration-state-guard\n' > "${source_root}/deploy/verify-paper-mes-migration-state.example.sh"
printf 'migration-runner\n' > "${source_root}/deploy/apply-paper-mes-migrations.example.sh"
printf 'migration-lock\n' > "${source_root}/deploy/migration-lock-support.sh"
printf 'migration-state\n' > "${source_root}/deploy/migration-state-support.sh"
printf 'production-deploy\n' > "${source_root}/deploy/deploy-paper-mes.example.sh"
printf 'production-rollback\n' > "${source_root}/deploy/paper-mes-runtime-rollback.example.sh"
cp "${source_root}/deploy/paper-mes.service.example" "${temp_dir}/installed.service"
cp "${source_root}/deploy/preflight-paper-mes-release.example.sh" "${temp_dir}/installed-preflight"
cp "${source_root}/deploy/verify-paper-mes-migration-state.example.sh" "${temp_dir}/installed-migration-guard"
cp "${source_root}/deploy/apply-paper-mes-migrations.example.sh" "${temp_dir}/installed-migration-runner"
cp "${source_root}/deploy/migration-lock-support.sh" "${temp_dir}/installed-migration-lock"
cp "${source_root}/deploy/migration-state-support.sh" "${temp_dir}/installed-migration-state"
cp "${source_root}/deploy/deploy-paper-mes.example.sh" "${temp_dir}/installed-deploy"
cp "${source_root}/deploy/paper-mes-runtime-rollback.example.sh" "${temp_dir}/installed-rollback"

cat > "${temp_dir}/bin/git" <<'EOF'
#!/usr/bin/env bash
case "$*" in
  *"symbolic-ref --short HEAD"*) printf '%s\n' "${GIT_TEST_BRANCH:-main}" ;;
  *"rev-parse HEAD"*) printf '%s\n' "${GIT_TEST_HEAD:-commit-1}" ;;
  *"rev-parse refs/remotes/origin/main"*) printf '%s\n' "${GIT_TEST_REMOTE_HEAD:-commit-1}" ;;
  *"merge-base --is-ancestor"*) [ "${GIT_TEST_ANCESTOR:-1}" = '1' ] ;;
  *"status --porcelain --untracked-files=all"*)
    [ -z "${GIT_TEST_STATUS:-}" ] || printf '%s\n' "${GIT_TEST_STATUS}"
    ;;
  *) exit 1 ;;
esac
EOF
chmod 0700 "${temp_dir}/bin/git"

run_verify() {
  PATH="${temp_dir}/bin:${PATH}" \
  SOURCE_ROOT="${source_root}" \
  SERVICE_UNIT_PATH="${temp_dir}/installed.service" \
  INSTALLED_PREFLIGHT_PATH="${temp_dir}/installed-preflight" \
  INSTALLED_MIGRATION_GUARD_PATH="${temp_dir}/installed-migration-guard" \
  INSTALLED_MIGRATION_RUNNER_PATH="${temp_dir}/installed-migration-runner" \
  INSTALLED_MIGRATION_LOCK_PATH="${temp_dir}/installed-migration-lock" \
  INSTALLED_MIGRATION_STATE_PATH="${temp_dir}/installed-migration-state" \
  INSTALLED_DEPLOY_PATH="${temp_dir}/installed-deploy" \
  INSTALLED_ROLLBACK_PATH="${temp_dir}/installed-rollback" \
  bash "${verify_script}"
}

run_verify >/dev/null
if GIT_TEST_STATUS=' M tracked-file' run_verify >/dev/null 2>&1; then
  echo "source verification unexpectedly accepted a dirty cloud working tree" >&2
  exit 1
fi
GIT_TEST_REMOTE_HEAD=commit-2 run_verify >/dev/null
if GIT_TEST_HEAD=local-commit GIT_TEST_REMOTE_HEAD=commit-2 GIT_TEST_ANCESTOR=0 run_verify >/dev/null 2>&1; then
  echo "source verification unexpectedly accepted a commit outside GitHub main history" >&2
  exit 1
fi
if GIT_TEST_BRANCH=release run_verify >/dev/null 2>&1; then
  echo "source verification unexpectedly accepted the wrong branch" >&2
  exit 1
fi

printf 'direct-cloud-edit\n' > "${temp_dir}/installed.service"
if run_verify >/dev/null 2>&1; then
  echo "source verification unexpectedly accepted a directly edited service unit" >&2
  exit 1
fi
cp "${source_root}/deploy/paper-mes.service.example" "${temp_dir}/installed.service"
printf 'direct-cloud-edit\n' > "${temp_dir}/installed-preflight"
if run_verify >/dev/null 2>&1; then
  echo "source verification unexpectedly accepted a directly edited preflight" >&2
  exit 1
fi
cp "${source_root}/deploy/preflight-paper-mes-release.example.sh" "${temp_dir}/installed-preflight"
printf 'direct-cloud-edit\n' > "${temp_dir}/installed-migration-runner"
if run_verify >/dev/null 2>&1; then
  echo "source verification unexpectedly accepted a directly edited migration runner" >&2
  exit 1
fi
cp "${source_root}/deploy/apply-paper-mes-migrations.example.sh" "${temp_dir}/installed-migration-runner"
printf 'direct-cloud-edit\n' > "${temp_dir}/installed-migration-guard"
if run_verify >/dev/null 2>&1; then
  echo "source verification unexpectedly accepted a directly edited migration guard" >&2
  exit 1
fi
cp "${source_root}/deploy/verify-paper-mes-migration-state.example.sh" "${temp_dir}/installed-migration-guard"
printf 'direct-cloud-edit\n' > "${temp_dir}/installed-deploy"
if run_verify >/dev/null 2>&1; then
  echo "source verification unexpectedly accepted a directly edited production deployment script" >&2
  exit 1
fi
cp "${source_root}/deploy/deploy-paper-mes.example.sh" "${temp_dir}/installed-deploy"
printf 'direct-cloud-edit\n' > "${temp_dir}/installed-rollback"
if run_verify >/dev/null 2>&1; then
  echo "source verification unexpectedly accepted a directly edited production rollback helper" >&2
  exit 1
fi

echo "source provenance behavior test passed"
