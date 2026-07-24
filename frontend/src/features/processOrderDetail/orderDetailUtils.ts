import type {
  FinishRoll,
  FinishProductionVO,
  OriginalRoll,
  ProcessOrder,
  ProcessOrderDetailVO,
  RollProductionVO,
} from '../../types/processOrder'
import {
  decimalPlaces,
  formatNumber as formatSharedNumber,
  formatKgWithMaxDecimals,
  formatOptionalKg,
  formatOptionalMoney,
  formatOptionalTonFromKg,
} from '../../utils/numberFormatters'

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
): number | undefined {
  if (!isDeliverableFinish(finish)) return undefined
  if (finish.estimateWeight != null && finish.estimateWeight > 0) return finish.estimateWeight
  const officialFinishes = finishes.filter(isDeliverableFinish)
  if (!officialFinishes.length) return undefined
  const availableWeight = productionAvailableWeight(production, officialFinishes)
  if (availableWeight <= 0) return undefined
  const widthBasis = officialFinishes.reduce((sum, item) => sum + (item.finishWidth ?? 0), 0)
  if (widthBasis > 0 && finish.finishWidth) {
    return roundWeight((availableWeight * finish.finishWidth) / widthBasis)
  }
  return roundWeight(availableWeight / officialFinishes.length)
}

export function sumProductionEstimateWeight(production: RollProductionVO): number {
  const finishes = production.finishes ?? []
  return finishes.reduce((sum, finish) => {
    return sum + (resolveFinishEstimateWeight(finish, finishes, production) ?? 0)
  }, 0)
}

export function formatKg(value?: number): string {
  return formatOptionalKg(value)
}

export function formatProductionKg(value: number | null | undefined, production: RollProductionVO): string {
  if (value == null) return '-'
  return formatKgWithMaxDecimals(value, productionWeightDigits(production))
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
  return rolls.reduce((sum, roll) => {
    const weight = roll.totalWeight ?? (roll.rollWeight ?? 0) * (roll.pieceNum ?? 1)
    return sum + weight
  }, 0)
}

function sumFinishes(finishes: FinishRoll[], key: 'estimateWeight' | 'actualWeight'): number {
  return finishes.reduce((sum, finish) => {
    if (!isDeliverableFinish(finish)) return sum
    return sum + (finish[key] ?? 0)
  }, 0)
}

function sumEstimateWeight(detail?: ProcessOrderDetailVO): number {
  const explicit = sumFinishes(detail?.finishRolls ?? [], 'estimateWeight')
  const fallback = (detail?.rollProductions ?? []).reduce((sum, production) => {
    return sum + sumProductionEstimateWeight(production)
  }, 0)
  return Math.max(explicit, fallback)
}

function productionAvailableWeight(production: RollProductionVO, finishes: FinishProductionVO[]): number {
  const totalWeight = (production.rollWeight ?? 0) * (production.pieceNum ?? 1)
  const trimWeight = finishes.reduce((sum, finish) => sum + (finish.trimWeightShare ?? 0), 0)
  return Math.max(0, totalWeight - trimWeight)
}

function productionWeightDigits(production: RollProductionVO): number {
  return decimalPlaces(production.actualWeight ?? production.rollWeight)
}

function roundWeight(value: number): number {
  return Number(value.toFixed(3))
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
