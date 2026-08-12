#!/usr/bin/env python3
"""Collect operational, backup, security and update summaries."""

from __future__ import annotations

import os
import re
from datetime import date, datetime
from pathlib import Path

from server_daily_report_common import age_hours, human_bytes, journal_count, matching_lines, read_text, run


def _nginx(day: date) -> dict:
    token = day.strftime("%d/%b/%Y")
    logs = ["var/log/nginx/*access*.log*"]
    total = matching_lines(logs, token, r'"[A-Z]+ [^\"]+ HTTP/[^\"]+" [1-5][0-9]{2} ')
    errors_4xx = matching_lines(logs, token, r'"[A-Z]+ [^\"]+ HTTP/[^\"]+" 4[0-9]{2} ')
    errors_5xx = matching_lines(logs, token, r'"[A-Z]+ [^\"]+ HTTP/[^\"]+" 5[0-9]{2} ')
    suspicious = matching_lines(logs, token, r'"[A-Z]+ /(\.env|\.git/|wp-login\.php|server-status|phpunit|actuator(?:/|\s))')
    return {"requests": total, "http_4xx": errors_4xx, "http_5xx": errors_5xx, "suspicious": suspicious}


def _ssh(day: date) -> dict:
    token = day.isoformat()
    logs = ["var/log/auth.log", "var/log/auth.log.1", "var/log/auth.log.*.gz"]
    failed = matching_lines(logs, token, r"Failed password|Invalid user|authentication failure")
    accepted = matching_lines(logs, token, r"Accepted (publickey|password)")
    sudo_failed = matching_lines(logs, token, r"authentication failure.*sudo|sudo.*incorrect password")
    return {"failed": failed, "accepted": accepted, "sudo_failed": sudo_failed}


def _ufw(day: date) -> dict:
    code, status = run(["ufw", "status"])
    active = code == 0 and status.startswith("Status: active")
    token = day.strftime("%Y-%m-%d")
    blocked = matching_lines(["var/log/ufw.log*"], token, r"\[UFW BLOCK\]") if active else 0
    return {"active": active, "blocked": blocked}


def activity(day: date, previous: date) -> dict:
    current_web, previous_web = _nginx(day), _nginx(previous)
    current_ssh, previous_ssh = _ssh(day), _ssh(previous)
    service_units = os.getenv("REPORT_ERROR_UNITS", "nginx paper-mes paper-mes-test mysql docker pm2-root").split()
    service_errors = {unit: journal_count(unit, day, r"error|exception|failed|critical") for unit in service_units}
    app_errors = {
        "MES 生产": matching_lines(["var/log/paper-mes/app.log*"], day.isoformat(), r"error|exception|critical"),
        "MES 测试": matching_lines(["var/log/paper-mes-test/app.log*"], day.isoformat(), r"error|exception|critical"),
    }
    return {
        "nginx": current_web,
        "nginx_previous": previous_web,
        "ssh": current_ssh,
        "ssh_previous": previous_ssh,
        "ufw": _ufw(day),
        "service_errors": service_errors,
        "application_errors": app_errors,
        "oom_events": journal_count("systemd-oomd", day, r"killed|memory pressure"),
        "reboots": journal_count("systemd-logind", day, r"system is rebooting|powering off"),
    }


def _latest_backup(root: str) -> dict:
    base = Path(root)
    candidates = sorted(
        (path for path in base.glob("????????-??????") if path.is_dir()),
        key=lambda path: path.stat().st_mtime,
        reverse=True,
    )
    if not candidates:
        return {"root": root, "status": "MISSING"}
    latest = candidates[0]
    size = sum(path.stat().st_size for path in latest.rglob("*") if path.is_file())
    checksum = latest / "SHA256SUMS"
    return {
        "root": root,
        "id": latest.name,
        "age_hours": age_hours(int(latest.stat().st_mtime)),
        "size": human_bytes(size),
        "checksum_manifest": checksum.is_file(),
        "status": "SUCCESS" if checksum.is_file() else "INCOMPLETE",
    }


def _remote_status(path: str) -> dict:
    values = dict(
        line.split("=", 1) for line in read_text(path).splitlines() if "=" in line
    )
    completed = values.get("completed_at", "")
    try:
        timestamp = datetime.fromisoformat(completed.replace("Z", "+00:00")).timestamp()
        age = age_hours(int(timestamp))
    except ValueError:
        age = None
    return {"status": values.get("status", "MISSING"), "completed_at": completed or "-", "age_hours": age, "remote": values.get("remote_name", "-")}


def backups() -> dict:
    roots = [
        ("MES", "/opt/backups/paper-mes", "/opt/backups/paper-mes/.remote-sync-status"),
        ("业务项目", "/opt/backups/business-projects", "/opt/backups/business-projects/.remote-sync-status"),
    ]
    rows = []
    for name, root, remote in roots:
        rows.append({"name": name, "local": _latest_backup(root), "offsite": _remote_status(remote)})
    _, timer = run(["systemctl", "show", "paper-mes-offsite-backup.service", "-p", "Result", "-p", "ExecMainStatus"])
    return {
        "sets": rows,
        "max_age_hours": int(os.getenv("MAX_BACKUP_AGE_HOURS", "48")),
        "timer": dict(line.split("=", 1) for line in timer.splitlines() if "=" in line),
    }


def updates() -> dict:
    code, output = run(["apt-get", "-s", "upgrade"], timeout=120)
    match = re.search(r"(\d+) upgraded, (\d+) newly installed, (\d+) to remove and (\d+) not upgraded", output)
    counts = [int(value) for value in match.groups()] if code == 0 and match else [None] * 4
    _, upgradable = run(["apt", "list", "--upgradable"], timeout=120)
    security = sum(1 for line in upgradable.splitlines() if "security" in line.lower())
    return {
        "upgradable": counts[0],
        "new": counts[1],
        "remove": counts[2],
        "held": counts[3],
        "security": security,
        "reboot_required": Path("/var/run/reboot-required").exists(),
        "kernel": os.uname().release,
    }
