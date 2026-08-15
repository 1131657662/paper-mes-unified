import type { OriginalRoll, RollProductionVO } from '../../../types/processOrder'
import type { BackRecordFormValues } from './backRecordUtils'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

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
  const recordedTotal = weights.reduce<number>((sum, weight) => sum + (weight ?? 0), 0)
  return {
    completeTotal: sources.length > 0 && missingCount === 0 ? recordedTotal : undefined,
    missingCount,
    measuredMissingCount,
    recordedTotal,
    sources,
  }
}

export function sourceActualWeight(
  source: BackRecordSourceRoll,
  values: BackRecordFormValues,
): number | undefined {
  return values.rolls?.[source.uuid]?.actualWeight ?? storedMeasuredWeight(source)
}

export function storedMeasuredWeight(source: BackRecordSourceRoll): number | undefined {
  if (!positive(source.actualWeight)) return undefined
  if (source.weightStatus === 'UNKNOWN' || source.weightStatus === 'ESTIMATED') return undefined
  return source.actualWeight
}

export function storedEstimatedWeight(source: BackRecordSourceRoll): number | undefined {
  if (source.weightStatus !== 'ESTIMATED' || !positive(source.actualWeight)) return undefined
  return source.actualWeight
}

export function sourceIsMeasured(source: BackRecordSourceRoll, values: BackRecordFormValues): boolean {
  const mode = values.rolls?.[source.uuid]?.weightEntryMode
  if (mode) return mode === 'MEASURED' && positive(values.rolls?.[source.uuid]?.actualWeight)
  if (source.weightStatus === 'UNKNOWN' || source.weightStatus === 'ESTIMATED') return false
  if (positive(values.rolls?.[source.uuid]?.actualWeight)) return true
  return storedMeasuredWeight(source) != null
}

export function sourceEstimatedWeight(source: BackRecordSourceRoll): number | undefined {
  if (source.weightStatus === 'UNKNOWN') return undefined
  if (source.totalWeight != null && source.totalWeight > 0) return source.totalWeight
  if (source.rollWeight == null || source.rollWeight <= 0) return undefined
  return source.rollWeight * (source.pieceNum ?? 1)
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
  return value != null && value > 0
}
