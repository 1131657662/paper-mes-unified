import type { FinishProductionVO, FinishSourceVO, RollProductionVO } from '../../../types/processOrder'
import { roundWeightTotal } from '../../../utils/integerWeightAllocation'

type WidthPolicy = 'LOSS' | 'ALLOCATE' | 'REMAINDER'

export interface FinishSourceBudgetOptions {
  effectiveRatios: Map<FinishSourceVO, number>
  finishes: FinishProductionVO[]
  production: RollProductionVO
  sourceProductions?: RollProductionVO[]
}

export function finishGroupsBySource(
  finishes: FinishProductionVO[],
  production: RollProductionVO,
): FinishProductionVO[][] {
  const groups = new Map<string, FinishProductionVO[]>()
  for (const finish of finishes) {
    const key = finishSourceIds(finish, production).join('|')
    groups.set(key, [...(groups.get(key) ?? []), finish])
  }
  return Array.from(groups.values())
}

export function finishGroupSourceWeight(options: FinishSourceBudgetOptions): number | undefined {
  const sources = productionMap(options)
  const snapshots = snapshotMap(options.finishes)
  const sourceIds = groupSourceIds(options)
  if (sourceIds.length === 0) return productionWeight(options.production)
  let total = 0
  for (const sourceId of sourceIds) {
    const weight = sourceWeight(sources.get(sourceId), snapshots.get(sourceId))
    if (weight == null || weight <= 0) return undefined
    total += weight * groupSourceRatio(options, sourceId) / 100
  }
  return total > 0 ? roundWeightTotal(total) : undefined
}

export function finishGroupTargetWeight(
  options: FinishSourceBudgetOptions,
  sourceWeight: number,
): number {
  const plannedLoss = groupPlannedLoss(options)
  if (plannedLoss > 0) return Math.max(0, sourceWeight - plannedLoss)
  if (finishGroupWidthPolicy(options) !== 'LOSS') return sourceWeight
  const sourceWidth = finishGroupSourceWidth(options)
  if (!sourceWidth) return sourceWeight
  const usedWidth = options.finishes.reduce(
    (sum, finish) => sum + Math.max(0, finish.finishWidth ?? 0),
    0,
  )
  const lossWidth = Math.max(0, sourceWidth - usedWidth)
  return Math.max(0, sourceWeight - roundWeightTotal(sourceWeight * lossWidth / sourceWidth))
}

export function finishGroupSourceWidth(options: FinishSourceBudgetOptions): number | undefined {
  const sources = productionMap(options)
  const widths = groupSourceIds(options).map((sourceId) => {
    const source = sources.get(sourceId)
    return source?.actualWidth ?? source?.originalWidth
  })
  if (widths.some((width) => width == null || width <= 0)) return undefined
  return new Set(widths).size === 1 ? widths[0] : undefined
}

export function finishGroupWidthPolicy(options: FinishSourceBudgetOptions): WidthPolicy | undefined {
  const sources = productionMap(options)
  const steps = groupSourceIds(options).flatMap((sourceId) => sources.get(sourceId)?.steps ?? [])
    .filter((step) => step.stepType === 1)
    .sort((left, right) => (left.stageLevel ?? 1) - (right.stageLevel ?? 1)
      || (left.stepSort ?? 0) - (right.stepSort ?? 0))
  const latest = steps.at(-1)
  if (latest?.widthDifferencePolicy) return latest.widthDifferencePolicy
  return steps.length > 0 || options.production.mainStepType === 1 ? 'REMAINDER' : undefined
}

function groupPlannedLoss(options: FinishSourceBudgetOptions): number {
  const sources = productionMap(options)
  const loss = groupSourceIds(options).reduce((sum, sourceId) => {
    const source = sources.get(sourceId)
    const sourceLoss = (source?.steps ?? [])
      .filter((step) => step.widthDifferencePolicy === 'LOSS')
      .reduce((total, step) => total + Math.max(0, step.plannedLossWeight ?? 0), 0)
    return sum + sourceLoss * groupSourceRatio(options, sourceId) / 100
  }, 0)
  return roundWeightTotal(loss)
}

function groupSourceRatio(options: FinishSourceBudgetOptions, sourceId: string): number {
  const relations = options.finishes.flatMap((finish) => finish.sources ?? [])
    .filter((source) => source.originalUuid === sourceId)
  if (relations.length === 0) return 100
  return Math.min(100, relations.reduce(
    (sum, source) => sum + (options.effectiveRatios.get(source) ?? 0),
    0,
  ))
}

function groupSourceIds(options: FinishSourceBudgetOptions): string[] {
  return Array.from(new Set(options.finishes.flatMap((finish) => (
    finishSourceIds(finish, options.production)
  )))).sort()
}

function finishSourceIds(finish: FinishProductionVO, production: RollProductionVO): string[] {
  const ids = Array.from(new Set((finish.sources ?? [])
    .map((source) => source.originalUuid)
    .filter((uuid): uuid is string => Boolean(uuid)))).sort()
  if (ids.length === 0 && production.originalUuid) ids.push(production.originalUuid)
  return ids
}

function productionMap(options: FinishSourceBudgetOptions): Map<string, RollProductionVO> {
  const result = new Map<string, RollProductionVO>()
  for (const source of [options.production, ...(options.sourceProductions ?? [])]) {
    if (source.originalUuid) result.set(source.originalUuid, source)
  }
  return result
}

function snapshotMap(finishes: FinishProductionVO[]): Map<string, FinishSourceVO> {
  const result = new Map<string, FinishSourceVO>()
  for (const source of finishes.flatMap((finish) => finish.sources ?? [])) {
    if (source.originalUuid) result.set(source.originalUuid, source)
  }
  return result
}

function sourceWeight(
  production: RollProductionVO | undefined,
  snapshot?: FinishSourceVO,
): number | undefined {
  return production ? productionWeight(production) ?? snapshotWeight(snapshot) : snapshotWeight(snapshot)
}

function snapshotWeight(source?: FinishSourceVO): number | undefined {
  if (!source) return undefined
  if (isPositiveFinite(source.actualWeight)) return roundWeightTotal(source.actualWeight)
  if (source.weightStatus === 'UNKNOWN') return undefined
  if (isPositiveFinite(source.totalWeight)) return roundWeightTotal(source.totalWeight)
  return multipliedWeight(source.rollWeight, source.pieceNum)
}

function productionWeight(production: RollProductionVO): number | undefined {
  if (isPositiveFinite(production.actualWeight)) return roundWeightTotal(production.actualWeight)
  if (production.weightStatus === 'UNKNOWN') return undefined
  if (isPositiveFinite(production.totalWeight)) return roundWeightTotal(production.totalWeight)
  return multipliedWeight(production.rollWeight, production.pieceNum)
}

function multipliedWeight(weight?: number, pieces?: number): number | undefined {
  const value = Number(weight ?? 0) * Math.max(1, Number(pieces ?? 1))
  return Number.isFinite(value) && value > 0 ? roundWeightTotal(value) : undefined
}

function isPositiveFinite(value?: number): value is number {
  return value != null && Number.isFinite(value) && value > 0
}
