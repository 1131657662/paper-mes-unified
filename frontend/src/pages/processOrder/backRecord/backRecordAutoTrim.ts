import { decimalPlaces } from '../../../utils/numberFormatters'
import type { BackRecordFormValues } from './backRecordUtils'
import type { BackRecordWorkItem, WorkbenchFinish } from './backRecordWorkbenchTypes'

interface AutoTrimOptions {
  autoTrimUuids: Set<string>
  manualTrimUuids: Set<string>
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

  const officialFinishes = item.finishes.filter((entry) => isOfficialFinish(entry, values))
  if (officialFinishes.length === 0 || !allOfficialWeightsFilled(officialFinishes, values)) return []

  const sourceWeight = values.rolls?.[item.roll.uuid]?.actualWeight ?? item.roll.actualWeight
  if (sourceWeight == null || sourceWeight <= 0) return []

  const remainder = sourceWeight - officialTotal(officialFinishes, values) - lossTotal(item, values) - scrapTotal(item, values)
  if (remainder < 0) return []

  const editableTrims = trimFinishes.filter(({ finish }) =>
    !options.manualTrimUuids.has(finish.uuid)
    && (values.finishes?.[finish.uuid]?.actualWeight == null || options.autoTrimUuids.has(finish.uuid)))
  if (editableTrims.length === 0) return []

  const weights = distributeWeight(remainder, trimFinishes.length, decimalPlaces(sourceWeight, 3))
  return trimFinishes
    .map((entry, index) => ({
      uuid: entry.finish.uuid,
      actualWeight: weights[index] ?? 0,
    }))
    .filter((entry) => editableTrims.some(({ finish }) => finish.uuid === entry.uuid))
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
    return actualWeight != null && actualWeight > 0
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

function distributeWeight(total: number, count: number, digits: number) {
  if (count <= 0) return []
  const scale = 10 ** Math.max(0, digits)
  const units = Math.max(0, Math.round(total * scale))
  const base = Math.floor(units / count)
  let remainder = units - base * count
  return Array.from({ length: count }, () => {
    const value = base + (remainder > 0 ? 1 : 0)
    remainder -= remainder > 0 ? 1 : 0
    return value / scale
  })
}

function sum(values: Array<number | undefined>) {
  return values.reduce<number>((total, value) => total + (value ?? 0), 0)
}
