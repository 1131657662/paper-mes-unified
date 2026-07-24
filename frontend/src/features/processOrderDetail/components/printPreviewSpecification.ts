import type { FinishProductionVO, StageOutputVO } from '../../../types/processOrder'
import { formatGram, formatMm } from '../../../utils/numberFormatters'

export function printOutputSpec(output: StageOutputVO) {
  return specText(output.paperName, output.gramWeight, output.finishWidth)
}

export function printFinishSpec(finish: FinishProductionVO) {
  return specText(finish.paperName, finish.gramWeight, finish.finishWidth)
}

export function printTrimTitle(identifier?: string) {
  if (!identifier || identifier === '修边' || identifier === '切边') return '修边'
  return `修边 ${identifier}`
}

function specText(paperName?: string, gramWeight?: number, width?: number) {
  return [
    paperName || '-',
    gramWeight ? formatGram(gramWeight) : undefined,
    width ? formatMm(width) : undefined,
  ].filter(Boolean).join(' / ')
}
