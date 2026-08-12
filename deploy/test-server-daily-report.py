#!/usr/bin/env python3
"""Behavior tests for the daily report HTML renderer."""

from __future__ import annotations

import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path
from unittest.mock import patch

from server_daily_report_system import _pm2_processes


ROOT = Path(__file__).resolve().parent
RENDERER = Path(os.getenv("DAILY_REPORT_RENDERER", ROOT / "render-server-daily-report.example.py"))


def fixture() -> dict:
    return {
        "host": "mes-host<script>",
        "generated_at": "2026-08-13T07:30:00+08:00",
        "report_date": "2026-08-12",
        "comparison_date": "2026-08-11",
        "resources": {"cpu_now_percent": 5.2, "cpu_yesterday_percent": 7.1, "cpu_trend": "较前日下降 1.20%", "memory_available": "11.0 GiB", "memory_available_percent": 72, "disk_used_percent": 27, "disk_available": "70.0 GiB", "inode_used_percent": 8, "load": ["0.1", "0.2", "0.3"], "swap_used": "0.0 B", "uptime_days": 98.2},
        "services": {"critical": [{"name": "paper-mes.service", "ActiveState": "active", "SubState": "running", "MainPID": "123", "NRestarts": "0"}], "running_count": 30, "failed": []},
        "probes": [{"name": "MES 生产", "ok": True, "http": "200", "seconds": "0.04"}],
        "workloads": {"docker_query_ok": True, "docker": [{"name": "jimureport", "status": "Up 2 days (healthy)"}], "pm2_query_ok": True, "pm2": [{"name": "wms-backend", "status": "online", "pid": 321, "restarts": 2}]},
        "activity": {"nginx": {"requests": 100, "http_4xx": 2, "http_5xx": 0, "suspicious": 3}, "nginx_previous": {"requests": 90, "http_4xx": 1, "http_5xx": 0, "suspicious": 1}, "ssh": {"accepted": 2, "failed": 4, "sudo_failed": 0}, "ssh_previous": {"accepted": 1, "failed": 3, "sudo_failed": 0}, "ufw": {"active": False, "blocked": 0}, "service_errors": {"paper-mes": 0}, "application_errors": {"MES 生产": 0}, "oom_events": 0, "reboots": 0},
        "backups": {"max_age_hours": 48, "sets": [{"name": "MES", "local": {"id": "20260812-023515", "size": "80.0 MiB", "age_hours": 12, "status": "SUCCESS"}, "offsite": {"completed_at": "2026-08-12T03:53:42Z", "age_hours": 11, "status": "SUCCESS"}}]},
        "certificates": [{"name": "mes.nbsmzwl.cn", "expires": "2026-10-04T15:40:39+08:00", "days": 52}],
        "updates": {"upgradable": 153, "security": 4, "reboot_required": False},
    }


def test_renderer_escapes_content_and_summarizes() -> None:
    with tempfile.TemporaryDirectory() as directory:
        source = Path(directory) / "report.json"
        target = Path(directory) / "report.html"
        source.write_text(json.dumps(fixture(), ensure_ascii=False), encoding="utf-8")
        subprocess.run([sys.executable, RENDERER, source, target], check=True)
        rendered = target.read_text(encoding="utf-8")
    assert "服务器每日运营与安全日报" in rendered
    assert "mes-host&lt;script&gt;" in rendered
    assert "服务器可能依赖云安全组" in rendered
    assert "可升级软件包：153" in rendered
    assert "不含 URL 和参数" in rendered
    assert "敏感路径自动化探测" in rendered
    assert "容器与业务进程" in rendered
    assert "wms-backend" in rendered
    assert "80.0 MiB" in rendered
    assert "结论：健康" in rendered


def test_renderer_marks_failed_probe_as_critical() -> None:
    data = fixture()
    data["probes"][0]["ok"] = False
    with tempfile.TemporaryDirectory() as directory:
        source = Path(directory) / "report.json"
        target = Path(directory) / "report.html"
        source.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
        subprocess.run([sys.executable, RENDERER, source, target], check=True)
        rendered = target.read_text(encoding="utf-8")
    assert "存在失败服务或健康探测异常" in rendered
    assert "结论：异常" in rendered


def test_renderer_marks_stale_backup_as_critical() -> None:
    data = fixture()
    data["backups"]["sets"][0]["offsite"]["age_hours"] = 72
    with tempfile.TemporaryDirectory() as directory:
        source = Path(directory) / "report.json"
        target = Path(directory) / "report.html"
        source.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
        subprocess.run([sys.executable, RENDERER, source, target], check=True)
        rendered = target.read_text(encoding="utf-8")
    assert "本地或异地备份状态异常" in rendered
    assert "结论：异常" in rendered


def test_renderer_marks_workload_collection_failure_as_critical() -> None:
    data = fixture()
    data["workloads"]["pm2_query_ok"] = False
    data["workloads"]["pm2"] = []
    with tempfile.TemporaryDirectory() as directory:
        source = Path(directory) / "report.json"
        target = Path(directory) / "report.html"
        source.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
        subprocess.run([sys.executable, RENDERER, source, target], check=True)
        rendered = target.read_text(encoding="utf-8")
    assert "Docker 或 PM2 采集失败" in rendered
    assert "无法读取业务进程状态" in rendered
    assert "结论：异常" in rendered


def test_pm2_processes_reports_live_and_missing_pids() -> None:
    with tempfile.TemporaryDirectory() as directory:
        root = Path(directory)
        pid_dir, process_root = root / "pids", root / "proc"
        pid_dir.mkdir()
        (process_root / "123").mkdir(parents=True)
        (process_root / "123" / "stat").write_text("process", encoding="utf-8")
        (pid_dir / "wms-backend-0.pid").write_text("123", encoding="utf-8")
        environment = {
            "REPORT_PM2_PID_DIR": str(pid_dir),
            "REPORT_PROCESS_ROOT": str(process_root),
            "REPORT_PM2_PROCESSES": "wms-backend warehouse-api",
        }
        with patch.dict(os.environ, environment):
            ok, rows = _pm2_processes()
    assert ok is True
    assert rows[0]["status"] == "online"
    assert rows[1]["status"] == "offline"


if __name__ == "__main__":
    test_renderer_escapes_content_and_summarizes()
    test_renderer_marks_failed_probe_as_critical()
    test_renderer_marks_stale_backup_as_critical()
    test_renderer_marks_workload_collection_failure_as_critical()
    test_pm2_processes_reports_live_and_missing_pids()
    print("server daily report renderer tests passed")
