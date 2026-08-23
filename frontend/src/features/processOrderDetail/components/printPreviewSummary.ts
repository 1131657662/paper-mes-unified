import type {
  FinishProductionVO,
  FinishRoll,
  ProcessOrderDetailVO,
  ProcessStep,
  RollProductionVO,
} from '../../../types/processOrder'
import { canonicalFinishEstimateWeights, weightFromCanonicalMap } from '../../../components/processOrder/shared/canonicalEstimateWeight'
import { formatTon } from '../orderDetailUtils'
import { isRollWeightKnown, rollTotalWeight } from '../routeConfigSource'
import { isProductionWeightKnown, productionSourceEstimateWeight } from '../productionSourceWeight'
import type { PrintSummaryItem } from './printPreviewTypes'

export function buildPrintSummary(detail: ProcessOrderDetailVO): PrintSummaryItem[] {
  const productions = detail.rollProductions ?? []
  const originals = originalTotals(detail, productions)
  const finals = finalTotals(detail, productions)
  return [
    { label: '原卷', value: `${originals.count} 卷 / ${weightText(originals.weight, originals.weightKnown)}` },
    { label: '最终成品', value: `${finals.count} 件 / ${weightText(finals.weight, finals.weightKnown)}` },
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
  const rewindWeightKnown = rewind.every(isProductionWeightKnown)
  if (saw.length) items.push({ label: '锯纸', value: `${saw.length} 卷 / ${knifeCount} 刀` })
  if (rewind.length) items.push({ label: '复卷', value: `${rewind.length} 卷 / ${weightText(rewindWeight, rewindWeightKnown)}` })
  if (direct.length) items.push({ label: '直发', value: `${direct.length} 卷` })
  if (service.length) items.push({ label: '附加工艺', value: `${service.length} 卷` })
  return items
}

function originalTotals(detail: ProcessOrderDetailVO, productions: RollProductionVO[]) {
  if (detail.originalRolls.length) {
    return {
      count: detail.originalRolls.length,
      weight: detail.originalRolls.reduce((sum, item) => sum + rollTotalWeight(item), 0),
      weightKnown: detail.originalRolls.every(isRollWeightKnown),
    }
  }
  return {
    count: productions.length,
    weight: productions.reduce((sum, item) => sum + sourceWeight(item), 0),
    weightKnown: productions.every(isProductionWeightKnown),
  }
}

function finalTotals(detail: ProcessOrderDetailVO, productions: RollProductionVO[]) {
  const finishes = uniqueFinishes(detail.finishRolls, productions)
  const estimates = new Map<string, number | undefined>()
  productions.forEach((production) => {
    canonicalFinishEstimateWeights({
      production,
      finishes: production.finishes,
      sourceProductions: productions,
    }).forEach((weight, uuid) => estimates.set(uuid, weight))
  })
  const missingDirects = productions.filter((item) => (
    item.processMode === 3
    && !(item.finishes ?? []).some(isDeliverable)
    && !hasDirectFinish(item, detail.finishRolls)
  ))
  const weightKnown = finishes.every((item) => {
    if (item.actualWeight != null && item.actualWeight > 0) return true
    if (estimates.has(item.uuid)) return estimates.get(item.uuid) != null
    return item.estimateWeight != null && item.estimateWeight >= 0
  })
    && missingDirects.every(isProductionWeightKnown)
  return {
    count: finishes.length + missingDirects.length,
    weight: finishes.reduce((sum, item) => sum + (weightFromCanonicalMap(estimates, item.uuid, item.estimateWeight) ?? 0), 0)
      + missingDirects.reduce((sum, item) => sum + sourceWeight(item), 0),
    weightKnown,
  }
}

function weightText(weight: number, known: boolean): string {
  return known ? formatTon(weight) : '待称重'
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
  if (production.actualWeight != null && production.actualWeight > 0) return production.actualWeight
  return productionSourceEstimateWeight(production)
}
