import type { SorterResult } from 'antd/es/table/interface'
import type { DeliveryDetail } from '../../types/delivery'
import type { DeliveryCustomerSortField, DeliveryCustomerSortSpec } from '../../types/deliverySort'
import { compareValues, isEmpty } from '../../pages/delivery/deliveryDetailSorting'
import type { DeliveryCustomerSpec } from './deliveryCustomerSpecTypes'

export interface DeliveryCustomerTableRow {
  detail?: DeliveryDetail
  spec: DeliveryCustomerSpec
}

export const DELIVERY_CUSTOMER_SORT_FIELDS = new Set<DeliveryCustomerSortField>([
  'finishRollNo', 'customerPaperName', 'customerSpecification', 'customerDisplayWeight',
  'orderNo', 'customerRemark', 'sourceMotherRoll',
])

export function isDeliveryCustomerSortSpec(value: unknown): value is DeliveryCustomerSortSpec {
  if (!value || typeof value !== 'object') return false
  const item = value as Record<string, unknown>
  return DELIVERY_CUSTOMER_SORT_FIELDS.has(item.field as DeliveryCustomerSortField)
    && (item.direction === 'asc' || item.direction === 'desc')
}

export function sortDeliveryCustomerRows<T extends DeliveryCustomerTableRow>(
  rows: T[], sortChain: DeliveryCustomerSortSpec[],
): T[] {
  if (!sortChain.length) return rows
  return rows.map((row, index) => ({ row, index }))
    .sort((left, right) => compareRows(left, right, sortChain))
    .map(({ row }) => row)
}

export function updateDeliveryCustomerSortChain(
  current: DeliveryCustomerSortSpec[],
  sorter: SorterResult<DeliveryCustomerTableRow> | SorterResult<DeliveryCustomerTableRow>[],
) {
  const values = Array.isArray(sorter) ? sorter : [sorter]
  const active = values.map(toSortSpec).filter((item): item is DeliveryCustomerSortSpec => item !== null)
  if (!active.length) return []
  const activeByField = new Map(active.map((item) => [item.field, item.direction]))
  const next = current.filter((item) => activeByField.has(item.field))
    .map((item) => ({ ...item, direction: activeByField.get(item.field) ?? item.direction }))
  const currentFields = new Set(current.map((item) => item.field))
  active.forEach((item) => { if (!currentFields.has(item.field)) next.push(item) })
  return next
}

function toSortSpec(value: SorterResult<DeliveryCustomerTableRow>): DeliveryCustomerSortSpec | null {
  const field = value.field ?? value.columnKey
  if (!DELIVERY_CUSTOMER_SORT_FIELDS.has(field as DeliveryCustomerSortField)) return null
  if (value.order !== 'ascend' && value.order !== 'descend') return null
  return { field: field as DeliveryCustomerSortField, direction: value.order === 'ascend' ? 'asc' : 'desc' }
}

function compareRows(
  left: { row: DeliveryCustomerTableRow; index: number },
  right: { row: DeliveryCustomerTableRow; index: number },
  sortChain: DeliveryCustomerSortSpec[],
) {
  for (const spec of sortChain) {
    const leftValue = sortValue(left.row, spec.field)
    const rightValue = sortValue(right.row, spec.field)
    const result = compareValues(leftValue, rightValue)
    if (isEmpty(leftValue) || isEmpty(rightValue)) {
      if (result !== 0) return result
      continue
    }
    if (result !== 0) return spec.direction === 'asc' ? result : -result
  }
  return left.index - right.index
}

function sortValue(row: DeliveryCustomerTableRow, field: DeliveryCustomerSortField): string | number | undefined {
  const { spec, detail } = row
  if (field === 'customerSpecification') return specificationText(spec)
  if (field === 'sourceMotherRoll') return detail?.originalSummary ?? detail?.originalRollNos
  return spec[field]
}

function specificationText(spec: DeliveryCustomerSpec) {
  const gram = spec.customerGramWeight == null ? '' : String(spec.customerGramWeight)
  const width = spec.customerFinishWidth == null ? '' : String(spec.customerFinishWidth)
  return gram || width ? `${gram}/${width}` : undefined
}
