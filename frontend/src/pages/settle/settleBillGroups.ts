import type { SettlePrintLine } from '../../types/settle'

export interface SettleBillGroup {
  extraAmount: number
  extraFeeSummary?: string
  finishCount: number
  finishWeight: number
  key: string
  lineAmount: number
  orderDate?: string
  orderNo: string
  originalWeight: number
  processAmount: number
  trimWeight: number
  lines: SettlePrintLine[]
}

export function buildSettleBillGroups(lines: SettlePrintLine[]) {
  const map = new Map<string, SettleBillGroup>()
  for (const line of lines) {
    const key = line.orderUuid || line.orderNo || line.originalUuid
    const group = map.get(key) ?? emptyGroup(key, line)
    group.lines.push(line)
    group.originalWeight += line.originalWeight ?? 0
    group.finishCount += line.finishCount ?? 0
    group.finishWeight += line.finishWeight ?? 0
    group.trimWeight += line.trimWeight ?? 0
    group.processAmount += line.processAmount ?? 0
    group.extraAmount += line.extraAmount ?? 0
    group.extraFeeSummary ||= line.extraFeeSummary
    group.lineAmount += line.lineAmount ?? 0
    map.set(key, group)
  }
  return Array.from(map.values())
}

function emptyGroup(key: string, line: SettlePrintLine): SettleBillGroup {
  return {
    extraAmount: 0,
    extraFeeSummary: undefined,
    finishCount: 0,
    finishWeight: 0,
    key,
    lineAmount: 0,
    orderDate: line.orderDate,
    orderNo: line.orderNo || key,
    originalWeight: 0,
    processAmount: 0,
    trimWeight: 0,
    lines: [],
  }
}
