#!/usr/bin/env python3
"""Collect host resource, service, container, HTTP and certificate facts."""

from __future__ import annotations

import glob
import json
import os
import re
import tempfile
import time
from datetime import date, datetime, timezone
from pathlib import Path

from server_daily_report_common import entries, human_bytes, read_text, run, trend


def _cpu_sample() -> float:
    def counters() -> tuple[int, int]:
        values = [int(value) for value in read_text("/proc/stat").splitlines()[0].split()[1:]]
        return sum(values), values[3] + values[4]

    total_before, idle_before = counters()
    time.sleep(float(os.getenv("CPU_SAMPLE_SECONDS", "1")))
    total_after, idle_after = counters()
    delta = max(1, total_after - total_before)
    return round(100 * (1 - (idle_after - idle_before) / delta), 2)


def _sar_cpu(day: date) -> float | None:
    path = f"/var/log/sysstat/sa{day.day:02d}"
    code, output = run(["sar", "-u", "-f", path])
    if code != 0:
        return None
    for line in reversed(output.splitlines()):
        if line.strip().startswith("Average:"):
            try:
                return round(100 - float(line.split()[-1]), 2)
            except ValueError:
                return None
    return None


def resources(day: date, previous: date) -> dict:
    memory = {
        line.split(":")[0]: int(line.split()[1]) * 1024
        for line in read_text("/proc/meminfo").splitlines()
        if ":" in line and line.split()[1].isdigit()
    }
    disk = os.statvfs("/")
    disk_used = round(100 * (disk.f_blocks - disk.f_bavail) / disk.f_blocks, 2)
    inode_used = round(100 * (disk.f_files - disk.f_favail) / disk.f_files, 2)
    cpu_day, cpu_previous = _sar_cpu(day), _sar_cpu(previous)
    return {
        "cpu_now_percent": _cpu_sample(),
        "cpu_yesterday_percent": cpu_day,
        "cpu_trend": trend(cpu_day, cpu_previous, "%") if cpu_day is not None else "sysstat 无数据",
        "load": read_text("/proc/loadavg").split()[:3],
        "memory_available": human_bytes(memory.get("MemAvailable", 0)),
        "memory_available_percent": round(100 * memory.get("MemAvailable", 0) / memory.get("MemTotal", 1), 2),
        "swap_used": human_bytes(memory.get("SwapTotal", 0) - memory.get("SwapFree", 0)),
        "disk_used_percent": disk_used,
        "disk_available": human_bytes(disk.f_bavail * disk.f_frsize),
        "inode_used_percent": inode_used,
        "uptime_days": round(float(read_text("/proc/uptime").split()[0]) / 86400, 1),
    }


def services() -> dict:
    defaults = "nginx.service mysql.service docker.service paper-mes.service paper-mes-test.service pm2-root.service"
    units = os.getenv("REPORT_SYSTEMD_UNITS", defaults).split()
    rows = []
    for unit in units:
        _, output = run(["systemctl", "show", unit, "--no-pager", "-p", "ActiveState", "-p", "SubState", "-p", "MainPID", "-p", "NRestarts"])
        values = dict(line.split("=", 1) for line in output.splitlines() if "=" in line)
        rows.append({"name": unit, **values})
    _, active = run(["systemctl", "list-units", "--type=service", "--state=running", "--no-legend", "--plain"])
    _, failed = run(["systemctl", "list-units", "--state=failed", "--no-legend", "--plain"])
    return {"critical": rows, "running_count": len(active.splitlines()), "failed": [line.split()[0] for line in failed.splitlines()]}


def _pm2_processes() -> tuple[bool, list[dict]]:
    root = Path(os.getenv("REPORT_PM2_PID_DIR", "/root/.pm2/pids"))
    process_root = Path(os.getenv("REPORT_PROCESS_ROOT", "/proc"))
    names = os.getenv("REPORT_PM2_PROCESSES", "wms-backend warehouse-api").split()
    if not root.is_dir() or any(not re.fullmatch(r"[A-Za-z0-9._-]+", name) for name in names):
        return False, []
    rows = []
    for name in names:
        candidates = list(root.glob(f"{name}-*.pid"))
        pid = read_text(candidates[0]).strip() if candidates else ""
        online = pid.isdigit() and (process_root / pid / "stat").is_file()
        rows.append({"name": name, "status": "online" if online else "offline", "pid": pid or "-", "restarts": "-"})
    return True, rows


def containers() -> dict:
    docker_code, docker_output = run(["docker", "ps", "-a", "--format", "{{json .}}"])
    docker_rows = []
    for line in docker_output.splitlines():
        try:
            item = json.loads(line)
            docker_rows.append({"name": item.get("Names"), "status": item.get("Status")})
        except json.JSONDecodeError:
            continue
    pm2_ok, pm2_rows = _pm2_processes()
    return {
        "docker": docker_rows,
        "docker_query_ok": docker_code == 0,
        "pm2": pm2_rows,
        "pm2_query_ok": pm2_ok,
    }


def probes() -> list[dict]:
    defaults = [
        "MES 生产|http://127.0.0.1:8081/actuator/health|200|\"status\":\"UP\"",
        "MES 测试|http://127.0.0.1:8082/actuator/health|200|\"status\":\"UP\"",
        "WMS 后端|http://127.0.0.1:3000/api/health/status|401|",
        "卷筒纸扫描|http://127.0.0.1:3001/api/health|200|\"success\":true",
        "JimuReport|http://127.0.0.1:8085/|200|",
    ]
    results = []
    for name, url, expected, token in entries("REPORT_HTTP_PROBES", defaults):
        with tempfile.NamedTemporaryFile() as body_file:
            code, output = run(["curl", "-sS", "--max-time", "10", "-o", body_file.name, "-w", "%{http_code} %{time_total}", url])
            body = read_text(body_file.name)[:65536]
        parts = output.split()
        actual = parts[0] if parts else "000"
        elapsed = parts[1] if len(parts) > 1 else "-"
        results.append({"name": name, "ok": code == 0 and actual == expected and (not token or token in body), "http": actual, "seconds": elapsed})
    return results


def certificates() -> list[dict]:
    rows = []
    for path in glob.glob("/etc/letsencrypt/live/*/fullchain.pem"):
        code, output = run(["openssl", "x509", "-enddate", "-noout", "-in", path])
        if code != 0 or "=" not in output:
            continue
        expires = datetime.strptime(output.split("=", 1)[1], "%b %d %H:%M:%S %Y %Z")
        expires = expires.replace(tzinfo=timezone.utc).astimezone()
        rows.append({"name": Path(path).parent.name, "expires": expires.isoformat(), "days": (expires.date() - date.today()).days})
    return sorted(rows, key=lambda item: item["days"])
