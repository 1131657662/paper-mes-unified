#!/usr/bin/env bash

add_issue() {
  issue_keys+=("$1")
  issues_en+=("$2")
  issues_zh+=("$3")
}

require_positive_integer() {
  local name="$1" value="$2"
  [[ "${value}" =~ ^[1-9][0-9]*$ ]] || {
    echo "${name} must be a positive integer" >&2
    exit 2
  }
}

check_systemd_units() {
  local unit
  for unit in ${SYSTEMD_UNITS}; do
    systemctl is-active --quiet "${unit}" || add_issue "systemd:${unit}:inactive" \
      "systemd unit ${unit} is not active" \
      "systemd 服务 ${unit} 未运行"
  done
}

check_timers() {
  local timer
  for timer in ${SYSTEMD_TIMERS}; do
    systemctl is-active --quiet "${timer}" || add_issue "timer:${timer}:inactive" \
      "systemd timer ${timer} is not active" \
      "systemd 定时器 ${timer} 未运行"
    systemctl is-enabled --quiet "${timer}" || add_issue "timer:${timer}:disabled" \
      "systemd timer ${timer} is not enabled" \
      "systemd 定时器 ${timer} 未启用"
  done
}

check_failed_units() {
  local failed
  failed="$(systemctl list-units --state=failed --no-legend --plain 2>/dev/null \
    | awk '{print $1}' | paste -sd, -)"
  [ -z "${failed}" ] || add_issue "systemd:failed:${failed}" \
    "failed systemd units: ${failed}" \
    "存在失败的 systemd 服务：${failed}"
}

check_mysql() {
  mysqladmin --protocol=socket ping --silent >/dev/null 2>&1 || add_issue "mysql:ping" \
    "MySQL did not answer mysqladmin ping" "MySQL 数据库连接检查失败"
}

check_docker_containers() {
  local container state
  for container in ${DOCKER_CONTAINERS}; do
    state="$(docker inspect --format '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{end}}' \
      "${container}" 2>/dev/null || true)"
    [[ "${state}" == "running healthy" || "${state}" == "running" ]] || add_issue \
      "docker:${container}:${state:-missing}" \
      "Docker container ${container} is not healthy (${state:-not found})" \
      "Docker 容器 ${container} 状态异常（${state:-未找到}）"
  done
}

http_result() {
  local body="$1" url="$2" mode="$3"
  if [ "${mode}" = code ]; then
    curl --silent --show-error --max-time "${HTTP_TIMEOUT_SECONDS}" \
      --output /dev/null --write-out '%{http_code} %{time_total}\n' "${url}"
  else
    curl --silent --show-error --max-time "${HTTP_TIMEOUT_SECONDS}" \
      --max-filesize 65536 --output "${body}" \
      --write-out '%{http_code} %{time_total}\n' "${url}"
  fi
}

check_http_probe() {
  local name="$1" mode="$2" url="$3" codes="$4" body result status elapsed
  body="$(mktemp)"
  if ! result="$(http_result "${body}" "${url}" "${mode}")"; then
    rm -f "${body}"
    add_issue "http:${name}:request" "${name} request failed" "${name} 请求失败"
    return
  fi
  read -r status elapsed <<< "${result}"
  if [[ ",${codes}," != *",${status},"* ]]; then
    add_issue "http:${name}:status:${status}" "${name} returned HTTP ${status}" "${name} 返回 HTTP ${status}"
  elif [ "${mode}" = json-up ] && ! grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' "${body}"; then
    add_issue "http:${name}:not-up" "${name} health is not UP" "${name} 健康状态不是 UP"
  elif [ "${mode}" = json-success ] && ! grep -Eq '"success"[[:space:]]*:[[:space:]]*true' "${body}"; then
    add_issue "http:${name}:unsuccessful" "${name} health response is unsuccessful" "${name} 健康接口返回失败"
  fi
  awk -v actual="${elapsed}" -v limit="${HTTP_MAX_SECONDS}" 'BEGIN { exit !(actual > limit) }' && \
    add_issue "http:${name}:slow" "${name} response took ${elapsed}s" "${name} 响应耗时 ${elapsed} 秒"
  rm -f "${body}"
}

check_http_probes() {
  local name mode url codes
  while IFS='|' read -r name mode url codes; do
    [ -n "${name}" ] || continue
    check_http_probe "${name}" "${mode}" "${url}" "${codes}"
  done <<< "${HTTP_PROBES}"
}

check_certificates() {
  local name certificate
  while IFS='|' read -r name certificate; do
    [ -n "${name}" ] || continue
    if [ ! -r "${certificate}" ]; then
      add_issue "certificate:${name}:unreadable" "${name} certificate is not readable" "${name} 证书无法读取"
    elif ! openssl x509 -checkend "$((CERT_MIN_DAYS * 86400))" -noout \
      -in "${certificate}" >/dev/null 2>&1; then
      add_issue "certificate:${name}:expiring" "${name} certificate expires within ${CERT_MIN_DAYS} days" \
        "${name} 证书将在 ${CERT_MIN_DAYS} 天内到期"
    fi
  done <<< "${CERTIFICATES}"
}

