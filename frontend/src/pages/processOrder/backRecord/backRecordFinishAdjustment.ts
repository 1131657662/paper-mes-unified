import type {
  BackRecordAddedFinishValues,
  BackRecordFinishAdjustmentValues,
  FinishRoll,
} from '../../../types/processOrder'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

export function officialFinishUuids(item: BackRecordWorkItem): string[] {
  return item.finishes
    .filter(({ finish }) => finish.isSpare !== 1 && finish.isRemain !== 1)
    .map(({ finish }) => finish.uuid)
}

export function defaultFinishAdjustment(item: BackRecordWorkItem): BackRecordFinishAdjustmentValues {
  return {
    plannedFinishUuids: officialFinishUuids(item),
    producedFinishUuids: officialFinishUuids(item),
    reason: '',
    added: [],
  }
}

export function normalizeFinishAdjustment(
  item: BackRecordWorkItem,
  adjustment?: BackRecordFinishAdjustmentValues,
): BackRecordFinishAdjustmentValues {
  const planned = officialFinishUuids(item)
  if (!adjustment) return defaultFinishAdjustment(item)
  const produced = new Set(adjustment.producedFinishUuids ?? [])
  return {
    plannedFinishUuids: planned,
    producedFinishUuids: planned.filter((uuid) => produced.has(uuid)),
    reason: adjustment.reason ?? '',
    added: adjustment.added ?? [],
  }
}

export function isFinishProduced(
  finishUuid: string,
  adjustment?: BackRecordFinishAdjustmentValues,
): boolean {
  if (!adjustment || !adjustment.plannedFinishUuids.includes(finishUuid)) return true
  return adjustment.producedFinishUuids.includes(finishUuid)
}

export function visibleFinishEntries(
  item: BackRecordWorkItem,
  adjustment?: BackRecordFinishAdjustmentValues,
) {
  const normalized = normalizeFinishAdjustment(item, adjustment)
  return item.finishes.filter(({ finish }) => {
    const isPlanned = finish.isSpare !== 1 && finish.isRemain !== 1
    return !isPlanned || isFinishProduced(finish.uuid, normalized)
  })
}

export function addedFinishKey(itemKey: string, index: number): string {
  return `added-${itemKey}-${index}`
}

export function nextAddedFinishIndex(
  itemKey: string,
  added: BackRecordAddedFinishValues[],
): number {
  const usedKeys = new Set(added.map(({ uuid }) => uuid))
  const prefix = `added-${itemKey}-`
  let index = added.reduce((next, { uuid }) => {
    if (!uuid.startsWith(prefix)) return next
    const parsed = Number(uuid.slice(prefix.length))
    return Number.isInteger(parsed) && parsed >= next ? parsed + 1 : next
  }, 0)
  while (usedKeys.has(addedFinishKey(itemKey, index))) index += 1
  return index
}

export function createAddedFinish(item: BackRecordWorkItem, index: number): BackRecordAddedFinishValues {
  const sourceUuid = item.roll?.uuid ?? item.rollProductions[0]?.originalUuid
  return {
    uuid: addedFinishKey(item.key, index),
    originalUuid: sourceUuid ?? '',
  }
}

export function addedFinishEntity(value: BackRecordAddedFinishValues, source?: FinishRoll): FinishRoll {
  return {
    uuid: value.uuid,
    finishRollNo: '待生成',
    rowSort: source?.rowSort,
    isSpare: 0,
    isRemain: 0,
    sourceType: 1,
    paperName: source?.paperName,
    gramWeight: source?.gramWeight,
    finishWidth: value.finishWidth,
    finishDiameter: value.finishDiameter,
    finishCoreDiameter: value.finishCoreDiameter,
    actualWeight: value.actualWeight,
    scrapWeight: value.scrapWeight,
    isAbnormal: value.isAbnormal,
    abnormalType: value.abnormalType,
    actualRemark: value.actualRemark,
    rollNoStatus: 1,
    finishStatus: 1,
    productionResult: 4,
  }
}

export function addedFinishEntities(item: BackRecordWorkItem, adjustment?: BackRecordFinishAdjustmentValues): FinishRoll[] {
  return (adjustment?.added ?? []).map((value) => addedFinishEntity(value, item.finishes[0]?.finish))
}
