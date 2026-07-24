import { PROCESS_MODE, STEP_TYPE } from '../../../constants/processOrder'
import type { RollProductionVO } from '../../../types/processOrder'
import { formatGram, formatMm } from '../../../utils/numberFormatters'
import { formatProductionKg } from '../orderDetailUtils'

export function printSourceItems(
  production: RollProductionVO,
): Array<{ label: string; value: string }> {
  const gramWeight = production.actualGramWeight ?? production.gramWeight
  const width = production.actualWidth ?? production.originalWidth
  const weight = production.actualWeight
    ?? (production.rollWeight ?? 0) * (production.pieceNum ?? 1)
  const gramText = `${formatGram(gramWeight)}${production.actualGramWeight == null ? '' : '（实）'}`
  const widthText = `${formatMm(width)}${production.actualWidth == null ? '' : '（实）'}`
  return [
    {
      label: '卷号/编号',
      value: [production.rollNo, production.extraNo].filter(Boolean).join(' / ') || '-',
    },
    { label: '品名', value: production.paperName || '-' },
    { label: '克重/门幅', value: `${gramText} / ${widthText}` },
    {
      label: production.actualWeight == null ? '标重' : '实重',
      value: formatProductionKg(weight, production),
    },
    { label: '方式', value: sourceProcessText(production) },
  ]
}

export function printRollTitle(
  seq: number,
  production: RollProductionVO,
  isMergeGroup: boolean,
) {
  if (isMergeGroup) return `合并复卷 ${seq}`
  return production.rollNo || production.extraNo || `母卷 ${seq}`
}

function sourceProcessText(production: RollProductionVO) {
  const mode = PROCESS_MODE[production.processMode ?? 1] ?? '-'
  if (production.processMode === 3 || production.processMode === 4) return mode
  return `${mode} / ${STEP_TYPE[production.mainStepType ?? 1] ?? '-'}`
}
