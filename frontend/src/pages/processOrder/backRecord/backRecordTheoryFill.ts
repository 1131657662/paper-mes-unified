import type { FinishRoll, OriginalRoll, ProcessOrderDetailVO, RollProductionVO } from '../../../types/processOrder'
import { allocateIntegerWeight, roundWeightTotal } from '../../../utils/integerWeightAllocation'
import {
  activeFinishRolls,
  isActiveBackRecordFinish,
  type BackRecordFormValues,
  type FinishRecordValues,
  type RollRecordValues,
} from './backRecordUtils'
import { autoTrimWeights } from './backRecordAutoTrim'
import {
  sourceCalculationWeight, sourceConsumptionRatio,
  sourceEstimatedWeight,
  storedEstimatedWeight,
  storedMeasuredWeight,
  workItemSourceRolls,
} from './backRecordSourceRolls'
import { buildBackRecordWorkbench, processSteps } from './backRecordWorkbenchUtils'
import type { BackRecordWorkItem, WorkbenchFinish } from './backRecordWorkbenchTypes'

export function theoreticalBackRecordValues(detail: ProcessOrderDetailVO): BackRecordFormValues {
  const rolls = theoreticalRollValues(detail)
  return {
    rolls,
    finishes: theoreticalFinishValues(detail, rolls),
  }
}

/**
 * Fills the current reference values and explicitly confirms those values for this submission.
 * Unknown rolls remain unconfirmed and still require a real weight entry.
 */
