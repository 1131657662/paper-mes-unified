import type {
  DeliveryInventoryFilter,
  DeliveryInventoryScope,
  DeliveryInventoryStockState,
  DeliveryInventoryType,
} from '../../types/deliveryInventory'

export type DeliveryInventoryView = 'customers' | 'finishes'
export type DeliveryInventoryQuickFilter = 'all' | DeliveryInventoryScope | 'direct'

export function isDeliveryInventoryView(value: string | number): value is DeliveryInventoryView {
  return value === 'customers' || value === 'finishes'
}

export const EMPTY_INVENTORY_FILTER: DeliveryInventoryFilter = {}

export function stockStateFrom(value: string | number): DeliveryInventoryStockState | undefined {
  return value === 1 || value === 2 ? value : undefined
}

export function inventoryTypeFrom(value?: number): DeliveryInventoryType | undefined {
  return value === 1 || value === 2 || value === 3 ? value : undefined
}

export function inventoryQuickFilterFrom(value: string | number): DeliveryInventoryQuickFilter {
  if (value === 'product' || value === 'remain' || value === 'direct') return value
  return 'all'
}

export function inventoryQuickFilterValue(filters: DeliveryInventoryFilter): DeliveryInventoryQuickFilter {
  if (filters.inventoryScope) return filters.inventoryScope
  if (filters.inventoryType === 3) return 'direct'
  if (filters.inventoryType === 2) return 'remain'
  if (filters.inventoryType === 1) return 'product'
  return 'all'
}

export function inventoryProductLabel(filters: DeliveryInventoryFilter): string {
  return filters.inventoryType === 1 && !filters.inventoryScope ? '普通成品' : '成品（含直发）'
}

export function filtersForInventoryQuickFilter(
  filters: DeliveryInventoryFilter,
  value: DeliveryInventoryQuickFilter,
): DeliveryInventoryFilter {
  const next = { ...filters, inventoryScope: undefined, inventoryType: undefined }
  if (value === 'direct') return { ...next, inventoryType: 3 }
  if (value === 'product' || value === 'remain') return { ...next, inventoryScope: value }
  return next
}

export function inventoryTypeText(isRemain?: number, sourceType?: number): string {
  if (isRemain === 1) return '余料'
  if (sourceType === 2) return '原纸直发'
  if (sourceType === 3) return '整理成品'
  return '普通成品'
}

export function mergeInventorySelection(
  current: Record<string, import('../../types/deliveryInventory').DeliveryInventoryFinish>,
  keys: React.Key[],
  rows: import('../../types/deliveryInventory').DeliveryInventoryFinish[],
) {
  const selected = new Set(keys.map(String))
  const next = Object.fromEntries(Object.entries(current).filter(([uuid]) => selected.has(uuid)))
  for (const row of rows) if (selected.has(row.finishUuid)) next[row.finishUuid] = row
  return next
}
