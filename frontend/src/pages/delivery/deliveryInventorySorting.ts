import type { SorterResult } from 'antd/es/table/interface'
import type { DeliveryInventoryFinish, DeliveryInventoryOrderGroup } from '../../types/deliveryInventory'
import { compareValues, isEmpty } from './deliveryDetailSorting'

export type InventorySortDirection = 'asc' | 'desc'

export type DeliveryInventoryFinishSortField =
  | 'finishRollNo'
  | 'orderNo'
  | 'paperName'
  | 'specification'
  | 'remainingWeight'
  | 'warehouseName'
  | 'stockInTime'
  | 'inventoryType'
  | 'stockState'
  | 'deliveryNo'

export interface DeliveryInventoryFinishSortSpec {
  field: DeliveryInventoryFinishSortField
  direction: InventorySortDirection
}

export type DeliveryInventoryOrderGroupSortField =
  | 'orderNo'
  | 'orderDate'
  | 'totalRollCount'
  | 'totalWeight'
  | 'availableRollCount'
  | 'lockedRollCount'

export interface DeliveryInventoryOrderGroupSortSpec {
  field: DeliveryInventoryOrderGroupSortField
  direction: InventorySortDirection
}

export const DELIVERY_INVENTORY_FINISH_SORT_FIELDS = new Set<DeliveryInventoryFinishSortField>([
  'finishRollNo', 'orderNo', 'paperName', 'specification', 'remainingWeight',
  'warehouseName', 'stockInTime', 'inventoryType', 'stockState', 'deliveryNo',
])

export const DELIVERY_INVENTORY_ORDER_GROUP_SORT_FIELDS = new Set<DeliveryInventoryOrderGroupSortField>([
  'orderNo', 'orderDate', 'totalRollCount', 'totalWeight', 'availableRollCount', 'lockedRollCount',
])

export function isDeliveryInventoryFinishSortSpec(value: unknown): value is DeliveryInventoryFinishSortSpec {
  return isSortSpec(value, DELIVERY_INVENTORY_FINISH_SORT_FIELDS)
}

export function isDeliveryInventoryOrderGroupSortSpec(value: unknown): value is DeliveryInventoryOrderGroupSortSpec {
  return isSortSpec(value, DELIVERY_INVENTORY_ORDER_GROUP_SORT_FIELDS)
}

export function sortDeliveryInventoryFinishes(
  rows: DeliveryInventoryFinish[], sortChain: DeliveryInventoryFinishSortSpec[],
) {
  return sortRows(rows, sortChain, finishSortValue)
}

export function sortDeliveryInventoryOrderGroups(
  rows: DeliveryInventoryOrderGroup[], sortChain: DeliveryInventoryOrderGroupSortSpec[],
) {
  return sortRows(rows, sortChain, groupSortValue)
}

export function updateDeliveryInventoryFinishSortChain(
  current: DeliveryInventoryFinishSortSpec[],
  sorter: SorterResult<DeliveryInventoryFinish> | SorterResult<DeliveryInventoryFinish>[],
) {
  return updateSortChain(current, sorter, DELIVERY_INVENTORY_FINISH_SORT_FIELDS)
}

export function updateDeliveryInventoryOrderGroupSortChain(
  current: DeliveryInventoryOrderGroupSortSpec[],
  sorter: SorterResult<DeliveryInventoryOrderGroup> | SorterResult<DeliveryInventoryOrderGroup>[],
) {
  return updateSortChain(current, sorter, DELIVERY_INVENTORY_ORDER_GROUP_SORT_FIELDS)
}

function isSortSpec<TField extends string>(value: unknown, fields: ReadonlySet<TField>): value is { field: TField; direction: InventorySortDirection } {
  if (!value || typeof value !== 'object') return false
  const item = value as Record<string, unknown>
  return typeof item.field === 'string' && fields.has(item.field as TField)
    && (item.direction === 'asc' || item.direction === 'desc')
}

function updateSortChain<TField extends string, TRow>(
  current: { field: TField; direction: InventorySortDirection }[],
  sorter: SorterResult<TRow> | SorterResult<TRow>[],
  fields: ReadonlySet<TField>,
) {
  const active = (Array.isArray(sorter) ? sorter : [sorter])
    .map((item) => toSortSpec(item, fields))
    .filter((item): item is { field: TField; direction: InventorySortDirection } => item !== null)
  if (!active.length) return []
  const activeByField = new Map(active.map((item) => [item.field, item.direction]))
  const next = current.filter((item) => activeByField.has(item.field))
    .map((item) => ({ ...item, direction: activeByField.get(item.field) ?? item.direction }))
  const currentFields = new Set(current.map((item) => item.field))
  active.forEach((item) => { if (!currentFields.has(item.field)) next.push(item) })
  return next
}

function toSortSpec<TField extends string, TRow>(
  value: SorterResult<TRow>, fields: ReadonlySet<TField>,
) {
  const field = value.field ?? value.columnKey
  if (typeof field !== 'string' || !fields.has(field as TField)) return null
  if (value.order !== 'ascend' && value.order !== 'descend') return null
  return { field: field as TField, direction: value.order === 'ascend' ? 'asc' as const : 'desc' as const }
}

function sortRows<TRow, TField extends string>(
  rows: TRow[], sortChain: { field: TField; direction: InventorySortDirection }[],
  getValue: (row: TRow, field: TField) => string | number | undefined,
) {
  if (!sortChain.length) return rows
  return rows.map((row, index) => ({ row, index })).sort((left, right) => {
    for (const spec of sortChain) {
      const leftValue = getValue(left.row, spec.field)
      const rightValue = getValue(right.row, spec.field)
      const result = compareValues(leftValue, rightValue)
      if (isEmpty(leftValue) || isEmpty(rightValue)) {
        if (result !== 0) return result
        continue
      }
      if (result !== 0) return spec.direction === 'asc' ? result : -result
    }
    return left.index - right.index
  }).map(({ row }) => row)
}

function finishSortValue(row: DeliveryInventoryFinish, field: DeliveryInventoryFinishSortField) {
  if (field === 'specification') return [row.gramWeight, row.finishWidth, row.finishDiameter, row.finishCoreDiameter].map(value => value ?? '').join('/')
  if (field === 'inventoryType') return row.isRemain === 1 ? 1 : row.sourceType ?? 0
  return row[field]
}

function groupSortValue(row: DeliveryInventoryOrderGroup, field: DeliveryInventoryOrderGroupSortField) {
  return row[field]
}
