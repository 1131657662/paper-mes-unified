import { formatKg, formatTon } from '../../features/settle/utils/settleFormatters'
import type { OriginalWeightStatus, SettlePrintLine } from '../../types/settle'

interface OriginalWeightSummary {
  label: string
  value: string
}

export function formatSettleOriginalWeight(line: SettlePrintLine): string {
  if (line.originalWeightStatus === 'UNKNOWN') return '未知（待称重）'
  if (line.originalWeightStatus === 'ESTIMATED') {
    return line.originalWeight == null
      ? '参考重量缺失（未实测）'
      : `参考 ${formatKg(line.originalWeight)}（未实测）`
  }
  if (line.originalWeightStatus === 'MEASURED') {
    return line.originalWeight == null ? '实测重量缺失' : `实测 ${formatKg(line.originalWeight)}`
  }
  return formatKg(line.originalWeight)
}

export function summarizeSettleOriginalWeights(lines: SettlePrintLine[]): OriginalWeightSummary {
  const statuses = lines.map((line) => line.originalWeightStatus)
  const unknownCount = statuses.filter((status) => status === 'UNKNOWN').length
  const knownWeight = lines.reduce((sum, line) => (
    line.originalWeightStatus === 'UNKNOWN' ? sum : sum + (line.originalWeight ?? 0)
  ), 0)
  return {
    label: originalWeightLabel(statuses),
    value: originalWeightValue({ count: lines.length, knownWeight, unknownCount }),
  }
}

function originalWeightLabel(statuses: Array<OriginalWeightStatus | undefined>): string {
  if (statuses.length === 0 || statuses.every((status) => status == null)) return '原纸'
  if (statuses.every((status) => status === 'UNKNOWN')) return '原纸（未知）'
  if (statuses.some((status) => status === 'UNKNOWN')) return '原纸（含未知）'
  if (statuses.every((status) => status === 'MEASURED')) return '原纸（实测）'
  if (statuses.every((status) => status === 'ESTIMATED')) return '原纸（参考）'
  if (statuses.some((status) => status == null)) return '原纸（状态不完整）'
  return '原纸（含未实测）'
}

function originalWeightValue(options: {
  count: number
  knownWeight: number
  unknownCount: number
}): string {
  if (options.unknownCount === options.count && options.count > 0) {
    return `${options.count} 卷 / 重量全部待称重`
  }
  if (options.unknownCount > 0) {
    return `${options.count} 卷 / 已知 ${formatTon(options.knownWeight)}，${options.unknownCount} 卷待称重`
  }
  return `${options.count} 卷 / ${formatTon(options.knownWeight)}`
}
