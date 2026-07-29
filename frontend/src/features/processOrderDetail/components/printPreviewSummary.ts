import type {
  FinishProductionVO,
  FinishRoll,
  ProcessOrderDetailVO,
  ProcessStep,
  RollProductionVO,
} from '../../../types/processOrder'
import { formatTon } from '../orderDetailUtils'
import type { PrintSummaryItem } from './printPreviewTypes'

export function buildPrintSummary(detail: ProcessOrderDetailVO): PrintSummaryItem[] {
  const productions = detail.rollProductions ?? []
  const originals = originalTotals(detail, productions)
  const finals = finalTotals(detail, productions)
  return [
    { label: '原卷', value: `${originals.count} 卷 / ${formatTon(originals.weight)}` },
    { label: '最终成品', value: `${finals.count} 件 / ${formatTon(finals.weight)}` },
    ...processSummary(detail, productions),
  ]
}

function processSummary(
  detail: ProcessOrderDetailVO,
  productions: RollProductionVO[],
): PrintSummaryItem[] {
  const items: PrintSummaryItem[] = []
  const saw = productions.filter((item) => hasProcess(item, 1))
  const rewind = productions.filter((item) => hasProcess(item, 2))
  const direct = productions.filter((item) => item.processMode === 3)
  const service = productions.filter((item) => item.processMode === 4)
  const steps = uniqueSteps(detail, productions)
  const knifeCount = detail.order.actualTotalKnife ?? sumStepValue(steps, 1, 'knifeCount')
  const rewindWeight = rewind.reduce((sum, production) => sum + sourceWeight(production), 0)
  if (saw.length) items.push({ label: '锯纸', value: `${saw.length} 卷 / ${knifeCount} 刀` })
  if (rewind.length) items.push({ label: '复卷', value: `${rewind.length} 卷 / ${formatTon(rewindWeight)}` })
  if (direct.length) items.push({ label: '直发', value: `${direct.length} 卷` })
  if (service.length) items.push({ label: '附加工艺', value: `${service.length} 卷` })
  return items
}

function originalTotals(detail: ProcessOrderDetailVO, productions: RollProductionVO[]) {
  if (detail.originalRolls.length) {
    return {
      count: detail.originalRolls.length,
      weight: detail.originalRolls.reduce((sum, item) => (
        sum + (item.actualWeight ?? item.totalWeight ?? (item.rollWeight ?? 0) * (item.pieceNum ?? 1))
      ), 0),
    }
  }
  return {
    count: productions.length,
    weight: productions.reduce((sum, item) => sum + sourceWeight(item), 0),
  }
}

function finalTotals(detail: ProcessOrderDetailVO, productions: RollProductionVO[]) {
  const finishes = uniqueFinishes(detail.finishRolls, productions)
  const missingDirects = productions.filter((item) => (
    item.processMode === 3
    && !(item.finishes ?? []).some(isDeliverable)
    && !hasDirectFinish(item, detail.finishRolls)
  ))
  return {
    count: finishes.length + missingDirects.length,
    weight: finishes.reduce((sum, item) => sum + (item.estimateWeight ?? 0), 0)
      + missingDirects.reduce((sum, item) => sum + sourceWeight(item), 0),
  }
}

function uniqueFinishes(
  detailFinishes: FinishRoll[],
  productions: RollProductionVO[],
): Array<FinishRoll | FinishProductionVO> {
  const finishes = new Map<string, FinishRoll | FinishProductionVO>()
  detailFinishes.filter(isDeliverable).forEach((item) => finishes.set(item.uuid, item))
  productions.flatMap((item) => item.finishes ?? [])
    .filter(isDeliverable)
    .forEach((item) => {
      if (!finishes.has(item.uuid)) finishes.set(item.uuid, item)
    })
  return Array.from(finishes.values())
}

function uniqueSteps(detail: ProcessOrderDetailVO, productions: RollProductionVO[]) {
  const steps = new Map<string, ProcessStep>()
  detail.steps.forEach((step) => steps.set(step.uuid, step))
  productions.flatMap((item) => item.steps ?? []).forEach((step) => steps.set(step.uuid, step))
  return Array.from(steps.values())
}

function sumStepValue(
  steps: ProcessStep[],
  stepType: number,
  field: 'knifeCount' | 'processWeight',
) {
  return steps.reduce((sum, step) => (
    step.stepType === stepType ? sum + (step[field] ?? 0) : sum
  ), 0)
}

function hasProcess(production: RollProductionVO, stepType: number) {
  if (production.processMode === 3 || production.processMode === 4) return false
  return production.mainStepType === stepType
    || (production.steps ?? []).some((step) => step.stepType === stepType)
}

function hasDirectFinish(production: RollProductionVO, finishes: FinishRoll[]) {
  const rollNo = production.rollNo || production.extraNo
  return finishes.some((finish) => (
    isDeliverable(finish) && finish.sourceType === 2 && finish.finishRollNo === rollNo
  ))
}

function isDeliverable(item: { isSpare?: number; isRemain?: number; rollNoStatus?: number }) {
  return item.isSpare !== 1 && item.isRemain !== 1 && item.rollNoStatus !== 3
}

function sourceWeight(production: RollProductionVO) {
  return production.actualWeight
    ?? (production.rollWeight ?? 0) * (production.pieceNum ?? 1)
}
