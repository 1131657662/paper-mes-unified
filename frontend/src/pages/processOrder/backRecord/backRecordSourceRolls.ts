import type { OriginalRoll, RollProductionVO } from '../../../types/processOrder'
import type { BackRecordFormValues } from './backRecordUtils'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'
import { roundWeightTotal } from '../../../utils/integerWeightAllocation'
import {
  effectiveSourceConsumptionRatios,
  validateExplicitSourceConsumptionRatios,
} from '../../../utils/sourceConsumptionRatios'

export interface BackRecordSourceRoll {
  uuid: string
  actualGramWeight?: number
  actualWeight?: number
  actualWidth?: number
  batchNo?: string
  extraNo?: string
  gramWeight?: number
  originalWidth?: number
  paperName?: string
  pieceNum?: number
  remark?: string
  rollNo?: string
  rollWeight?: number
  totalWeight?: number
  weightStatus?: 'UNKNOWN' | 'ESTIMATED' | 'MEASURED'
}

export interface SourceWeightSummary {
  completeTotal?: number
  missingCount: number
  measuredMissingCount: number
  recordedTotal: number
  sources: BackRecordSourceRoll[]
}

type WeightSource = Pick<BackRecordSourceRoll, 'actualWeight' | 'weightStatus' | 'totalWeight' | 'rollWeight' | 'pieceNum'>

export function workItemSourceRolls(item: BackRecordWorkItem): BackRecordSourceRoll[] {
  const productions = item.rollProductions
    .map(normalizeProduction)
    .filter((roll): roll is BackRecordSourceRoll => roll != null)
  if (productions.length > 0) return uniqueSources(productions)
  return item.roll ? [normalizeRoll(item.roll)] : []
}

export function sourceWeightSummary(
  item: BackRecordWorkItem,
  values: BackRecordFormValues,
): SourceWeightSummary {
  const sources = workItemSourceRolls(item)
  const weights = sources.map((source) => sourceActualWeight(source, values))
  const missingCount = weights.filter((weight) => !positive(weight)).length
  const measuredMissingCount = sources.filter((source) => !sourceIsMeasured(source, values)).length
  const recordedTotal = sources.reduce<number>((sum, source, index) => (
    sum + (weights[index] ?? 0) * sourceConsumptionRatio(item, source.uuid)
  ), 0)
  return {
    completeTotal: sources.length > 0 && missingCount === 0 ? recordedTotal : undefined,
    missingCount,
    measuredMissingCount,
    recordedTotal,
    sources,
  }
}

export function sourceConsumptionRatio(item: BackRecordWorkItem, sourceUuid: string): number {
  const relations = item.finishes
    .flatMap(({ finish }) => finish.sources ?? [])
    .filter((source) => source.originalUuid === sourceUuid)
  if (relations.length === 0) return 1
  validateExplicitSourceConsumptionRatios(relations)
  const effective = effectiveSourceConsumptionRatios(relations)
  const ratio = relations.reduce((sum, relation) => sum + (effective.get(relation) ?? 0), 0)
  return Math.min(1, Math.max(0, ratio / 100))
}

export function sourceActualWeight(
  source: BackRecordSourceRoll,
  values: BackRecordFormValues,
): number | undefined {
  const value = values.rolls?.[source.uuid]?.actualWeight ?? storedMeasuredWeight(source)
  return isPositiveFinite(value) ? value : undefined
}

export function storedMeasuredWeight(source: WeightSource): number | undefined {
  if (!positive(source.actualWeight)) return undefined
  if (source.weightStatus === 'UNKNOWN') return undefined
  if (source.weightStatus === 'ESTIMATED') return undefined
  return source.actualWeight
}

export function storedEstimatedWeight(source: WeightSource): number | undefined {
  if (source.weightStatus !== 'ESTIMATED' || !positive(source.actualWeight)) return undefined
  return roundWeightTotal(source.actualWeight)
}

export function sourceIsMeasured(source: BackRecordSourceRoll, values: BackRecordFormValues): boolean {
  const mode = values.rolls?.[source.uuid]?.weightEntryMode
  if (mode) {
    const confirmed = mode === 'MEASURED' || mode === 'CONFIRM_REFERENCE'
    return confirmed && positive(values.rolls?.[source.uuid]?.actualWeight)
  }
  if (source.weightStatus === 'ESTIMATED') return false
  if (positive(values.rolls?.[source.uuid]?.actualWeight)) return true
  return storedMeasuredWeight(source) != null
}

export function sourceEstimatedWeight(source: WeightSource): number | undefined {
  if (source.weightStatus === 'UNKNOWN') return undefined
  if (isPositiveFinite(source.totalWeight)) return roundWeightTotal(source.totalWeight)
  if (!isPositiveFinite(source.rollWeight)) return undefined
  const value = source.rollWeight * (source.pieceNum ?? 1)
  return Number.isFinite(value) && value > 0 ? roundWeightTotal(value) : undefined
}

export function sourceCalculationWeight(source: WeightSource): number | undefined {
  if (isPositiveFinite(source.actualWeight)) {
    return roundWeightTotal(source.actualWeight)
  }
  if (source.weightStatus === 'UNKNOWN') return undefined
  const estimated = sourceEstimatedWeight(source)
  return estimated == null ? undefined : roundWeightTotal(estimated)
}

function normalizeProduction(source: RollProductionVO): BackRecordSourceRoll | undefined {
  if (!source.originalUuid) return undefined
  return { ...source, uuid: source.originalUuid }
}

function normalizeRoll(roll: OriginalRoll): BackRecordSourceRoll {
  return { ...roll }
}

function uniqueSources(sources: BackRecordSourceRoll[]): BackRecordSourceRoll[] {
  return Array.from(new Map(sources.map((source) => [source.uuid, source])).values())
}

function positive(value?: number): boolean {
  return isPositiveFinite(value)
}

function isPositiveFinite(value?: number): value is number {
  return value != null && Number.isFinite(value) && value > 0
}
