import type { FinishProductionVO } from '../../../types/processOrder'
import type { FinishedProductRow } from './finishedProductRows'

export type PhysicalProductType = 'FINISH' | 'SPARE' | 'TRIM'

export interface PhysicalSpecificationGroup {
  actualWeight?: number
  count: number
  difference?: number
  estimateWeight: number
  gramWeight?: number
  key: string
  paperName?: string
  productType: PhysicalProductType
  recordedCount: number
  width?: number
}

export interface PhysicalSpecificationTotals {
  actualWeight?: number
  count: number
  difference?: number
  estimateWeight: number
  recordedCount: number
}

export function buildPhysicalSpecificationGroups(
  rows: FinishedProductRow[],
): PhysicalSpecificationGroup[] {
  const groups = new Map<string, PhysicalSpecificationGroup>()
  for (const { finish } of rows.filter(isActiveFinish)) {
    const key = groupKey(finish)
    const group = groups.get(key) ?? emptyGroup(key, finish)
    addFinish(group, finish)
    groups.set(key, group)
  }
  return [...groups.values()].map(withDifference).sort(compareGroups)
}

export function calculatePhysicalSpecificationTotals(
  groups: PhysicalSpecificationGroup[],
): PhysicalSpecificationTotals {
  const totals = groups.reduce<PhysicalSpecificationTotals>((result, group) => ({
    actualWeight: addOptional(result.actualWeight, group.actualWeight),
    count: result.count + group.count,
    estimateWeight: result.estimateWeight + group.estimateWeight,
    recordedCount: result.recordedCount + group.recordedCount,
  }), { count: 0, estimateWeight: 0, recordedCount: 0 })
  return withDifference(totals)
}

function emptyGroup(key: string, finish: FinishProductionVO): PhysicalSpecificationGroup {
  return {
    count: 0,
    estimateWeight: 0,
    gramWeight: finish.gramWeight,
    key,
    paperName: finish.paperName,
    productType: productType(finish),
    recordedCount: 0,
    width: finish.finishWidth,
  }
}

function addFinish(group: PhysicalSpecificationGroup, finish: FinishProductionVO) {
  group.count += 1
  group.estimateWeight += finish.estimateWeight ?? 0
  if (finish.actualWeight == null) return
  group.actualWeight = (group.actualWeight ?? 0) + finish.actualWeight
  group.recordedCount += 1
}

function withDifference<T extends PhysicalSpecificationTotals>(values: T): T {
  if (values.recordedCount !== values.count || values.actualWeight == null) return values
  return { ...values, difference: values.actualWeight - values.estimateWeight }
}

function groupKey(finish: FinishProductionVO) {
  return JSON.stringify([
    finish.paperName ?? null,
    finish.gramWeight ?? null,
    finish.finishWidth ?? null,
    productType(finish),
  ])
}

function productType(finish: FinishProductionVO): PhysicalProductType {
  if (finish.isRemain === 1) return 'TRIM'
  if (finish.isSpare === 1) return 'SPARE'
  return 'FINISH'
}

function isActiveFinish({ finish }: FinishedProductRow) {
  return finish.rollNoStatus !== 3
}

function addOptional(left?: number, right?: number) {
  if (left == null) return right
  if (right == null) return left
  return left + right
}

function compareGroups(left: PhysicalSpecificationGroup, right: PhysicalSpecificationGroup) {
  const typeOrder = { FINISH: 0, SPARE: 1, TRIM: 2 } satisfies Record<PhysicalProductType, number>
  return typeOrder[left.productType] - typeOrder[right.productType]
    || (left.paperName ?? '').localeCompare(right.paperName ?? '', 'zh-CN')
    || (left.gramWeight ?? 0) - (right.gramWeight ?? 0)
    || (left.width ?? 0) - (right.width ?? 0)
}
