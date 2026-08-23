#!/usr/bin/env bash
set -Eeuo pipefail

source_root=/opt/paper-mes-test/source
deploy_sha="${1:-}"
[[ "${deploy_sha}" =~ ^[0-9a-f]{40}$ ]] || {
  echo "a full commit sha is required" >&2
  exit 2
}

git -C "${source_root}" fetch --prune origin main
remote_main="$(git -C "${source_root}" rev-parse origin/main 2>/dev/null || true)"
if [ "${remote_main}" != "${deploy_sha}" ]; then
  printf 'requested commit is not the current origin/main: requested=%s fetched=%s remote=%s\n' \
    "${deploy_sha}" "${remote_main:-<missing>}" \
    "$(git -C "${source_root}" remote get-url origin 2>/dev/null || printf '<missing>')" >&2
  git -C "${source_root}" status --short --branch >&2 || true
  exit 1
fi

tmp_script="$(mktemp)"
cleanup() { rm -f "${tmp_script}"; }
trap cleanup EXIT
git -C "${source_root}" show "${deploy_sha}:deploy/deploy-paper-mes-test.example.sh" > "${tmp_script}"
chmod 700 "${tmp_script}"
exec "${tmp_script}" "${deploy_sha}"
