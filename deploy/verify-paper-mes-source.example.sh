#!/usr/bin/env bash
set -Eeuo pipefail

SOURCE_ROOT="${SOURCE_ROOT:-/opt/paper-mes/source}"
SOURCE_REMOTE="${SOURCE_REMOTE:-origin}"
SOURCE_BRANCH="${SOURCE_BRANCH:-main}"
SERVICE_UNIT_PATH="${SERVICE_UNIT_PATH:-/etc/systemd/system/paper-mes.service}"
INSTALLED_PREFLIGHT_PATH="${INSTALLED_PREFLIGHT_PATH:-/usr/local/bin/preflight-paper-mes-release}"
INSTALLED_MIGRATION_GUARD_PATH="${INSTALLED_MIGRATION_GUARD_PATH:-/usr/local/bin/verify-paper-mes-migration-state}"
INSTALLED_MIGRATION_RUNNER_PATH="${INSTALLED_MIGRATION_RUNNER_PATH:-/usr/local/bin/apply-paper-mes-migrations}"
INSTALLED_MIGRATION_LOCK_PATH="${INSTALLED_MIGRATION_LOCK_PATH:-/usr/local/bin/migration-lock-support.sh}"
INSTALLED_MIGRATION_STATE_PATH="${INSTALLED_MIGRATION_STATE_PATH:-/usr/local/bin/migration-state-support.sh}"
INSTALLED_DEPLOY_PATH="${INSTALLED_DEPLOY_PATH:-/usr/local/sbin/deploy-paper-mes.sh}"
INSTALLED_ROLLBACK_PATH="${INSTALLED_ROLLBACK_PATH:-/usr/local/sbin/paper-mes-runtime-rollback.sh}"

fail() {
  echo "source provenance verification failed: $1" >&2
  exit 1
}

for command_name in git cmp; do
  command -v "${command_name}" >/dev/null 2>&1 || fail "required command not found: ${command_name}"
done
[[ "${SOURCE_REMOTE}" =~ ^[A-Za-z0-9][A-Za-z0-9._-]*$ ]] || fail "invalid SOURCE_REMOTE"
[[ "${SOURCE_BRANCH}" =~ ^[A-Za-z0-9][A-Za-z0-9._/-]*$ ]] || fail "invalid SOURCE_BRANCH"
[ -d "${SOURCE_ROOT}/.git" ] || fail "source is not a Git working tree: ${SOURCE_ROOT}"

current_branch="$(git -C "${SOURCE_ROOT}" symbolic-ref --short HEAD)" \
  || fail "cannot read current source branch"
[ "${current_branch}" = "${SOURCE_BRANCH}" ] \
  || fail "source branch is ${current_branch}, expected ${SOURCE_BRANCH}"
head_commit="$(git -C "${SOURCE_ROOT}" rev-parse HEAD)" || fail "cannot read source HEAD"
remote_commit="$(git -C "${SOURCE_ROOT}" rev-parse "refs/remotes/${SOURCE_REMOTE}/${SOURCE_BRANCH}")" \
  || fail "cannot read ${SOURCE_REMOTE}/${SOURCE_BRANCH}"
[ "${head_commit}" = "${remote_commit}" ] \
  || fail "source HEAD ${head_commit} does not match ${SOURCE_REMOTE}/${SOURCE_BRANCH} ${remote_commit}"

working_tree_status="$(git -C "${SOURCE_ROOT}" status --porcelain --untracked-files=all)" \
  || fail "cannot inspect source working tree"
[ -z "${working_tree_status}" ] || fail "cloud source working tree has local changes"

source_service_unit="${SOURCE_ROOT}/deploy/paper-mes.service.example"
source_preflight="${SOURCE_ROOT}/deploy/preflight-paper-mes-release.example.sh"
source_migration_guard="${SOURCE_ROOT}/deploy/verify-paper-mes-migration-state.example.sh"
source_migration_runner="${SOURCE_ROOT}/deploy/apply-paper-mes-migrations.example.sh"
source_migration_lock="${SOURCE_ROOT}/deploy/migration-lock-support.sh"
source_migration_state="${SOURCE_ROOT}/deploy/migration-state-support.sh"
source_deploy="${SOURCE_ROOT}/deploy/deploy-paper-mes.example.sh"
source_rollback="${SOURCE_ROOT}/deploy/paper-mes-runtime-rollback.example.sh"
[ -f "${source_service_unit}" ] || fail "service unit template is missing from source"
[ -f "${source_preflight}" ] || fail "release preflight is missing from source"
[ -f "${source_migration_guard}" ] || fail "migration state guard is missing from source"
[ -f "${source_migration_runner}" ] || fail "migration runner is missing from source"
[ -f "${source_migration_lock}" ] || fail "migration lock support is missing from source"
[ -f "${source_migration_state}" ] || fail "migration state support is missing from source"
[ -f "${source_deploy}" ] || fail "production deployment script is missing from source"
[ -f "${source_rollback}" ] || fail "production rollback helper is missing from source"
cmp -s "${source_service_unit}" "${SERVICE_UNIT_PATH}" \
  || fail "installed service unit does not match the pulled source"
cmp -s "${source_preflight}" "${INSTALLED_PREFLIGHT_PATH}" \
  || fail "installed release preflight does not match the pulled source"
cmp -s "${source_migration_guard}" "${INSTALLED_MIGRATION_GUARD_PATH}" \
  || fail "installed migration state guard does not match the pulled source"
cmp -s "${source_migration_runner}" "${INSTALLED_MIGRATION_RUNNER_PATH}" \
  || fail "installed migration runner does not match the pulled source"
cmp -s "${source_migration_lock}" "${INSTALLED_MIGRATION_LOCK_PATH}" \
  || fail "installed migration lock support does not match the pulled source"
cmp -s "${source_migration_state}" "${INSTALLED_MIGRATION_STATE_PATH}" \
  || fail "installed migration state support does not match the pulled source"
cmp -s "${source_deploy}" "${INSTALLED_DEPLOY_PATH}" \
  || fail "installed production deployment script does not match the pulled source"
cmp -s "${source_rollback}" "${INSTALLED_ROLLBACK_PATH}" \
  || fail "installed production rollback helper does not match the pulled source"

echo "source provenance verification passed: ${head_commit}"
