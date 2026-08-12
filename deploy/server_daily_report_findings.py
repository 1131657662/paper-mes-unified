#!/usr/bin/env python3
"""Derive concise daily report findings from collected facts."""

from __future__ import annotations


def _resource_findings(data: dict) -> list[tuple[str, str]]:
    resources = data["resources"]
    items = []
    if resources["disk_used_percent"] >= 85 or resources["inode_used_percent"] >= 85:
        items.append(("严重", "磁盘或 inode 使用率达到 85%，应立即清理或扩容。"))
    if resources["memory_available_percent"] < 15:
        items.append(("关注", "可用内存低于 15%，建议检查高占用进程。"))
    return items


def _availability_findings(data: dict) -> list[tuple[str, str]]:
    inactive = any(
        item.get("ActiveState") != "active" or item.get("SubState") != "running"
        for item in data["services"]["critical"]
    )
    items = []
    if data["services"]["failed"] or inactive or any(not item["ok"] for item in data["probes"]):
        items.append(("严重", "存在失败服务或健康探测异常，请优先检查。"))
    workloads = data["workloads"]
    query_failed = not workloads.get("docker_query_ok") or not workloads.get("pm2_query_ok")
    docker_failed = any(not item.get("status", "").startswith("Up") for item in workloads["docker"])
    pm2_failed = any(item.get("status") != "online" for item in workloads["pm2"])
    if query_failed or docker_failed or pm2_failed:
        items.append(("严重", "Docker 或 PM2 采集失败，或存在业务进程异常，请优先检查。"))
    return items


def _security_findings(data: dict) -> list[tuple[str, str]]:
    activity = data["activity"]
    items = []
    if activity["nginx"]["http_5xx"]:
        items.append(("关注", f"昨日产生 {activity['nginx']['http_5xx']} 次 HTTP 5xx。"))
    if activity["nginx"].get("suspicious", 0):
        items.append(("建议", f"昨日识别到 {activity['nginx']['suspicious']} 次敏感路径自动化探测，邮件不含来源或路径。"))
    if activity["ssh"]["failed"] >= 100:
        items.append(("关注", f"昨日 SSH 失败尝试 {activity['ssh']['failed']} 次，建议复核来源和访问策略。"))
    errors = sum(activity["service_errors"].values()) + sum(activity["application_errors"].values())
    if errors:
        items.append(("关注", f"昨日关键服务与应用共匹配到 {errors} 条错误关键词，请结合本机日志复核。"))
    if not activity["ufw"]["active"]:
        items.append(("建议", "UFW 当前未启用；服务器可能依赖云安全组，请确认入站规则保持最小开放。"))
    return items


def _backup_findings(data: dict) -> list[tuple[str, str]]:
    items = []
    maximum_age = data["backups"].get("max_age_hours", 48)
    for item in data["backups"]["sets"]:
        local, offsite = item["local"], item["offsite"]
        stale = local.get("age_hours", maximum_age + 1) > maximum_age
        stale = stale or offsite.get("age_hours") is None or offsite["age_hours"] > maximum_age
        if local.get("status") != "SUCCESS" or offsite.get("status") != "SUCCESS" or stale:
            items.append(("严重", f"{item['name']} 本地或异地备份状态异常。"))
    return items


def _maintenance_findings(data: dict) -> list[tuple[str, str]]:
    certs = data["certificates"]
    items = []
    if not certs:
        items.append(("严重", "未采集到任何 TLS 证书信息，请检查证书目录和日报读取权限。"))
    elif certs[0]["days"] < 30:
        items.append(("关注", f"证书 {certs[0]['name']} 仅剩 {certs[0]['days']} 天。"))
    if data["updates"]["security"]:
        items.append(("建议", f"存在 {data['updates']['security']} 个安全更新，建议安排维护窗口评估。"))
    return items


def findings(data: dict) -> list[tuple[str, str]]:
    return (
        _resource_findings(data)
        + _availability_findings(data)
        + _security_findings(data)
        + _backup_findings(data)
        + _maintenance_findings(data)
    )
