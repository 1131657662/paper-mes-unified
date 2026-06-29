import type {
  FinishRoll,
  OriginalRoll,
  ProcessOrder,
  ProcessOrderDetailVO,
  RollProductionVO,
} from '../../types/processOrder'

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
    spareCount: finishes.filter((f) => f.isSpare === 1 && f.rollNoStatus !== 3).length,
    totalOriginalWeight: sumOriginalWeight(rolls),
    totalEstimateWeight: sumFinishes(finishes, 'estimateWeight'),
    totalActualWeight: sumFinishes(finishes, 'actualWeight'),
    knifeCount: sumKnifeCount(productions),
    stepCount: detail?.steps?.length ?? 0,
    processLabel: processLabel(detail?.order, productions),
  }
}

export function formatKg(value?: number): string {
  if (!value) return '-'
  return `${formatNumber(value, 3)} kg`
}

export function formatTon(value?: number): string {
  if (!value) return '-'
  return `${formatNumber(value / 1000, 3)} t`
}

export function formatMoney(value?: number): string {
  if (value == null) return '-'
  return `¥${formatNumber(value, 2)}`
}

export function formatNumber(value: number, digits = 0): string {
  return new Intl.NumberFormat('zh-CN', {
    maximumFractionDigits: digits,
    minimumFractionDigits: digits,
  }).format(value)
}

function countOfficialFinishes(finishes: FinishRoll[]): number {
  return finishes.filter((f) => f.isSpare !== 1 && f.rollNoStatus !== 3).length
}

function sumOriginalWeight(rolls: OriginalRoll[]): number {
  return rolls.reduce((sum, roll) => {
    const weight = roll.totalWeight ?? (roll.rollWeight ?? 0) * (roll.pieceNum ?? 1)
    return sum + weight
  }, 0)
}

function sumFinishes(finishes: FinishRoll[], key: 'estimateWeight' | 'actualWeight'): number {
  return finishes.reduce((sum, finish) => {
    if (finish.isSpare === 1 || finish.rollNoStatus === 3) return sum
    return sum + (finish[key] ?? 0)
  }, 0)
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
  return first.mainStepType === 2 ? '复卷' : '锯纸'
}