export function confirmedReferenceBackRecordValues(detail: ProcessOrderDetailVO): BackRecordFormValues {
  const values = theoreticalBackRecordValues(detail)
  return {
    ...values,
    rolls: Object.fromEntries(
      Object.entries(values.rolls ?? {}).map(([uuid, value]) => [
        uuid,
        value.actualWeight != null && value.actualWeight > 0 && value.weightEntryMode !== 'MEASURED'
          ? { ...value, weightEntryMode: 'CONFIRM_REFERENCE' as const }
          : value,
      ]),
    ),
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
  const sourceRolls = item.rollProductions.length
    ? item.rollProductions
    : item.roll ? [item.roll] : []
  const rolls = Object.fromEntries(sourceRolls.map((roll) => [
    'uuid' in roll ? roll.uuid : roll.originalUuid,
    theoreticalRollValue(roll),
  ]))
  assignItemFinishes(item, values, rolls)
  return Object.fromEntries(values)
}

function assignItemFinishes(
  item: BackRecordWorkItem,
  values: Map<string, FinishRecordValues>,
  rolls: BackRecordFormValues['rolls'],
) {
  const entries = item.finishes.filter(({ finish }) => isActiveBackRecordFinish(finish))
  if (hasCompleteStoredPlan(entries.map(({ finish }) => finish))) {
    entries.forEach(({ finish }) => values.set(finish.uuid, theoreticalFinishValue(finish, storedPlanWeight(finish))))
    return
  }
  const official = entries.filter(({ finish }) => finish.isSpare !== 1 && finish.isRemain !== 1)
  const trims = entries.filter(({ finish }) => finish.isRemain === 1)
  const sourceWeight = itemSourceWeight(item, rolls)
  const reservedTrimWeight = trimBudget(item, trims, official, sourceWeight)
  const plannedLossWeight = roundWeightTotal(sum(processSteps(item).map((step) => (
    step.lossWeight ?? step.plannedLossWeight
  ))))
  const productTarget = sourceWeight == null
    ? undefined
    : Math.max(0, sourceWeight - roundWeightTotal(reservedTrimWeight) - plannedLossWeight)
  const distributedWeights = distributeOfficialWeights(
    official.map(({ finish }) => finish),
    productTarget,
    true,
  )
  const weights = new Map(official.map(({ finish }, index) => [finish.uuid, distributedWeights[index]]))
  entries.forEach(({ finish }) => {
    const fallback = finish.isSpare === 1 || finish.isRemain === 1 ? undefined : weights.get(finish.uuid)
    values.set(finish.uuid, theoreticalFinishValue(finish, fallback))
  })
  assignAutoTrimWeights(item, values, rolls)
}

function trimBudget(
  item: BackRecordWorkItem,
  trims: WorkbenchFinish[],
  products: WorkbenchFinish[],
  sourceWeight?: number,
) {
  if (!trims.length) return 0
  const explicit = sum(trims.map(({ finish }) => firstPositive(finish.actualWeight, finish.estimateWeight)))
  const measured = sum(trims.map(({ finish }) => firstPositive(finish.actualWeight)))
  const allMeasured = trims.every(({ finish }) => firstPositive(finish.actualWeight) != null)
  if (allMeasured && explicit > 0) return roundWeightTotal(explicit)
  if (sourceWeight == null || sourceWeight <= 0) return roundWeightTotal(explicit)
  const sourceWidth = itemSourceWidth(item)
  const trimWidth = sum(trims.map(({ finish }) => finish.finishWidth))
  if (!sourceWidth || sourceWidth <= 0 || trimWidth <= 0) return roundWeightTotal(explicit)
  const productWidth = sum(products.map(({ finish }) => finish.finishWidth))
  const physicalTrimWidth = trimWidth + (widthPolicy(item) === 'REMAINDER'
    ? Math.max(0, sourceWidth - productWidth - trimWidth) : 0)
  const widthBudget = sourceWeight * physicalTrimWidth / sourceWidth
  return roundWeightTotal(Math.max(measured, widthBudget))
}

function theoreticalRollValue(roll: OriginalRoll | RollProductionVO): RollRecordValues {
  const measured = storedMeasuredWeight(roll)
  const estimated = storedEstimatedWeight(roll)
  const nominal = sourceEstimatedWeight(roll)
  return {
    actualGramWeight: roll.actualGramWeight ?? roll.gramWeight,
    actualWidth: roll.actualWidth ?? roll.originalWidth,
    actualWeight: measured ?? estimated ?? nominal,
    weightEntryMode: measured != null ? 'MEASURED'
      : estimated != null ? 'USER_ESTIMATE'
        : nominal != null ? 'CARRY_NOMINAL' : undefined,
    remark: roll.remark,
  }
}

function theoreticalFinishValue(
  finish: FinishRoll,
  fallbackWeight?: number,
): FinishRecordValues {
  return {
    finishWidth: finish.finishWidth && finish.finishWidth > 0 ? finish.finishWidth : undefined,
    finishDiameter: finish.finishDiameter,
    finishCoreDiameter: finish.finishCoreDiameter,
    actualWeight: finish.actualWeight ?? fallbackWeight,
    scrapWeight: finish.scrapWeight ?? 0,
    isRemain: finish.isRemain ?? 0,
    isAbnormal: finish.isAbnormal ?? 0,
    abnormalType: finish.abnormalType,
    actualRemark: finish.actualRemark,
  }
}

function distributeOfficialWeights(
  finishes: FinishRoll[],
  roll: number | undefined,
  balanceToSource: boolean,
): Array<number | undefined> {
  if (!finishes.length) return []
  const explicit = finishes.map((finish) => roundOptional(firstPositive(finish.actualWeight, finish.estimateWeight)))
  const total = roll
  if (balanceToSource) return balanceExplicitWeights(finishes, explicit, total)
  if (explicit.every((weight) => weight != null)) return explicit
  if (!total || total <= 0) return explicit
  const totalWeight = roundWeightTotal(total)
  const knownTotal = sum(explicit)
  const missingCount = explicit.filter((weight) => weight == null).length
  if (knownTotal > 0 && knownTotal < totalWeight && missingCount > 0) {
    const missing = allocateIntegerWeight(totalWeight - knownTotal, Array.from({ length: missingCount }, () => 1))
    let missingIndex = 0
    return explicit.map((weight) => {
      if (weight != null) return weight
      return missing[missingIndex++] ?? 0
    })
  }
  if (knownTotal >= totalWeight) return explicit
  return allocateIntegerWeight(totalWeight, finishes.map(() => 1))
}

function balanceExplicitWeights(
  finishes: FinishRoll[],
  weights: Array<number | undefined>,
  total: number | undefined,
): Array<number | undefined> {
  if (total == null || total <= 0 || weights.length === 0) return weights
  const fixed = finishes.reduce((sum, finish) => (
    finish.actualWeight != null && finish.actualWeight > 0 ? sum + finish.actualWeight : sum
  ), 0)
  const adjustable = finishes.map((finish, index) => (
    finish.actualWeight != null && finish.actualWeight > 0 ? undefined : index
  )).filter((index): index is number => index != null)
  if (fixed > total) return clearAdjustableWeights(weights, adjustable)
  if (!adjustable.length) return weights
  const remaining = total - fixed
  if (!Number.isFinite(remaining) || Math.abs(remaining - Math.round(remaining)) >= 1e-9) {
    return clearAdjustableWeights(weights, adjustable)
  }
  const allocated = allocateIntegerWeight(
    remaining,
    adjustable.map((index) => Math.max(1, finishes[index]?.finishWidth ?? 1)),
  )
  const result = [...weights]
  adjustable.forEach((index, position) => { result[index] = allocated[position] ?? 0 })
  return result
}

function clearAdjustableWeights(weights: Array<number | undefined>, adjustable: number[]) {
  const adjustableIndexes = new Set(adjustable)
  return weights.map((weight, index) => adjustableIndexes.has(index) ? undefined : weight)
}

function assignAutoTrimWeights(
  item: BackRecordWorkItem,
  values: Map<string, FinishRecordValues>,
  rolls: BackRecordFormValues['rolls'],
) {
  const finishes = Object.fromEntries(Array.from(values.entries()))
  const fixedTrimUuids = new Set(item.finishes
    .filter(({ finish }) => finish.isRemain === 1 && (values.get(finish.uuid)?.actualWeight ?? 0) > 0)
    .map(({ finish }) => finish.uuid))
  const patches = autoTrimWeights(item, { finishes, rolls }, {
    autoTrimUuids: new Set(item.finishes
      .filter(({ finish }) => finish.isRemain === 1 && values.get(finish.uuid)?.actualWeight == null)
      .map(({ finish }) => finish.uuid)),
    manualTrimUuids: fixedTrimUuids,
  })
  for (const patch of patches) {
    values.set(patch.uuid, { ...values.get(patch.uuid), actualWeight: patch.actualWeight })
  }
}

function itemSourceWidth(item: BackRecordWorkItem) {
  const widths = workItemSourceRolls(item)
    .map((source) => source.actualWidth ?? source.originalWidth)
    .filter((width): width is number => width != null && width > 0)
  return widths.length > 0 && widths.every((width) => width === widths[0]) ? widths[0] : undefined
}

function widthPolicy(item: BackRecordWorkItem) {
  return processSteps(item).find((step) => step.widthDifferencePolicy)?.widthDifferencePolicy ?? 'REMAINDER'
}

function firstPositive(...values: Array<number | undefined>) {
  return values.find((value) => value != null && Number.isFinite(value) && value > 0)
}

function hasStoredPlanWeight(finish: FinishRoll): boolean {
  return storedPlanWeight(finish) != null
}

function hasCompleteStoredPlan(finishes: FinishRoll[]): boolean {
  return finishes.length > 0 && finishes.every(hasStoredPlanWeight)
}

function storedPlanWeight(finish: FinishRoll): number | undefined {
  const value = finish.estimateWeightSnap ?? (finish.finishRollNo ? finish.estimateWeight : undefined)
  return value != null && Number.isFinite(value) && value >= 0 ? roundWeightTotal(value) : undefined
}

function itemSourceWeight(item: BackRecordWorkItem, rolls: BackRecordFormValues['rolls']) {
  const sources = workItemSourceRolls(item)
  const total = sources.reduce((sum, source) => {
    const weight = rolls?.[source.uuid]?.actualWeight ?? sourceCalculationWeight(source) ?? 0
    return sum + weight * sourceConsumptionRatio(item, source.uuid)
  }, 0)
  return total > 0 ? roundWeightTotal(total) : undefined
}

function roundOptional(value: number | undefined) {
  return value == null ? undefined : roundWeightTotal(value)
}

function sum(values: Array<number | undefined>): number {
  return values.reduce<number>((total, value) => total + (value ?? 0), 0)
}
