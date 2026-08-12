#!/usr/bin/env python3
"""Render a structured server snapshot as a concise Chinese HTML email."""

from __future__ import annotations

import html
import json
import sys
from pathlib import Path
from typing import Any

from server_daily_report_common import trend
from server_daily_report_findings import findings


def esc(value: Any) -> str:
    return html.escape(str(value))


def status(ok: bool) -> str:
    return '<span class="ok">正常</span>' if ok else '<span class="bad">异常</span>'


def table(headers: list[str], rows: list[list[Any]]) -> str:
    heading = "".join(f"<th>{esc(item)}</th>" for item in headers)
    body = "".join("<tr>" + "".join(f"<td>{item}</td>" for item in row) + "</tr>" for row in rows)
    return f"<table><thead><tr>{heading}</tr></thead><tbody>{body}</tbody></table>"


def resource_section(data: dict) -> str:
    item = data["resources"]
    rows = [[
        f"{esc(item['cpu_now_percent'])}%",
        esc(item.get("cpu_yesterday_percent") if item.get("cpu_yesterday_percent") is not None else "-"),
        esc(item["cpu_trend"]),
        f"{esc(item['memory_available'])} ({esc(item['memory_available_percent'])}%)",
        f"{esc(item['disk_used_percent'])}% / {esc(item['disk_available'])}",
        f"{esc(item['inode_used_percent'])}%",
        esc(" / ".join(item["load"])),
        esc(item["swap_used"]),
        esc(item["uptime_days"]),
    ]]
    return section("系统资源", table(["CPU 当前", "昨日均值", "CPU 趋势", "可用内存", "磁盘", "inode", "负载 1/5/15m", "Swap 已用", "运行天数"], rows))


def service_section(data: dict) -> str:
    rows = []
    for item in data["services"]["critical"]:
        ok = item.get("ActiveState") == "active" and item.get("SubState") == "running"
        rows.append([esc(item["name"]), status(ok), esc(item.get("MainPID", "-")), esc(item.get("NRestarts", "-"))])
    for item in data["probes"]:
        rows.append([esc(item["name"]), status(item["ok"]), "HTTP " + esc(item["http"]), esc(item["seconds"]) + "s"])
    note = f"<p>运行中的 systemd 服务：{esc(data['services']['running_count'])} 个；失败单元：{esc(', '.join(data['services']['failed']) or '无')}。</p>"
    return section("关键服务与探测", note + table(["服务", "状态", "PID / HTTP", "重启 / 耗时"], rows))


def workload_section(data: dict) -> str:
    rows = []
    if not data["workloads"].get("docker_query_ok"):
        rows.append(["Docker", "采集命令", status(False), "无法读取容器状态"])
    if not data["workloads"].get("pm2_query_ok"):
        rows.append(["PM2", "采集命令", status(False), "无法读取业务进程状态"])
    for item in data["workloads"]["docker"]:
        ok = item.get("status", "").startswith("Up")
        rows.append(["Docker", esc(item.get("name", "-")), status(ok), esc(item.get("status", "-"))])
    for item in data["workloads"]["pm2"]:
        ok = item.get("status") == "online"
        detail = f"PID {item.get('pid', '-')} / 累计重启 {item.get('restarts', '-')}"
        rows.append(["PM2", esc(item.get("name", "-")), status(ok), esc(detail)])
    if not rows:
        rows.append(["-", "未发现容器或 PM2 进程", status(True), "-"])
    return section("容器与业务进程", table(["运行方式", "名称", "状态", "详情"], rows))


def activity_section(data: dict) -> str:
    current, previous = data["activity"], data["activity"]["nginx_previous"]
    nginx = current["nginx"]
    ssh, ssh_previous = current["ssh"], current["ssh_previous"]
    rows = [
        ["Nginx 请求", esc(nginx["requests"]), esc(trend(nginx["requests"], previous["requests"])), "仅汇总状态码"],
        ["HTTP 4xx / 5xx", f"{esc(nginx['http_4xx'])} / {esc(nginx['http_5xx'])}", f"前日 {esc(previous['http_4xx'])} / {esc(previous['http_5xx'])}", "不含 URL 和参数"],
        ["SSH 成功 / 失败", f"{esc(ssh['accepted'])} / {esc(ssh['failed'])}", f"前日 {esc(ssh_previous['accepted'])} / {esc(ssh_previous['failed'])}", "不含 IP 与用户名"],
        ["UFW", "已启用" if current["ufw"]["active"] else "未启用", esc(current["ufw"]["blocked"]), "云安全组需另行核对"],
    ]
    rows.append(["敏感路径自动化探测", esc(nginx.get("suspicious", 0)), f"前日 {esc(previous.get('suspicious', 0))}", "不含来源 IP 和路径明细"])
    error_sources = {**current["service_errors"], **current.get("application_errors", {})}
    errors = sum(error_sources.values())
    detail = "；".join(f"{esc(name)}: {esc(count)}" for name, count in error_sources.items() if count)
    summary = f"关键服务与应用错误关键词计数：{esc(errors)}"
    if detail:
        summary += f"（{detail}）"
    summary += f"；OOM 事件：{esc(current['oom_events'])}；重启/关机事件：{esc(current['reboots'])}。"
    return section("运营与安全摘要", f"<p>{summary}</p>" + table(["指标", "昨日", "对比", "说明"], rows))


