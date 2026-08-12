#!/usr/bin/env python3
"""Shared, side-effect-light helpers for the server daily report."""

from __future__ import annotations

import gzip
import os
import re
import subprocess
from datetime import date, datetime, timedelta
from pathlib import Path
from typing import Iterable


def run(command: list[str], timeout: int = 30) -> tuple[int, str]:
    env = os.environ.copy()
    env["LC_ALL"] = "C"
    try:
        result = subprocess.run(
            command, capture_output=True, text=True, timeout=timeout, env=env, check=False
        )
    except (OSError, subprocess.TimeoutExpired) as error:
        return 127, str(error)
    return result.returncode, (result.stdout or result.stderr).strip()


def report_days() -> tuple[date, date]:
    requested = os.getenv("REPORT_DATE")
    target = date.fromisoformat(requested) if requested else date.today() - timedelta(days=1)
    return target, target - timedelta(days=1)


def period(day: date) -> tuple[str, str]:
    start = datetime.combine(day, datetime.min.time()).astimezone()
    end = start + timedelta(days=1)
    return start.isoformat(), end.isoformat()


def entries(name: str, defaults: Iterable[str]) -> list[list[str]]:
    raw = os.getenv(name, "\n".join(defaults))
    return [line.split("|") for line in raw.splitlines() if line.strip()]


def read_text(path: str | Path) -> str:
    try:
        return Path(path).read_text(encoding="utf-8", errors="replace")
    except OSError:
        return ""


def matching_lines(patterns: Iterable[str], token: str, expression: str) -> int:
    matcher = re.compile(expression, re.IGNORECASE)
    total = 0
    for pattern in patterns:
        for path in Path("/").glob(pattern.lstrip("/")):
            try:
                opener = gzip.open if path.suffix == ".gz" else open
                with opener(path, "rt", encoding="utf-8", errors="replace") as handle:
                    total += sum(1 for line in handle if token in line and matcher.search(line))
            except OSError:
                continue
    return total


def journal_count(unit: str, day: date, expression: str) -> int:
    start, end = period(day)
    code, output = run(
        ["journalctl", "-u", unit, "--since", start, "--until", end, "--no-pager", "-o", "cat"]
    )
    if code != 0:
        return 0
    return sum(1 for line in output.splitlines() if re.search(expression, line, re.IGNORECASE))


def human_bytes(value: int) -> str:
    amount = float(value)
    for unit in ("B", "KiB", "MiB", "GiB", "TiB"):
        if amount < 1024 or unit == "TiB":
            return f"{amount:.1f} {unit}"
        amount /= 1024
    return f"{value} B"


def age_hours(epoch: int) -> int:
    return max(0, int((datetime.now().timestamp() - epoch) // 3600))


def trend(current: float, previous: float | None, suffix: str = "") -> str:
    if previous is None:
        return "暂无前日基线"
    delta = current - previous
    if abs(delta) < 0.01:
        return "与前日持平"
    direction = "上升" if delta > 0 else "下降"
    return f"较前日{direction} {abs(delta):.2f}{suffix}"
