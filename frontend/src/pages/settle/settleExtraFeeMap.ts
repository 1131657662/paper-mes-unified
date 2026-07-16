import type { SettlePrintLine } from '../../types/settle'

export function buildExtraFeeByOrder(lines: SettlePrintLine[]) {
  const result: Record<string, string> = {}
  for (const line of lines) {
    if (line.orderUuid && line.extraFeeSummary && !result[line.orderUuid]) {
      result[line.orderUuid] = line.extraFeeSummary
    }
  }
  return result
}