def backup_section(data: dict) -> str:
    rows = []
    for item in data["backups"]["sets"]:
        local, remote = item["local"], item["offsite"]
        rows.append([esc(item["name"]), esc(local.get("id", "-")), esc(local.get("size", "-")), esc(local.get("age_hours", "-")), status(local.get("status") == "SUCCESS"), esc(remote.get("completed_at", "-")), esc(remote.get("age_hours", "-")), status(remote.get("status") == "SUCCESS")])
    note = f"<p>本地与异地备份允许的最大年龄：{esc(data['backups'].get('max_age_hours', 48))} 小时。</p>"
    headers = ["范围", "本地备份", "大小", "年龄(h)", "本地状态", "异地完成时间", "年龄(h)", "异地状态"]
    return section("备份与异地同步", note + table(headers, rows))


def certificate_section(data: dict) -> str:
    rows = [[esc(item["name"]), esc(item["expires"]), esc(item["days"])] for item in data["certificates"]]
    updates = data["updates"]
    note = f"<p>可升级软件包：{esc(updates['upgradable'])}；安全更新：{esc(updates['security'])}；需要重启：{'是' if updates['reboot_required'] else '否'}。</p>"
    return section("证书与系统更新", note + table(["证书", "到期时间", "剩余天数"], rows))


def section(title: str, content: str) -> str:
    return f"<section><h2>{esc(title)}</h2>{content}</section>"


def render(data: dict) -> str:
    issues = findings(data)
    levels = {level for level, _ in issues}
    overall = "异常" if "严重" in levels else "需要关注" if "关注" in levels else "健康"
    cards = "".join(f'<li><strong>{esc(level)}</strong> {esc(message)}</li>' for level, message in issues) or "<li>未发现需要处理的异常。</li>"
    style = "body{font:14px/1.6 Arial,'Microsoft YaHei',sans-serif;color:#1f2328;background:#f6f8fa;margin:0;padding:24px}.wrap{max-width:980px;margin:auto;background:#fff;border:1px solid #d0d7de}.head{padding:22px 26px;background:#17212b;color:#fff}.head h1{margin:0;font-size:22px}.head p{margin:6px 0 0;color:#c9d1d9}section{padding:18px 26px;border-top:1px solid #d8dee4}h2{font-size:17px;margin:0 0 12px}table{width:100%;border-collapse:collapse}th,td{padding:8px;border:1px solid #d8dee4;text-align:left;vertical-align:top}th{background:#f0f3f6}.ok{color:#116329;font-weight:bold}.bad{color:#cf222e;font-weight:bold}.foot{padding:16px 26px;color:#57606a;background:#f6f8fa}ul{margin:0;padding-left:22px}"
    return f"<!doctype html><html lang='zh-CN'><head><meta charset='utf-8'><style>{style}</style></head><body><div class='wrap'><div class='head'><h1>服务器每日运营与安全日报 · {esc(data['report_date'])}</h1><p>{esc(data['host'])} · 结论：{overall} · 生成时间 {esc(data['generated_at'])}</p></div>{section('结论与建议','<ul>'+cards+'</ul>')}{resource_section(data)}{service_section(data)}{workload_section(data)}{activity_section(data)}{backup_section(data)}{certificate_section(data)}<div class='foot'>统计窗口：{esc(data['report_date'])} 00:00–24:00；日报是健康快照，不替代实时告警。敏感请求内容、业务数据和凭据未纳入邮件。</div></div></body></html>"


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("usage: render INPUT.json OUTPUT.html")
    data = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    Path(sys.argv[2]).write_text(render(data), encoding="utf-8")


if __name__ == "__main__":
    main()
