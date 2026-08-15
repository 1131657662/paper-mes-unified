#!/usr/bin/env bash
set -Eeuo pipefail

source_root=/opt/paper-mes-test/source
deploy_sha="${1:-}"
[[ "${deploy_sha}" =~ ^[0-9a-f]{40}$ ]] || {
  echo "a full commit sha is required" >&2
  exit 2
}

git -C "${source_root}" fetch --prune origin main
[ "$(git -C "${source_root}" rev-parse origin/main)" = "${deploy_sha}" ] || {
  echo "requested commit is not the current origin/main" >&2
  exit 1
}

tmp_script="$(mktemp)"
cleanup() { rm -f "${tmp_script}"; }
trap cleanup EXIT
git -C "${source_root}" show "${deploy_sha}:deploy/deploy-paper-mes-test.example.sh" > "${tmp_script}"
chmod 700 "${tmp_script}"
exec "${tmp_script}" "${deploy_sha}"
