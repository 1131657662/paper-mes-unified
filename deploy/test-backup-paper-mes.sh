#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
backup_script="${script_dir}/backup-paper-mes.example.sh"
temp_dir="$(mktemp -d)"
cleanup() { rm -rf "${temp_dir}"; }
trap cleanup EXIT

mkdir -p "${temp_dir}/bin" "${temp_dir}/upload"
printf 'test upload\n' > "${temp_dir}/upload/sample.txt"
cat > "${temp_dir}/bin/mysqldump" <<'EOF'
#!/usr/bin/env bash
printf 'CREATE TABLE test_backup (id INT);\n'
EOF
chmod 700 "${temp_dir}/bin/mysqldump"

backup_group="$(id -gn)"
PATH="${temp_dir}/bin:${PATH}" \
BACKUP_ENV_FILE=/dev/null \
BACKUP_ROOT="${temp_dir}/backups" \
BACKUP_GROUP="${backup_group}" \
DB_NAME=test_processing \
DB_USER=test_backup \
DB_PASSWORD=test-only \
UPLOAD_DIR="${temp_dir}/upload" \
bash "${backup_script}" >/dev/null

backup_dir="$(find "${temp_dir}/backups" -mindepth 1 -maxdepth 1 -type d -name '????????-??????')"
[ -n "${backup_dir}" ]
[ "$(stat -c %a "${backup_dir}")" = 750 ]
[ "$(stat -c %G "${backup_dir}")" = "${backup_group}" ]

for backup_file in test_processing.sql.gz upload.tar.gz backup-info.txt SHA256SUMS; do
  file="${backup_dir}/${backup_file}"
  [ -f "${file}" ]
  [ "$(stat -c %a "${file}")" = 640 ]
  [ "$(stat -c %G "${file}")" = "${backup_group}" ]
done

report="${backup_dir}/restore-check.txt"
[ -f "${report}" ]
[ "$(stat -c %a "${report}")" = 660 ]
[ "$(stat -c %G "${report}")" = "${backup_group}" ]
printf 'verified_at=test\n' > "${report}"
grep -Fx 'verified_at=test' "${report}" >/dev/null

echo "backup permission handoff test passed"
