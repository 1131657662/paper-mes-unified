#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
preflight_script="${script_dir}/preflight-paper-mes-release.example.sh"
temp_dir="$(mktemp -d)"

cleanup() { rm -rf "${temp_dir}"; }
trap cleanup EXIT

mkdir -p "${temp_dir}/bin" "${temp_dir}/app-tmp" "${temp_dir}/proc/4242" \
  "${temp_dir}/source/sql" \
  "${temp_dir}/proc/4243" "${temp_dir}/backups/$(date +%Y%m%d-%H%M%S)"
chmod 0750 "${temp_dir}/app-tmp"
printf '3.63\n' > "${temp_dir}/source/sql/schema-baseline.version"
printf 'PAPER_MES_EXPECTED_SCHEMA_VERSION=3.63\n' > "${temp_dir}/paper-mes.env"
backup_dir="$(find "${temp_dir}/backups" -mindepth 1 -maxdepth 1 -type d)"
printf 'backup-data' > "${backup_dir}/paper.sql.gz"
(cd "${backup_dir}" && sha256sum paper.sql.gz > SHA256SUMS)
printf '%s\0' /usr/bin/java "-Djava.io.tmpdir=${temp_dir}/app-tmp" \
  > "${temp_dir}/proc/4242/cmdline"
printf '%s\0' /usr/bin/java -Xmx768m > "${temp_dir}/proc/4243/cmdline"

cat > "${temp_dir}/bin/mysql" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "${MYSQL_TEST_RESULT:-0}"
EOF
cat > "${temp_dir}/bin/curl" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' '{"status":"UP"}'
EOF
cat > "${temp_dir}/bin/runuser" <<'EOF'
#!/usr/bin/env bash
exit "${RUNUSER_TEST_RESULT:-0}"
EOF
cat > "${temp_dir}/bin/systemctl" <<'EOF'
#!/usr/bin/env bash
case "$*" in
  *--property=PrivateTmp*) printf '%s\n' "${SYSTEMCTL_PRIVATE_TMP:-yes}" ;;
  *--property=RuntimeDirectory*) printf '%s\n' "${APP_RUNTIME_DIRECTORY}" ;;
  *--property=ExecStart*)
    printf '/usr/bin/java -Djava.io.tmpdir=%s -jar paper-mes.jar\n' "${SYSTEMCTL_EXEC_TMP_DIR}"
    ;;
  *--property=MainPID*) printf '%s\n' "${SYSTEMCTL_MAIN_PID:-4242}" ;;
  *) exit 1 ;;
esac
EOF
cat > "${temp_dir}/bin/verify-source" <<'EOF'
#!/usr/bin/env bash
exit "${SOURCE_PROVENANCE_TEST_RESULT:-0}"
EOF
chmod 0700 "${temp_dir}/bin/mysql" "${temp_dir}/bin/curl" \
  "${temp_dir}/bin/runuser" "${temp_dir}/bin/systemctl" "${temp_dir}/bin/verify-source"

app_user="$(stat -c %U "${temp_dir}/app-tmp")"
app_group="$(stat -c %G "${temp_dir}/app-tmp")"

run_preflight() {
  PATH="${temp_dir}/bin:${PATH}" \
  BACKUP_ENV_FILE=/dev/null \
  MIGRATION_ENV_FILE=/dev/null \
  DB_PASSWORD=test-only \
  BACKUP_ROOT="${temp_dir}/backups" \
  MAX_BACKUP_AGE_HOURS=48 \
  MYSQL_TEST_RESULT="$1" \
  RUNUSER_TEST_RESULT="${2:-0}" \
  SYSTEMCTL_PRIVATE_TMP="${3:-yes}" \
  SYSTEMCTL_EXEC_TMP_DIR="${4:-${temp_dir}/app-tmp}" \
  SYSTEMCTL_MAIN_PID="${5:-4242}" \
  APP_USER="${app_user}" \
  APP_GROUP="${app_group}" \
  APP_RUNTIME_DIRECTORY=app-tmp \
  APP_TMP_DIR="${temp_dir}/app-tmp" \
  PROC_ROOT="${temp_dir}/proc" \
  SOURCE_ROOT="${temp_dir}/source" \
  SCHEMA_BASELINE_FILE="${temp_dir}/source/sql/schema-baseline.version" \
  APP_ENV_FILE="${temp_dir}/paper-mes.env" \
  SOURCE_PROVENANCE_SCRIPT="${temp_dir}/bin/verify-source" \
  bash "${preflight_script}"
}

run_preflight 0 0 >/dev/null
printf 'PAPER_MES_EXPECTED_SCHEMA_VERSION=3.63\nPAPER_MES_AI_PROVIDER=DEEPSEEK\nPAPER_MES_AI_DATA_MODE=CONTEXT_ALLOWLIST\n' > "${temp_dir}/paper-mes.env"
if run_preflight 0 0 >/dev/null 2>&1; then
  echo "preflight unexpectedly accepted enabled AI without a master key" >&2
  exit 1
fi
printf 'PAPER_MES_EXPECTED_SCHEMA_VERSION=3.63\nPAPER_MES_AI_PROVIDER=DEEPSEEK\nPAPER_MES_AI_DATA_MODE=CONTEXT_ALLOWLIST\nPAPER_MES_AI_CONFIG_MASTER_KEY=QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUE=\n' > "${temp_dir}/paper-mes.env"
run_preflight 0 0 >/dev/null
printf 'PAPER_MES_EXPECTED_SCHEMA_VERSION=3.62\n' > "${temp_dir}/paper-mes.env"
if run_preflight 0 0 >/dev/null 2>&1; then
  echo "preflight unexpectedly accepted a schema baseline mismatch" >&2
  exit 1
fi
printf 'PAPER_MES_EXPECTED_SCHEMA_VERSION=3.63\n' > "${temp_dir}/paper-mes.env"
if run_preflight 1 0 >/dev/null 2>&1; then
  echo "preflight unexpectedly accepted a database conflict" >&2
  exit 1
fi
if run_preflight 0 1 >/dev/null 2>&1; then
  echo "preflight unexpectedly accepted an unwritable application temp directory" >&2
  exit 1
fi
if run_preflight 0 0 no >/dev/null 2>&1; then
  echo "preflight unexpectedly accepted PrivateTmp=no" >&2
  exit 1
fi
if run_preflight 0 0 yes /tmp >/dev/null 2>&1; then
  echo "preflight unexpectedly accepted a service unit without the managed temp directory" >&2
  exit 1
fi
if run_preflight 0 0 yes "${temp_dir}/app-tmp" 4243 >/dev/null 2>&1; then
  echo "preflight unexpectedly accepted a running process without the managed temp directory" >&2
  exit 1
fi
if SOURCE_PROVENANCE_TEST_RESULT=1 run_preflight 0 0 >/dev/null 2>&1; then
  echo "preflight unexpectedly accepted source files not pulled from GitHub" >&2
  exit 1
fi

echo "release preflight behavior test passed"
