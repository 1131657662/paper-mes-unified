import type { FinishedProductRow } from '../processOrderDetail/components/finishedProductRows'
import type { FinishCustomerSpec } from './customerSpecTypes'

export interface CustomerSpecificationGroup {
  key: string
  paperName?: string
  gramWeight?: number
  width?: number
  count: number
  weight: number
  physicalSpecifications: string[]
}

export function indexCustomerSpecs(items: FinishCustomerSpec[] = []) {
  return new Map(items.map((item) => [item.finishUuid, item]))
}

export function buildCustomerSpecificationGroups(
  rows: FinishedProductRow[],
  items: FinishCustomerSpec[] = [],
): CustomerSpecificationGroup[] {
  const specIndex = indexCustomerSpecs(items)
  const grouped = new Map<string, CustomerSpecificationGroup>()
  for (const row of rows.filter(isCustomerRow)) {
    const item = specIndex.get(row.finish.uuid)
    const values = customerValues(row, item)
    const key = [values.paperName, values.gramWeight, values.width].join('|')
    const group = grouped.get(key) ?? { ...values, key, count: 0, weight: 0, physicalSpecifications: [] }
    group.count += 1
    group.weight += values.weight
    const physical = physicalSpecification(row)
    if (!group.physicalSpecifications.includes(physical)) group.physicalSpecifications.push(physical)
    grouped.set(key, group)
  }
  return [...grouped.values()]
}

export function customerSpecForRow(row: FinishedProductRow, items: FinishCustomerSpec[] = []) {
  const item = items.find(({ finishUuid }) => finishUuid === row.finish.uuid)
  return customerValues(row, item)
}

export function customerSpecificationLabel(item: FinishCustomerSpec) {
  return [item.customerPaperName, formatGram(item.customerGramWeight), formatWidth(item.customerFinishWidth)]
    .filter(Boolean)
    .join(' / ')
}

export function physicalSpecificationLabel(item: FinishCustomerSpec) {
  return [item.physicalPaperName, formatGram(item.physicalGramWeight), formatWidth(item.physicalFinishWidth)]
    .filter(Boolean)
    .join(' / ')
}

function customerValues(row: FinishedProductRow, item?: FinishCustomerSpec) {
  const finish = row.finish
  return {
    paperName: item?.customerPaperName ?? finish.paperName,
    gramWeight: item?.customerGramWeight ?? finish.gramWeight,
    width: item?.customerFinishWidth ?? finish.finishWidth,
    weight: item?.customerDisplayWeight ?? finish.actualWeight ?? finish.estimateWeight ?? 0,
  }
}

function physicalSpecification(row: FinishedProductRow) {
  return [row.finish.paperName, formatGram(row.finish.gramWeight), formatWidth(row.finish.finishWidth)]
    .filter(Boolean)
    .join(' / ')
}

function isCustomerRow({ finish }: FinishedProductRow) {
  return finish.isRemain !== 1 && finish.isSpare !== 1 && finish.rollNoStatus !== 3
}

const formatGram = (value?: number) => value == null ? undefined : `${value}g`
const formatWidth = (value?: number) => value == null ? undefined : `${value}mm`
