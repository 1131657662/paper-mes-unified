#!/usr/bin/env bash
set -Eeuo pipefail
umask 022

FRONTEND_ROOT="${FRONTEND_ROOT:-/opt/paper-mes/frontend}"
KEEP_RELEASES="${KEEP_RELEASES:-3}"
MIN_RETENTION_HOURS="${MIN_RETENTION_HOURS:-72}"
RELEASES_DIR="${FRONTEND_ROOT}/releases"
SHARED_ASSETS="${FRONTEND_ROOT}/assets"
DIST_LINK="${FRONTEND_ROOT}/dist"
LOCK_FILE="${FRONTEND_ROOT}/.frontend-release.lock"
stage_dir=""
archived_legacy_dir=""

fail() {
  echo "frontend release failed: $1" >&2
  exit 1
}

cleanup() {
  if [ -n "${stage_dir}" ] && [ -d "${stage_dir}" ]; then
    rm -rf -- "${stage_dir}"
  fi
}
trap cleanup EXIT

validate_configuration() {
  [[ "${KEEP_RELEASES}" =~ ^[1-9][0-9]*$ ]] || fail "KEEP_RELEASES must be positive"
  [[ "${MIN_RETENTION_HOURS}" =~ ^[0-9]+$ ]] || fail "MIN_RETENTION_HOURS must be non-negative"
  [ "$(realpath -m "${FRONTEND_ROOT}")" != "/" ] || fail "FRONTEND_ROOT cannot be /"
}

validate_release_id() {
  [[ "$1" =~ ^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$ ]] || fail "invalid release id: $1"
}

require_commands() {
  local command_name
  for command_name in cmp cp date find flock grep ln mkdir mktemp mv readlink realpath rm sort stat touch; do
    command -v "${command_name}" >/dev/null 2>&1 || fail "required command not found: ${command_name}"
  done
}

write_asset_manifest() {
  local assets_dir="$1"
  local manifest_path="$2"
  (cd "${assets_dir}" && find . -type f -printf '%P\n' | LC_ALL=C sort) > "${manifest_path}"
}

import_assets() {
  local source_assets="$1"
  local source_file relative_path target_file
  find "${source_assets}" -type l -print -quit | grep -q . \
    && fail "asset symlinks are not allowed"
  while IFS= read -r -d '' source_file; do
    relative_path="${source_file#"${source_assets}/"}"
    target_file="${SHARED_ASSETS}/${relative_path}"
    mkdir -p -- "$(dirname "${target_file}")"
    if [ -e "${target_file}" ]; then
      cmp -s "${source_file}" "${target_file}" \
        || fail "asset hash collision: ${relative_path}"
    else
      cp -a -- "${source_file}" "${target_file}"
    fi
  done < <(find "${source_assets}" -type f -print0)
}

archive_legacy_dist() {
  [ -d "${DIST_LINK}" ] || return 0
  [ ! -L "${DIST_LINK}" ] || return 0
  [ -f "${DIST_LINK}/index.html" ] || fail "legacy dist has no index.html"
  [ -d "${DIST_LINK}/assets" ] || fail "legacy dist has no assets directory"
  import_assets "${DIST_LINK}/assets"
  local legacy_id legacy_dir legacy_mtime manifest
  legacy_mtime="$(stat -c %Y "${DIST_LINK}")"
  legacy_id="legacy-$(date -u +%Y%m%d-%H%M%S)"
  legacy_dir="${RELEASES_DIR}/${legacy_id}"
  [ ! -e "${legacy_dir}" ] || legacy_dir="${legacy_dir}-$$"
  manifest="$(mktemp "${FRONTEND_ROOT}/.legacy-assets.XXXXXX")"
  write_asset_manifest "${DIST_LINK}/assets" "${manifest}"
  mv -T -- "${DIST_LINK}" "${legacy_dir}"
  if ! mv -- "${manifest}" "${legacy_dir}/assets.manifest" \
    || ! touch -d "@${legacy_mtime}" "${legacy_dir}"; then
    mv -T -- "${legacy_dir}" "${DIST_LINK}"
    fail "could not archive legacy dist"
  fi
  archived_legacy_dir="${legacy_dir}"
}

activate_release() {
  local release_id="$1"
  local next_link="${FRONTEND_ROOT}/.dist-next-${release_id}-$$"
  if [ -e "${DIST_LINK}" ] && [ ! -d "${DIST_LINK}" ] && [ ! -L "${DIST_LINK}" ]; then
    fail "dist path is not a directory or symlink"
  fi
  ln -s "releases/${release_id}" "${next_link}"
  archived_legacy_dir=""
  archive_legacy_dist
  if ! mv -Tf -- "${next_link}" "${DIST_LINK}"; then
    rm -f -- "${next_link}"
    if [ -n "${archived_legacy_dir}" ] && [ ! -e "${DIST_LINK}" ]; then
      mv -T -- "${archived_legacy_dir}" "${DIST_LINK}"
    fi
    fail "could not activate release symlink"
  fi
}

