#!/usr/bin/env bash
set -Eeuo pipefail
umask 027

mode="${1:-production}"
case "${mode}" in
  production)
    source_root=/opt/paper-mes/source
    restore_env=/etc/paper-mes/backup-restore.env
    verifier=/usr/local/bin/verify-paper-mes-backup
    wrapper=/usr/local/sbin/verify-paper-mes-backup-root
    sudoers=/etc/sudoers.d/paper-mes-backup-verify
    sudoers_source=paper-mes-backup-verify.sudoers.example
    service_user=paper-mes
    ;;
  test)
    source_root=/opt/paper-mes-test/source
    restore_env=/etc/paper-mes-test/backup-restore.env
    verifier=/usr/local/bin/verify-paper-mes-test-backup
    wrapper=/usr/local/sbin/verify-paper-mes-test-backup-root
    sudoers=/etc/sudoers.d/paper-mes-test-backup-verify
    sudoers_source=paper-mes-test-backup-verify.sudoers.example
    service_user=paper-mes-test
    ;;
  *)
    echo "usage: $0 [production|test]" >&2
    exit 2
    ;;
esac

[ "$(id -u)" = 0 ] || { echo "root privileges are required" >&2; exit 1; }
[ -d "${source_root}/.git" ] || { echo "source repository is missing" >&2; exit 1; }
[ -f "${restore_env}" ] || { echo "root-only restore configuration is missing" >&2; exit 1; }
[ "$(stat -c '%U:%G:%a' "${restore_env}")" = "root:root:600" ] \
  || { echo "restore configuration must be root:root 0600" >&2; exit 1; }

install -o root -g root -m 0700 \
  "${source_root}/deploy/verify-backup-restore.example.sh" "${verifier}"
install -o root -g root -m 0700 \
  "${source_root}/deploy/verify-paper-mes-backup-root.example.sh" "${wrapper}"
sudoers_tmp="$(mktemp)"
trap 'rm -f "${sudoers_tmp}"' EXIT
install -o root -g root -m 0440 \
  "${source_root}/deploy/${sudoers_source}" "${sudoers_tmp}"
visudo -cf "${sudoers_tmp}"
mv -f "${sudoers_tmp}" "${sudoers}"
trap - EXIT

echo "installed root backup verification boundary for ${service_user}"
