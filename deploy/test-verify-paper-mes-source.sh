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
cp "${source_root}/deploy/paper-mes.service.example" "${temp_dir}/installed.service"
cp "${source_root}/deploy/preflight-paper-mes-release.example.sh" "${temp_dir}/installed-preflight"

cat > "${temp_dir}/bin/git" <<'EOF'
#!/usr/bin/env bash
case "$*" in
  *"symbolic-ref --short HEAD"*) printf '%s\n' "${GIT_TEST_BRANCH:-main}" ;;
  *"rev-parse HEAD"*) printf '%s\n' "${GIT_TEST_HEAD:-commit-1}" ;;
  *"rev-parse refs/remotes/origin/main"*) printf '%s\n' "${GIT_TEST_REMOTE_HEAD:-commit-1}" ;;
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
  bash "${verify_script}"
}

run_verify >/dev/null
if GIT_TEST_STATUS=' M tracked-file' run_verify >/dev/null 2>&1; then
  echo "source verification unexpectedly accepted a dirty cloud working tree" >&2
  exit 1
fi
if GIT_TEST_REMOTE_HEAD=commit-2 run_verify >/dev/null 2>&1; then
  echo "source verification unexpectedly accepted a commit not pulled from GitHub" >&2
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

echo "source provenance behavior test passed"