safe_remove_release() {
  local release_path resolved releases_root
  release_path="$1"
  resolved="$(realpath -m "${release_path}")"
  releases_root="$(realpath -m "${RELEASES_DIR}")"
  [[ "${resolved}" == "${releases_root}/"* ]] || fail "refusing release path outside root"
  [ "${resolved}" != "$(readlink -f "${DIST_LINK}")" ] || return 0
  rm -rf -- "${resolved}"
}

prune_shared_assets() {
  local keep_manifest asset_file relative_path
  keep_manifest="$(mktemp "${FRONTEND_ROOT}/.retained-assets.XXXXXX")"
  find "${RELEASES_DIR}" -mindepth 2 -maxdepth 2 -name assets.manifest \
    -exec cat {} + | LC_ALL=C sort -u > "${keep_manifest}"
  while IFS= read -r -d '' asset_file; do
    relative_path="${asset_file#"${SHARED_ASSETS}/"}"
    grep -Fxq -- "${relative_path}" "${keep_manifest}" || rm -f -- "${asset_file}"
  done < <(find "${SHARED_ASSETS}" -type f -print0)
  find "${SHARED_ASSETS}" -depth -type d -empty -delete
  rm -f -- "${keep_manifest}"
}

prune_releases() {
  local cutoff index entry timestamp release_path
  local -a releases
  cutoff=$(( $(date +%s) - MIN_RETENTION_HOURS * 3600 ))
  mapfile -t releases < <(find "${RELEASES_DIR}" -mindepth 1 -maxdepth 1 -type d \
    ! -name '.staging-*' -printf '%T@ %p\n' | sort -nr)
  for index in "${!releases[@]}"; do
    entry="${releases[$index]}"
    timestamp="${entry%% *}"
    release_path="${entry#* }"
    if (( index < KEEP_RELEASES )); then continue; fi
    if (( MIN_RETENTION_HOURS > 0 )) && (( ${timestamp%.*} >= cutoff )); then continue; fi
    safe_remove_release "${release_path}"
  done
  prune_shared_assets
}

publish_release() {
  local source_dist release_id source_real final_dir item
  source_dist="$1"
  release_id="$2"
  validate_release_id "${release_id}"
  source_real="$(realpath -e "${source_dist}")" || fail "source dist not found"
  [[ "${source_real}/" != "$(realpath -m "${FRONTEND_ROOT}")/"* ]] \
    || fail "source dist must be outside FRONTEND_ROOT"
  [ -f "${source_real}/index.html" ] || fail "source dist has no index.html"
  [ -d "${source_real}/assets" ] || fail "source dist has no assets directory"
  final_dir="${RELEASES_DIR}/${release_id}"
  [ ! -e "${final_dir}" ] || fail "release already exists: ${release_id}"
  import_assets "${source_real}/assets"
  stage_dir="$(mktemp -d "${RELEASES_DIR}/.staging-${release_id}.XXXXXX")"
  while IFS= read -r -d '' item; do cp -a -- "${item}" "${stage_dir}/"; done \
    < <(find "${source_real}" -mindepth 1 -maxdepth 1 ! -name assets -print0)
  write_asset_manifest "${source_real}/assets" "${stage_dir}/assets.manifest"
  ln -s ../../assets "${stage_dir}/assets"
  mv -T -- "${stage_dir}" "${final_dir}"
  stage_dir=""
  activate_release "${release_id}"
  prune_releases
  [ "$(readlink -f "${DIST_LINK}")" = "$(realpath -e "${final_dir}")" ] \
    || fail "release activation verification failed"
  echo "frontend release activated: ${release_id}"
}

rollback_release() {
  local release_id="$1"
  validate_release_id "${release_id}"
  [ -f "${RELEASES_DIR}/${release_id}/index.html" ] || fail "release not found: ${release_id}"
  activate_release "${release_id}"
  echo "frontend release rolled back: ${release_id}"
}

main() {
  require_commands
  validate_configuration
  mkdir -p -- "${RELEASES_DIR}" "${SHARED_ASSETS}"
  exec 9>"${LOCK_FILE}"
  flock -n 9 || fail "another frontend release is running"
  case "${1:-}" in
    publish) [ "$#" = 3 ] || fail "usage: $0 publish SOURCE_DIST RELEASE_ID"; publish_release "$2" "$3" ;;
    rollback) [ "$#" = 2 ] || fail "usage: $0 rollback RELEASE_ID"; rollback_release "$2" ;;
    *) fail "usage: $0 publish SOURCE_DIST RELEASE_ID | rollback RELEASE_ID" ;;
  esac
}

main "$@"
