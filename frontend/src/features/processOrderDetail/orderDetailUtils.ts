import type {
  FinishRoll,
  FinishProductionVO,
  OriginalRoll,
  ProcessOrder,
  ProcessOrderDetailVO,
  RollProductionVO,
} from '../../types/processOrder'
import {
  canonicalFinishEstimateWeights,
  canonicalFinishEstimateWeight,
  weightFromCanonicalMap,
} from '../../components/processOrder/shared/canonicalEstimateWeight'
import { isRollWeightKnown } from './routeConfigSource'
import {
  decimalPlaces,
  formatNumber as formatSharedNumber,
  formatKgWithMaxDecimals,
  formatOptionalKg,
  formatOptionalMoney,
  formatOptionalTonFromKg,
} from '../../utils/numberFormatters'
import { roundWeightTotal } from '../../utils/integerWeightAllocation'

const isActiveFinish = (finish: FinishRoll | FinishProductionVO) => finish.rollNoStatus !== 3
const isRemainFinish = (finish: FinishRoll | FinishProductionVO) => finish.isRemain === 1
const isDeliverableFinish = (finish: FinishRoll | FinishProductionVO) => (
  isActiveFinish(finish) && finish.isSpare !== 1 && !isRemainFinish(finish)
)

export interface DetailMetrics {
  rollCount: number
  finishCount: number
  spareCount: number
  totalOriginalWeight: number
  totalEstimateWeight: number
  totalActualWeight: number
  knifeCount: number
  stepCount: number
  processLabel: string
}

export function buildDetailMetrics(detail?: ProcessOrderDetailVO): DetailMetrics {
  const rolls = detail?.originalRolls ?? []
  const finishes = detail?.finishRolls ?? []
  const productions = detail?.rollProductions ?? []

  return {
    rollCount: rolls.length,
    finishCount: countOfficialFinishes(finishes),
    spareCount: finishes.filter((f) => f.isSpare === 1 && isActiveFinish(f)).length,
    totalOriginalWeight: sumOriginalWeight(rolls),
    totalEstimateWeight: sumEstimateWeight(detail),
    totalActualWeight: sumFinishes(finishes, 'actualWeight'),
    knifeCount: sumKnifeCount(productions),
    stepCount: detail?.steps?.length ?? 0,
    processLabel: processLabel(detail?.order, productions),
  }
}

export function resolveFinishEstimateWeight(
  finish: FinishProductionVO,
  finishes: FinishProductionVO[],
  production: RollProductionVO,
  sourceProductions: RollProductionVO[] = [production],
): number | undefined {
  if (!isDeliverableFinish(finish)) return undefined
  return canonicalFinishEstimateWeight(finish, { production, finishes, sourceProductions })
}

export function sumProductionEstimateWeight(
  production: RollProductionVO,
  sourceProductions: RollProductionVO[] = [production],
): number {
  const finishes = production.finishes ?? []
  const estimates = canonicalFinishEstimateWeights({ production, finishes, sourceProductions })
  const total = finishes.reduce((sum, finish) => (
    isDeliverableFinish(finish) ? sum + (weightFromCanonicalMap(estimates, finish.uuid) ?? 0) : sum
  ), 0)
  return roundWeightTotal(total)
}

export function formatKg(value?: number): string {
  return formatOptionalKg(value)
}

export function formatProductionKg(value: number | null | undefined, production: RollProductionVO): string {
  if (value == null) return '-'
  return formatKgWithMaxDecimals(value, productionWeightDigits(production))
}

export function formatProductionEstimateKg(value: number | null | undefined): string {
  if (value == null) return '-'
  return formatKgWithMaxDecimals(value, 0)
}

export function formatTon(value?: number): string {
  return formatOptionalTonFromKg(value)
}

export function formatMoney(value?: number): string {
  return formatOptionalMoney(value)
}

export function formatNumber(value: number, digits = 0): string {
  return formatSharedNumber(value, digits)
}

function countOfficialFinishes(finishes: FinishRoll[]): number {
  return finishes.filter(isDeliverableFinish).length
}

function sumOriginalWeight(rolls: OriginalRoll[]): number {
  const total = rolls.reduce((sum, roll) => sum + (sourceWeight(roll) ?? 0), 0)
  return roundWeightTotal(total)
}

function sumFinishes(finishes: FinishRoll[], key: 'estimateWeight' | 'actualWeight'): number {
  return finishes.reduce((sum, finish) => {
    if (!isDeliverableFinish(finish)) return sum
    return sum + (finish[key] ?? 0)
  }, 0)
}

function sumEstimateWeight(detail?: ProcessOrderDetailVO): number {
  const productions = detail?.rollProductions ?? []
  const estimates = new Map<string, number | undefined>()
  productions.forEach((production) => {
    canonicalFinishEstimateWeights({
      production,
      finishes: production.finishes,
      sourceProductions: productions,
    })
      .forEach((weight, uuid) => estimates.set(uuid, weight))
  })
  const explicit = roundWeightTotal((detail?.finishRolls ?? []).reduce((sum, finish) => {
    if (!isDeliverableFinish(finish)) return sum
    return sum + (weightFromCanonicalMap(estimates, finish.uuid,
      finish.estimateWeight ?? finish.estimateWeightSnap) ?? 0)
  }, 0))
  const fallback = productions.reduce((sum, production) => sum + sumProductionEstimateWeight(production, productions), 0)
  return roundWeightTotal(Math.max(explicit, fallback))
}

function sourceWeight(roll: OriginalRoll): number | undefined {
  if (!isRollWeightKnown(roll)) return undefined
  if (roll.actualWeight != null && Number.isFinite(roll.actualWeight) && roll.actualWeight > 0) {
    return roll.actualWeight
  }
  if (roll.totalWeight != null && Number.isFinite(roll.totalWeight) && roll.totalWeight > 0) {
    return roll.totalWeight
  }
  const value = Number(roll.rollWeight ?? 0) * Math.max(1, Number(roll.pieceNum ?? 1))
  return Number.isFinite(value) && value > 0 ? value : undefined
}

function productionWeightDigits(production: RollProductionVO): number {
  return decimalPlaces(production.actualWeight ?? production.rollWeight)
}

function sumKnifeCount(productions: RollProductionVO[]): number {
  return productions.reduce((sum, production) => {
    const rollKnives = (production.steps ?? []).reduce((stepSum, step) => {
      return step.stepType === 1 ? stepSum + (step.knifeCount ?? 0) : stepSum
    }, 0)
    return sum + rollKnives
  }, 0)
}

function processLabel(order?: ProcessOrder, productions: RollProductionVO[] = []): string {
  if (order?.isMixProcess === 1) return '混合工艺'
  const first = productions.find((p) => p.processMode !== 3)
  if (!first) return productions.length > 0 ? '直发' : '-'
  if (first.processMode === 4) return '附加工艺'
  return first.mainStepType === 2 ? '复卷' : '锯纸'
}
