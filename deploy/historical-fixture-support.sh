#!/usr/bin/env bash

# Artifact validation helpers for verify-historical-schema-fixture.example.sh.
manifest_value() {
  local key="$1"
  local count
  count="$(awk -F= -v key="${key}" '$1 == key { count++ } END { print count + 0 }' "${FIXTURE_MANIFEST}")"
  [ "${count}" = "1" ] || fail "manifest must contain exactly one ${key}"
  awk -F= -v key="${key}" '$1 == key { sub(/^[^=]*=/, ""); sub(/\r$/, ""); print }' "${FIXTURE_MANIFEST}"
}

validate_manifest() {
  fixture_format="$(manifest_value format)"
  fixture_version="$(manifest_value fixture_version)"
  fixture_checksum="$(manifest_value fixture_sha256)"
  sanitization="$(manifest_value sanitization)"
  approved_by="$(manifest_value approved_by)"
  approved_at="$(manifest_value approved_at)"
  [ "${fixture_format}" = "paper-mes-historical-fixture-v1" ] || fail "unsupported manifest format"
  [[ "${fixture_version}" =~ ^[0-9]+(\.[0-9]+)*$ ]] || fail "invalid fixture_version"
  [[ "${fixture_checksum}" =~ ^[0-9a-f]{64}$ ]] || fail "invalid fixture_sha256"
  [ "${sanitization}" = "approved" ] || fail "fixture sanitization is not approved"
  [ -n "${approved_by}" ] && [ "${#approved_by}" -le 100 ] || fail "invalid approved_by"
  if printf '%s' "${approved_by}" | LC_ALL=C awk '/[[:cntrl:]]/ { found=1 } END { exit !found }'; then
    fail "approved_by contains a control character"
  fi
  [[ "${approved_at}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(Z|[+-][0-9]{2}:[0-9]{2})$ ]] \
    || fail "approved_at must be ISO-8601 with timezone"
}

dump_stream() {
  case "${FIXTURE_DUMP}" in
    *.sql.gz) gzip -cd -- "${FIXTURE_DUMP}" ;;
    *.sql) cat -- "${FIXTURE_DUMP}" ;;
    *) fail "FIXTURE_DUMP must end in .sql or .sql.gz" ;;
  esac
}

validate_dump_scope() {
  local forbidden
  forbidden="$(dump_stream | awk '
    { line=toupper($0) }
    !found && line ~ /^[[:space:]]*(CREATE|DROP)[[:space:]]+DATABASE/ { print "blocked"; found=1 }
    !found && line ~ /^[[:space:]]*USE[[:space:]]/ { print "blocked"; found=1 }
    !found && line ~ /^[[:space:]]*(CREATE|ALTER|DROP)[[:space:]]+USER/ { print "blocked"; found=1 }
    !found && line ~ /^[[:space:]]*(GRANT|REVOKE|SOURCE|SYSTEM|\\!)/ { print "blocked"; found=1 }
    !found && line ~ /(INTO[[:space:]]+OUTFILE|SET[[:space:]]+(@@)?GLOBAL|DEFINER[[:space:]]*=)/ { print "blocked"; found=1 }
  ')"
  [ -z "${forbidden}" ] || fail "dump contains an out-of-scope statement"
}

is_newer_version() {
  local candidate="$1"
  [ "${candidate}" != "${fixture_version}" ] \
    && [ "$(printf '%s\n' "${candidate}" "${fixture_version}" | sort -V | head -n 1)" = "${fixture_version}" ]
}

prepare_pending_migrations() {
  local script name version
  pending_count=0
  boundary_found=0
  mkdir "${temp_dir}/migrations"
  while IFS= read -r -d '' script; do
    name="$(basename "${script}")"
    [[ "${name}" =~ ^V[0-9]+(\.[0-9]+)*__[A-Za-z0-9._-]+\.sql$ ]] || fail "invalid migration filename"
    version="${name%%__*}"
    version="${version#V}"
    [ "${version}" != "${fixture_version}" ] || boundary_found=1
    is_newer_version "${version}" || continue
    cp -- "${script}" "${temp_dir}/migrations/${name}"
    pending_count=$((pending_count + 1))
  done < <(find "${MIGRATION_DIR}" -maxdepth 1 -type f -name 'V*.sql' -print0 | sort -z -V)
  [ "${fixture_version}" = "1.1" ] || [ "${boundary_found}" = "1" ] \
    || fail "fixture_version is not a known migration boundary"
  [ "${pending_count}" -gt 0 ] || fail "no migrations exist after fixture_version"
}