check_host_resources() {
  local cpu_count load15 total available memory_available_percent disk_use inode_use
  cpu_count="$(getconf _NPROCESSORS_ONLN)"
  load15="$(awk '{print $3}' /proc/loadavg)"
  awk -v current_load="${load15}" -v cpus="${cpu_count}" -v ratio="${MAX_LOAD_PER_CPU}" \
    'BEGIN { exit !(current_load > cpus * ratio) }' && add_issue "host:load" \
      "15-minute load ${load15} exceeds threshold" "15 分钟系统负载 ${load15} 超过阈值"
  total="$(awk '/^MemTotal:/ {print $2}' /proc/meminfo)"
  available="$(awk '/^MemAvailable:/ {print $2}' /proc/meminfo)"
  memory_available_percent=$((available * 100 / total))
  (( memory_available_percent >= MIN_MEMORY_AVAILABLE_PERCENT )) || add_issue "host:memory" \
    "available memory is ${memory_available_percent}%" "可用内存仅剩 ${memory_available_percent}%"
  disk_use="$(df -P / | awk 'NR == 2 {gsub(/%/, "", $5); print $5}')"
  (( disk_use < MAX_DISK_USED_PERCENT )) || add_issue "host:disk" \
    "root filesystem usage is ${disk_use}%" "根文件系统使用率已达 ${disk_use}%"
  inode_use="$(df -Pi / | awk 'NR == 2 {gsub(/%/, "", $5); print $5}')"
  (( inode_use < MAX_INODE_USED_PERCENT )) || add_issue "host:inodes" \
    "root filesystem inode usage is ${inode_use}%" "根文件系统 inode 使用率已达 ${inode_use}%"
  [ "$(timedatectl show -p NTPSynchronized --value 2>/dev/null)" = yes ] || add_issue "host:ntp" \
    "system clock is not synchronized" "服务器时间未同步"
}

latest_backup_dir() {
  find "$1" -mindepth 1 -maxdepth 1 -type d -name '????????-??????' \
    -printf '%T@ %p\n' 2>/dev/null | sort -nr | head -1 | cut -d' ' -f2-
}

check_backup_root() {
  local name="$1" root="$2" latest age_hours
  latest="$(latest_backup_dir "${root}")"
  if [ -z "${latest}" ]; then
    add_issue "backup:${name}:missing" "${name} has no completed backup" "${name} 未找到已完成的备份"
    return
  fi
  [ -f "${latest}/SHA256SUMS" ] || add_issue "backup:${name}:checksum" \
    "${name} latest backup has no checksum manifest" "${name} 最新备份缺少校验清单"
  age_hours=$(( ($(date +%s) - $(stat -c %Y "${latest}")) / 3600 ))
  (( age_hours <= MAX_BACKUP_AGE_HOURS )) || add_issue "backup:${name}:stale" \
    "${name} latest backup is ${age_hours} hours old" "${name} 最新备份距今已有 ${age_hours} 小时"
}

check_backups() {
  local name root
  while IFS='|' read -r name root; do
    [ -n "${name}" ] || continue
    check_backup_root "${name}" "${root}"
  done <<< "${BACKUP_ROOTS}"
}

check_remote_statuses() {
  local name file status completed epoch age_hours
  while IFS='|' read -r name file; do
    [ -n "${name}" ] || continue
    status="$(sed -n 's/^status=//p' "${file}" 2>/dev/null | head -1)"
    completed="$(sed -n 's/^completed_at=//p' "${file}" 2>/dev/null | head -1)"
    epoch="$(date -d "${completed}" +%s 2>/dev/null || true)"
    if [ "${status}" != SUCCESS ] || [[ ! "${epoch}" =~ ^[0-9]+$ ]]; then
      add_issue "remote:${name}:invalid" "${name} off-site sync status is invalid" "${name} 异地同步状态异常"
      continue
    fi
    age_hours=$(( ($(date +%s) - epoch) / 3600 ))
    (( age_hours <= MAX_REMOTE_SYNC_AGE_HOURS )) || add_issue "remote:${name}:stale" \
      "${name} off-site sync is ${age_hours} hours old" "${name} 异地同步距今已有 ${age_hours} 小时"
  done <<< "${REMOTE_STATUS_FILES}"
}

check_fresh_files() {
  local name file expected age_hours
  while IFS='|' read -r name file expected; do
    [ -n "${name}" ] || continue
    if [ ! -f "${file}" ]; then
      add_issue "result:${name}:missing" "${name} result file is missing" "${name} 结果文件不存在"
      continue
    fi
    age_hours=$(( ($(date +%s) - $(stat -c %Y "${file}")) / 3600 ))
    (( age_hours <= MAX_CHECK_FILE_AGE_HOURS )) || add_issue "result:${name}:stale" \
      "${name} result is ${age_hours} hours old" "${name} 结果距今已有 ${age_hours} 小时"
    tail -n 1 "${file}" | grep -Fq "${expected}" || add_issue "result:${name}:failed" \
      "${name} latest result did not pass" "${name} 最近一次检查未通过"
  done <<< "${FRESH_CHECK_FILES}"
}
