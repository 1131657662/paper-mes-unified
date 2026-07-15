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
  taxAmount: number
  trimWeight: number
  lines: SettlePrintLine[]
}

export function buildSettleBillGroups(lines: SettlePrintLine[]): SettleBillGroup[] {
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
    group.taxAmount += lineTaxAmount(line)
    group.extraAmount += line.extraAmount ?? 0
    group.extraFeeSummary ||= line.extraFeeSummary
    group.lineAmount += line.lineAmount ?? 0
    map.set(key, group)
  }
  return Array.from(map.values())
}

function lineTaxAmount(line: SettlePrintLine) {
  if ((line.taxAmount ?? 0) > 0) return line.taxAmount ?? 0
  const feeTax = (line.feeLines ?? [])
    .filter((fee) => fee.feeType === 'tax')
    .reduce((sum, fee) => sum + (fee.taxAmount ?? fee.amountTax ?? 0), 0)
  if (feeTax > 0) return feeTax
  return Math.max(0, (line.lineAmount ?? 0) - (line.processAmount ?? 0) - (line.extraAmount ?? 0))
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
    taxAmount: 0,
    trimWeight: 0,
    lines: [],
  }
}
