import { allocateIntegerWeight, roundWeightTotal } from '../../../utils/integerWeightAllocation'
import type { BackRecordFinishAdjustmentValues } from '../../../types/processOrder'
import type { BackRecordFormValues } from './backRecordUtils'
import type { BackRecordWorkItem, WorkbenchFinish } from './backRecordWorkbenchTypes'
import { isFinishProduced } from './backRecordFinishAdjustment'
import { sourceCalculationWeight, sourceConsumptionRatio, workItemSourceRolls } from './backRecordSourceRolls'

interface AutoTrimOptions {
  autoTrimUuids: Set<string>
  manualTrimUuids: Set<string>
  adjustment?: BackRecordFinishAdjustmentValues
}

export interface AutoTrimWeight {
  uuid: string
  actualWeight: number
}

export function autoTrimWeights(
  item: BackRecordWorkItem,
  values: BackRecordFormValues,
  options: AutoTrimOptions,
): AutoTrimWeight[] {
  if (!item.roll) return []
  const trimFinishes = item.finishes.filter((entry) => isTrimFinish(entry, values))
  if (trimFinishes.length === 0) return []

  const officialFinishes = item.finishes.filter((entry) => (
    isOfficialFinish(entry, values)
    && isFinishProduced(entry.finish.uuid, options.adjustment)
  ))
  if (officialFinishes.length === 0 || !allOfficialWeightsFilled(officialFinishes, values)) return []

  const sourceWeight = sourceWeightForItem(item, values)
  if (sourceWeight == null || sourceWeight <= 0) return []

  const remainder = sourceWeight - officialTotal(officialFinishes, values) - lossTotal(item, values) - scrapTotal(item, values)
  if (remainder < 0 || !Number.isFinite(remainder) || Math.abs(remainder - Math.round(remainder)) >= 1e-9) return []

  const manualTrimTotal = sum(trimFinishes
    .filter(({ finish }) => options.manualTrimUuids.has(finish.uuid))
    .map(({ finish }) => values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight))
  const editableRemainder = Math.max(0, remainder - manualTrimTotal)
  const editableTrims = trimFinishes.filter(({ finish }) => !options.manualTrimUuids.has(finish.uuid))
  if (editableTrims.length === 0) return []

  const weights = allocateIntegerWeight(editableRemainder, editableTrims.map(() => 1))
  return editableTrims
    .map((entry, index) => ({
      uuid: entry.finish.uuid,
      actualWeight: weights[index] ?? 0,
    }))
    .filter((patch) => {
      const current = values.finishes?.[patch.uuid]?.actualWeight
      return current == null || options.autoTrimUuids.has(patch.uuid) || current === patch.actualWeight
    })
}

function isOfficialFinish(entry: WorkbenchFinish, values: BackRecordFormValues) {
  return entry.finish.isSpare !== 1 && !isTrimFinish(entry, values)
}

function isTrimFinish(entry: WorkbenchFinish, values: BackRecordFormValues) {
  return (values.finishes?.[entry.finish.uuid]?.isRemain ?? entry.finish.isRemain ?? 0) === 1
}

function allOfficialWeightsFilled(finishes: WorkbenchFinish[], values: BackRecordFormValues) {
  return finishes.every(({ finish }) => {
    const actualWeight = values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight
    return actualWeight != null && Number.isFinite(actualWeight) && actualWeight > 0
  })
}

function officialTotal(finishes: WorkbenchFinish[], values: BackRecordFormValues) {
  return sum(finishes.map(({ finish }) => values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight))
}

function scrapTotal(item: BackRecordWorkItem, values: BackRecordFormValues) {
  return sum(item.finishes.map(({ finish }) => values.finishes?.[finish.uuid]?.scrapWeight ?? finish.scrapWeight))
}

function lossTotal(item: BackRecordWorkItem, values: BackRecordFormValues) {
  const productions = item.rollProductions.length ? item.rollProductions : item.production ? [item.production] : []
  const steps = productions.flatMap((production) => production.steps ?? [])
  const uniqueSteps = Array.from(new Map(steps.map((step) => [step.uuid, step])).values())
  return sum(uniqueSteps.map((step) => values.steps?.[step.uuid]?.lossWeight ?? step.lossWeight))
}

function sourceWeightForItem(item: BackRecordWorkItem, values: BackRecordFormValues) {
  const sources = workItemSourceRolls(item)
  const total = sources.reduce((sum, source) => {
    const weight = values.rolls?.[source.uuid]?.actualWeight
      ?? sourceCalculationWeight(source)
      ?? 0
    return sum + weight * sourceConsumptionRatio(item, source.uuid)
  }, 0)
  return total > 0 ? roundWeightTotal(total) : undefined
}

function sum(values: Array<number | undefined>) {
  return values.reduce<number>((total, value) => total + (value ?? 0), 0)
}
