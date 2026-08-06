import type { SorterResult } from 'antd/es/table/interface'
import type { DeliveryDetail } from '../../types/delivery'
import type { DeliveryDetailSortField, DeliverySortSpec } from '../../types/deliverySort'
export type { DeliveryDetailSortField, DeliverySortSpec } from '../../types/deliverySort'

export const DELIVERY_DETAIL_SORT_FIELDS = new Set<DeliveryDetailSortField>([
  'orderNo', 'finishRollNo', 'paperName', 'gramWeight', 'spec', 'actualWeight',
  'outWeight', 'remainingWeight', 'originalSummary', 'remark', 'actualRemark',
])

export function isDeliverySortSpec(value: unknown): value is DeliverySortSpec {
  if (!value || typeof value !== 'object') return false
  const item = value as Record<string, unknown>
  return DELIVERY_DETAIL_SORT_FIELDS.has(item.field as DeliveryDetailSortField)
    && (item.direction === 'asc' || item.direction === 'desc')
}

export function sortDeliveryDetails(details: DeliveryDetail[], sortChain: DeliverySortSpec[]) {
  if (!sortChain.length) return details
  return details
    .map((detail, index) => ({ detail, index }))
    .sort((left, right) => compareRows(left, right, sortChain))
    .map(({ detail }) => detail)
}

export function updateDeliverySortChain(
  current: DeliverySortSpec[],
  sorter: SorterResult<DeliveryDetail> | SorterResult<DeliveryDetail>[],
) {
  const values = Array.isArray(sorter) ? sorter : [sorter]
  const active = values
    .map(toSortSpec)
    .filter((item): item is DeliverySortSpec => item !== null)
  if (!active.length) return []

  const activeByField = new Map(active.map((item) => [item.field, item.direction]))
  const next = current
    .filter((item) => activeByField.has(item.field))
    .map((item) => ({ ...item, direction: activeByField.get(item.field) ?? item.direction }))
  const currentFields = new Set(current.map((item) => item.field))
  active.forEach((item) => {
    if (!currentFields.has(item.field)) next.push(item)
  })
  return next
}

function toSortSpec(value: SorterResult<DeliveryDetail>): DeliverySortSpec | null {
  const field = value.field ?? value.columnKey
  if (!DELIVERY_DETAIL_SORT_FIELDS.has(field as DeliveryDetailSortField)) return null
  if (value.order !== 'ascend' && value.order !== 'descend') return null
  return {
    field: field as DeliveryDetailSortField,
    direction: value.order === 'ascend' ? 'asc' : 'desc',
  }
}

function compareRows(
  left: { detail: DeliveryDetail; index: number },
  right: { detail: DeliveryDetail; index: number },
  sortChain: DeliverySortSpec[],
) {
  for (const spec of sortChain) {
    const leftValue = sortValue(left.detail, spec.field)
    const rightValue = sortValue(right.detail, spec.field)
    const result = compareValues(leftValue, rightValue)
    if (isEmpty(leftValue) || isEmpty(rightValue)) {
      if (result !== 0) return result
      continue
    }
    if (result !== 0) return spec.direction === 'asc' ? result : -result
  }
  return left.index - right.index
}

function sortValue(detail: DeliveryDetail, field: DeliveryDetailSortField): string | number | undefined {
  if (field === 'spec') return detail.finishWidth
  if (field === 'originalSummary') return detail.originalSummary ?? detail.originalRollNos
  return detail[field]
}

export function isEmpty(value: string | number | undefined) {
  return value == null || value === ''
}

export function compareValues(left: string | number | undefined, right: string | number | undefined) {
  if (left == null || left === '') return right == null || right === '' ? 0 : 1
  if (right == null || right === '') return -1
  if (typeof left === 'number' && typeof right === 'number') return left - right
  return naturalCompare(String(left), String(right))
}

function naturalCompare(left: string, right: string) {
  const leftParts = left.match(/\d+|\D+/g) ?? []
  const rightParts = right.match(/\d+|\D+/g) ?? []
  const length = Math.max(leftParts.length, rightParts.length)
  for (let index = 0; index < length; index += 1) {
    const leftPart = leftParts[index]
    const rightPart = rightParts[index]
    if (leftPart == null) return -1
    if (rightPart == null) return 1
    const bothNumeric = /^\d+$/.test(leftPart) && /^\d+$/.test(rightPart)
    const result = bothNumeric
      ? compareNumericText(leftPart, rightPart)
      : leftPart < rightPart ? -1 : leftPart > rightPart ? 1 : 0
    if (result !== 0) return result
  }
  return 0
}

function compareNumericText(left: string, right: string) {
  const normalizedLeft = left.replace(/^0+(?=\d)/, '')
  const normalizedRight = right.replace(/^0+(?=\d)/, '')
  if (normalizedLeft.length !== normalizedRight.length) return normalizedLeft.length - normalizedRight.length
  return normalizedLeft < normalizedRight ? -1 : normalizedLeft > normalizedRight ? 1 : 0
}
