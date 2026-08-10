#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
publisher="${script_dir}/publish-paper-mes-frontend.example.sh"
temp_dir="$(mktemp -d)"
frontend_root="${temp_dir}/frontend"

cleanup() { rm -rf -- "${temp_dir}"; }
trap cleanup EXIT

make_dist() {
  local dist_dir="$1"
  local version="$2"
  local asset_name="$3"
  local asset_content="$4"
  mkdir -p "${dist_dir}/assets"
  printf '%s\n' "${version}" > "${dist_dir}/index.html"
  printf '%s\n' "${asset_content}" > "${dist_dir}/assets/${asset_name}"
}

publish() {
  FRONTEND_ROOT="${frontend_root}" KEEP_RELEASES=2 MIN_RETENTION_HOURS=0 \
    bash "${publisher}" publish "$1" "$2"
}

make_dist "${frontend_root}/dist" legacy old-hash.js old
make_dist "${temp_dir}/release-one" one one-hash.js one
chmod 0600 "${temp_dir}/release-one/index.html" "${temp_dir}/release-one/assets/one-hash.js"
publish "${temp_dir}/release-one" release-one >/dev/null

[ -L "${frontend_root}/dist" ] || { echo "dist was not activated as a symlink" >&2; exit 1; }
[ "$(stat -c %a "${frontend_root}/releases/release-one")" = 755 ] \
  || { echo "release directory is not readable by the web server" >&2; exit 1; }
[ "$(stat -c %a "${frontend_root}/dist/index.html")" = 644 ] \
  || { echo "release files are not readable by the web server" >&2; exit 1; }
[ "$(stat -c %a "${frontend_root}/dist/assets/one-hash.js")" = 644 ] \
  || { echo "shared assets are not readable by the web server" >&2; exit 1; }
[ "$(cat "${frontend_root}/dist/index.html")" = one ] || exit 1
[ -f "${frontend_root}/dist/assets/old-hash.js" ] || exit 1
[ -f "${frontend_root}/dist/assets/one-hash.js" ] || exit 1

make_dist "${temp_dir}/collision" collision one-hash.js changed
if publish "${temp_dir}/collision" collision >/dev/null 2>&1; then
  echo "publisher accepted a hashed asset collision" >&2
  exit 1
fi
[ "$(cat "${frontend_root}/dist/index.html")" = one ] || exit 1

make_dist "${temp_dir}/release-two" two two-hash.js two
publish "${temp_dir}/release-two" release-two >/dev/null
[ ! -f "${frontend_root}/assets/old-hash.js" ] || { echo "expired asset was retained" >&2; exit 1; }
[ -f "${frontend_root}/assets/one-hash.js" ] || exit 1
[ -f "${frontend_root}/assets/two-hash.js" ] || exit 1

FRONTEND_ROOT="${frontend_root}" KEEP_RELEASES=2 MIN_RETENTION_HOURS=0 \
  bash "${publisher}" rollback release-one >/dev/null
[ "$(cat "${frontend_root}/dist/index.html")" = one ] || exit 1

if FRONTEND_ROOT="${frontend_root}" bash "${publisher}" rollback '../outside' >/dev/null 2>&1; then
  echo "publisher accepted an invalid release id" >&2
  exit 1
fi

echo "frontend release behavior test passed"
