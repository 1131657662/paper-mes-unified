import type { FinishRoll, OriginalRoll, ProcessOrderDetailVO } from '../../../types/processOrder'
import { decimalPlaces } from '../../../utils/numberFormatters'
import {
  activeFinishRolls,
  type BackRecordFormValues,
  type FinishRecordValues,
  type RollRecordValues,
} from './backRecordUtils'
import { autoTrimWeights } from './backRecordAutoTrim'
import { buildBackRecordWorkbench } from './backRecordWorkbenchUtils'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

export function theoreticalBackRecordValues(detail: ProcessOrderDetailVO): BackRecordFormValues {
  const rolls = theoreticalRollValues(detail)
  return {
    rolls,
    finishes: theoreticalFinishValues(detail, rolls),
  }
}

export function theoreticalRollValues(detail: ProcessOrderDetailVO): BackRecordFormValues['rolls'] {
  return Object.fromEntries(detail.originalRolls.map((roll) => [roll.uuid, theoreticalRollValue(roll)]))
}

export function theoreticalFinishValues(
  detail: ProcessOrderDetailVO,
  rolls: BackRecordFormValues['rolls'] = theoreticalRollValues(detail),
): Record<string, FinishRecordValues> {
  const values = new Map<string, FinishRecordValues>()
  const active = new Set(activeFinishRolls(detail).map((finish) => finish.uuid))
  for (const item of buildBackRecordWorkbench(detail).items) {
    assignItemFinishes(item, values, rolls)
  }
  for (const finish of activeFinishRolls(detail)) {
    if (!values.has(finish.uuid)) values.set(finish.uuid, theoreticalFinishValue(finish))
  }
  return Object.fromEntries(Array.from(values.entries()).filter(([uuid]) => active.has(uuid)))
}

export function theoreticalItemFinishValues(item: BackRecordWorkItem): Record<string, FinishRecordValues> {
  const values = new Map<string, FinishRecordValues>()
  const rolls = item.roll ? { [item.roll.uuid]: theoreticalRollValue(item.roll) } : {}
  assignItemFinishes(item, values, rolls)
  return Object.fromEntries(values)
}

function assignItemFinishes(
  item: BackRecordWorkItem,
  values: Map<string, FinishRecordValues>,
  rolls: BackRecordFormValues['rolls'],
) {
  const entries = item.finishes.filter(({ finish }) => finish.rollNoStatus !== 3 && finish.sourceType !== 2)
  const official = entries.filter(({ finish }) => finish.isSpare !== 1 && finish.isRemain !== 1)
  const hasTrim = entries.some(({ finish }) => finish.isRemain === 1)
  const distributedWeights = distributeOfficialWeights(official.map(({ finish }) => finish), item.roll, !hasTrim)
  const weights = new Map(official.map(({ finish }, index) => [finish.uuid, distributedWeights[index]]))
  entries.forEach(({ finish }) => {
    const fallback = finish.isSpare === 1 || finish.isRemain === 1 ? undefined : weights.get(finish.uuid)
    values.set(finish.uuid, theoreticalFinishValue(finish, fallback))
  })
  assignAutoTrimWeights(item, values, rolls)
}

function theoreticalRollValue(roll: OriginalRoll): RollRecordValues {
  return {
    actualGramWeight: roll.actualGramWeight ?? roll.gramWeight,
    actualWidth: roll.actualWidth ?? roll.originalWidth,
    actualWeight: roll.actualWeight ?? nominalRollWeight(roll),
    remark: roll.remark,
  }
}

function theoreticalFinishValue(finish: FinishRoll, fallbackWeight?: number): FinishRecordValues {
  return {
    finishWidth: finish.finishWidth && finish.finishWidth > 0 ? finish.finishWidth : undefined,
    finishDiameter: finish.finishDiameter,
    finishCoreDiameter: finish.finishCoreDiameter,
    actualWeight: finish.isRemain === 1 || finish.isSpare === 1
      ? finish.actualWeight
      : finish.actualWeight ?? firstPositive(fallbackWeight, finish.estimateWeight),
    scrapWeight: finish.scrapWeight ?? 0,
    isRemain: finish.isRemain ?? 0,
    isAbnormal: finish.isAbnormal ?? 0,
    abnormalType: finish.abnormalType,
    actualRemark: finish.actualRemark,
  }
}

function distributeOfficialWeights(
  finishes: FinishRoll[],
  roll: OriginalRoll | undefined,
  balanceToSource: boolean,
): Array<number | undefined> {
  if (!finishes.length) return []
  const digits = sourceWeightDigits(roll)
  const explicit = finishes.map((finish) => roundOptional(firstPositive(finish.actualWeight, finish.estimateWeight), digits))
  if (explicit.every((weight) => weight != null)) return balanceToSource ? balanceWeights(explicit, nominalRollWeight(roll), digits) : explicit
  const total = nominalRollWeight(roll)
  if (!total || total <= 0) return explicit
  const totalWeight = roundWeight(total, digits)
  const knownTotal = sum(explicit)
  const missingCount = explicit.filter((weight) => weight == null).length
  if (knownTotal > 0 && knownTotal < totalWeight && missingCount > 0) {
    let remaining = totalWeight - knownTotal
    let missingLeft = missingCount
    return explicit.map((weight) => {
      if (weight != null) return weight
      const next = missingLeft === 1 ? roundWeight(remaining, digits) : roundWeight(remaining / missingLeft, digits)
      remaining -= next
      missingLeft -= 1
      return next
    })
  }
  if (knownTotal >= totalWeight) return explicit
  const share = roundWeight(totalWeight / finishes.length, digits)
  return finishes.map((_, index) => (index === finishes.length - 1 ? roundWeight(totalWeight - share * (finishes.length - 1), digits) : share))
}

function balanceWeights(weights: number[], total: number | undefined, digits: number) {
  if (total == null || total <= 0 || weights.length === 0) return weights
  const roundedTotal = roundWeight(total, digits)
  const currentTotal = sum(weights)
  const diff = roundWeight(roundedTotal - currentTotal, digits)
  if (diff === 0) return weights
  const result = [...weights]
  result[result.length - 1] = Math.max(0, roundWeight((result[result.length - 1] ?? 0) + diff, digits))
  return result
}

function assignAutoTrimWeights(
  item: BackRecordWorkItem,
  values: Map<string, FinishRecordValues>,
  rolls: BackRecordFormValues['rolls'],
) {
  const finishes = Object.fromEntries(Array.from(values.entries()))
  const patches = autoTrimWeights(item, { finishes, rolls }, {
    autoTrimUuids: new Set(item.finishes.map(({ finish }) => finish.uuid)),
    manualTrimUuids: new Set(),
  })
  for (const patch of patches) {
    values.set(patch.uuid, { ...values.get(patch.uuid), actualWeight: patch.actualWeight })
  }
}

function firstPositive(...values: Array<number | undefined>) {
  return values.find((value) => value != null && value > 0)
}

function nominalRollWeight(roll?: OriginalRoll) {
  if (!roll?.rollWeight) return undefined
  return roll.rollWeight * (roll.pieceNum ?? 1)
}

function sourceWeightDigits(roll?: OriginalRoll) {
  return decimalPlaces(nominalRollWeight(roll), 3)
}

function roundOptional(value: number | undefined, digits: number) {
  return value == null ? undefined : roundWeight(value, digits)
}

function roundWeight(value: number, digits: number) {
  const scale = 10 ** Math.max(0, digits)
  return Math.round(value * scale) / scale
}

function sum(values: Array<number | undefined>): number {
  return values.reduce<number>((total, value) => total + (value ?? 0), 0)
}
