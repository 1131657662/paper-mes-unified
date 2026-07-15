import type { PlanPreviewVO, ProcessPlanDTO, ProcessRoutePreviewVO } from '../../types/processOrder'
import type { MergedSourceLock } from './rewindConsumptionUtils'
import type { RollDraft } from './types'

export type WeightBalanceStatus = 'balanced' | 'unbalanced' | 'pending' | 'excluded'

export interface RollWeightBalance {
  status: WeightBalanceStatus
  inputWeight: number
  finishWeight: number
  trimWeight: number
  difference: number
  label: string
  detail: string
  blocking: boolean
}

export interface OrderWeightBalance {
  inputWeight: number
  finishWeight: number
  trimWeight: number
  difference: number
  balancedCount: number
  unbalancedCount: number
  excludedCount: number
  pendingCount: number
  blocking: boolean
}

interface RollBalanceOptions {
  roll: RollDraft
  rolls: RollDraft[]
  plan?: ProcessPlanDTO
  preview?: PlanPreviewVO
  routePreview?: ProcessRoutePreviewVO
  lock?: MergedSourceLock
}

export function calculateRollWeightBalance(options: RollBalanceOptions): RollWeightBalance {
  if (options.lock) return excludedBalance(`计入 ${options.lock.ownerLabel} 的合并方案`)
  if (options.roll.processMode === 3) return excludedBalance('直发母卷不生成加工成品')
  if (options.roll.processMode === 2) return excludedBalance('现场定尺，重量在回录时确认')
  if (options.routePreview) return routeBalance(options.roll, options.routePreview)
  if (!options.preview?.ready) return pendingBalance(options.preview ? '当前预览未通过' : '尚未取得后端预览')
  return previewBalance(options)
}

export function summarizeWeightBalances(balances: RollWeightBalance[]): OrderWeightBalance {
  const checked = balances.filter((item) => item.status === 'balanced' || item.status === 'unbalanced')
  const totals = checked.reduce(
    (sum, item) => ({
      inputWeight: sum.inputWeight + item.inputWeight,
      finishWeight: sum.finishWeight + item.finishWeight,
      trimWeight: sum.trimWeight + item.trimWeight,
    }),
    { inputWeight: 0, finishWeight: 0, trimWeight: 0 },
  )
  return {
    ...roundedTotals(totals),
    balancedCount: countStatus(balances, 'balanced'),
    unbalancedCount: countStatus(balances, 'unbalanced'),
    excludedCount: countStatus(balances, 'excluded'),
    pendingCount: countStatus(balances, 'pending'),
    blocking: balances.some((item) => item.blocking),
  }
}

function previewBalance(options: RollBalanceOptions): RollWeightBalance {
  const inputWeight = planInputWeight(options)
  return resolvedBalance({
    inputWeight,
    finishWeight: Number(options.preview?.totalEstimateWeight ?? 0),
    trimWeight: Number(options.preview?.totalTrimWeight ?? 0),
  })
}

function routeBalance(roll: RollDraft, preview: ProcessRoutePreviewVO): RollWeightBalance {
  const outputs = firstStageOutputs(preview)
  return resolvedBalance({
    inputWeight: rollTotalWeight(roll),
    finishWeight: sumOutputWeight(outputs.filter((item) => item.isRemain !== 1)),
    trimWeight: sumOutputWeight(outputs.filter((item) => item.isRemain === 1)),
  })
}

function resolvedBalance(weights: WeightTotals): RollWeightBalance {
  const totals = roundedTotals(weights)
  const balanced = Math.abs(totals.difference) < 0.001
  return {
    status: balanced ? 'balanced' : 'unbalanced',
    ...totals,
    label: balanced ? '重量已平衡' : '存在未分配差值',
    detail: balanced ? '投入重量已全部分配' : '请返回工艺配置检查成品、切边或损耗',
    blocking: !balanced,
  }
}

function planInputWeight(options: RollBalanceOptions): number {
  if (options.plan?.rewindMode !== 5) return rollTotalWeight(options.roll)
  const sources = options.plan.segments?.flatMap((segment) => segment.sources ?? []) ?? []
  if (!sources.length) return rollTotalWeight(options.roll)
  const weights = new Map(options.rolls.filter(hasUuid).map((roll) => [roll.uuid, rollTotalWeight(roll)]))
  const usesConsumption = sources.some((source) => source.consumeRatio != null)
  if (usesConsumption) {
    return sources.reduce((sum, source) => sum + (weights.get(source.originalUuid ?? '') ?? 0) * Number(source.consumeRatio ?? 0) / 100, 0)
  }
  return Array.from(new Set(sources.map((source) => source.originalUuid).filter(Boolean)))
    .reduce((sum, uuid) => sum + (weights.get(uuid ?? '') ?? 0), 0)
}

function firstStageOutputs(preview: ProcessRoutePreviewVO) {
  const outputs = preview.outputs ?? []
  const levels = outputs.map((item) => item.stageLevel).filter((value): value is number => value != null)
  const firstLevel = levels.length ? Math.min(...levels) : undefined
  return firstLevel == null ? [] : outputs.filter((item) => item.stageLevel === firstLevel)
}

function roundedTotals(weights: WeightTotals) {
  const inputWeight = roundKg(weights.inputWeight)
  const finishWeight = roundKg(weights.finishWeight)
  const trimWeight = roundKg(weights.trimWeight)
  return { inputWeight, finishWeight, trimWeight, difference: roundKg(inputWeight - finishWeight - trimWeight) }
}

function pendingBalance(detail: string): RollWeightBalance {
  return emptyBalance('pending', '等待重量校验', detail)
}

function excludedBalance(detail: string): RollWeightBalance {
  return emptyBalance('excluded', '无需开单校验', detail)
}

function emptyBalance(status: WeightBalanceStatus, label: string, detail: string): RollWeightBalance {
  return { status, inputWeight: 0, finishWeight: 0, trimWeight: 0, difference: 0, label, detail, blocking: false }
}

function sumOutputWeight(outputs: NonNullable<ProcessRoutePreviewVO['outputs']>): number {
  return outputs.reduce((sum, item) => sum + Number(item.estimateWeight ?? 0), 0)
}

function countStatus(items: RollWeightBalance[], status: WeightBalanceStatus): number {
  return items.filter((item) => item.status === status).length
}

function rollTotalWeight(roll: RollDraft): number {
  return Number(roll.rollWeight ?? 0) * Number(roll.pieceNum ?? 1)
}

function roundKg(value: number): number {
  return Number(value.toFixed(3))
}

function hasUuid(roll: RollDraft): roll is RollDraft & { uuid: string } {
  return Boolean(roll.uuid)
}

interface WeightTotals {
  inputWeight: number
  finishWeight: number
  trimWeight: number
}
