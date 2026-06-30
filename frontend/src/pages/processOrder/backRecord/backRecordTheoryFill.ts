import type { FinishRoll, OriginalRoll, ProcessOrderDetailVO } from '../../../types/processOrder'
import {
  activeFinishRolls,
  type BackRecordFormValues,
  type FinishRecordValues,
  type RollRecordValues,
} from './backRecordUtils'
import { buildBackRecordWorkbench } from './backRecordWorkbenchUtils'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

export function theoreticalRollValues(detail: ProcessOrderDetailVO): BackRecordFormValues['rolls'] {
  return Object.fromEntries(detail.originalRolls.map((roll) => [roll.uuid, theoreticalRollValue(roll)]))
}

export function theoreticalFinishValues(detail: ProcessOrderDetailVO): Record<string, FinishRecordValues> {
  const values = new Map<string, FinishRecordValues>()
  const active = new Set(activeFinishRolls(detail).map((finish) => finish.uuid))
  for (const item of buildBackRecordWorkbench(detail).items) {
    assignItemFinishes(item, values)
  }
  for (const finish of activeFinishRolls(detail)) {
    if (!values.has(finish.uuid)) values.set(finish.uuid, theoreticalFinishValue(finish))
  }
  return Object.fromEntries(Array.from(values.entries()).filter(([uuid]) => active.has(uuid)))
}

export function theoreticalItemFinishValues(item: BackRecordWorkItem): Record<string, FinishRecordValues> {
  const values = new Map<string, FinishRecordValues>()
  assignItemFinishes(item, values)
  return Object.fromEntries(values)
}

function assignItemFinishes(item: BackRecordWorkItem, values: Map<string, FinishRecordValues>) {
  const entries = item.finishes.filter(({ finish }) => finish.rollNoStatus !== 3 && finish.sourceType !== 2)
  const official = entries.filter(({ finish }) => finish.isSpare !== 1)
  const distributedWeights = distributeOfficialWeights(official.map(({ finish }) => finish), item.roll)
  const weights = new Map(official.map(({ finish }, index) => [finish.uuid, distributedWeights[index]]))
  entries.forEach(({ finish }) => {
    values.set(finish.uuid, theoreticalFinishValue(finish, finish.isSpare === 1 ? undefined : weights.get(finish.uuid)))
  })
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
    actualWeight: finish.actualWeight ?? firstPositive(finish.estimateWeight, fallbackWeight),
    scrapWeight: finish.scrapWeight ?? 0,
    isRemain: finish.isRemain ?? 0,
    isAbnormal: finish.isAbnormal ?? 0,
    abnormalType: finish.abnormalType,
    actualRemark: finish.actualRemark,
  }
}

function distributeOfficialWeights(finishes: FinishRoll[], roll?: OriginalRoll): Array<number | undefined> {
  if (!finishes.length) return []
  const explicit = finishes.map((finish) => firstPositive(finish.actualWeight, finish.estimateWeight))
  if (explicit.every((weight) => weight != null)) return explicit
  const total = nominalRollWeight(roll)
  if (!total || total <= 0) return explicit
  const totalWeight = total
  const knownTotal = sum(explicit)
  const missingCount = explicit.filter((weight) => weight == null).length
  if (knownTotal > 0 && knownTotal < totalWeight && missingCount > 0) {
    let remaining = totalWeight - knownTotal
    let missingLeft = missingCount
    return explicit.map((weight) => {
      if (weight != null) return weight
      const next = missingLeft === 1 ? roundWeight(remaining) : roundWeight(remaining / missingLeft)
      remaining -= next
      missingLeft -= 1
      return next
    })
  }
  if (knownTotal >= totalWeight) return explicit
  const share = roundWeight(totalWeight / finishes.length)
  return finishes.map((_, index) => (index === finishes.length - 1 ? roundWeight(totalWeight - share * (finishes.length - 1)) : share))
}

function firstPositive(...values: Array<number | undefined>) {
  return values.find((value) => value != null && value > 0)
}

function nominalRollWeight(roll?: OriginalRoll) {
  if (!roll?.rollWeight) return undefined
  return roll.rollWeight * (roll.pieceNum ?? 1)
}

function roundWeight(value: number) {
  return Math.round(value * 1000) / 1000
}

function sum(values: Array<number | undefined>): number {
  return values.reduce<number>((total, value) => total + (value ?? 0), 0)
}
