#!/usr/bin/env python3
"""Build one structured server daily health snapshot."""

from __future__ import annotations

import json
import os
import socket
from datetime import datetime

from server_daily_report_common import report_days
from server_daily_report_operations import activity, backups, updates
from server_daily_report_system import certificates, containers, probes, resources, services


def main() -> None:
    target, previous = report_days()
    payload = {
        "schema": 1,
        "host": socket.gethostname(),
        "generated_at": datetime.now().astimezone().isoformat(),
        "report_date": target.isoformat(),
        "comparison_date": previous.isoformat(),
        "resources": resources(target, previous),
        "services": services(),
        "workloads": containers(),
        "probes": probes(),
        "certificates": certificates(),
        "activity": activity(target, previous),
        "backups": backups(),
        "updates": updates(),
    }
    print(json.dumps(payload, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    os.umask(0o077)
    main()
