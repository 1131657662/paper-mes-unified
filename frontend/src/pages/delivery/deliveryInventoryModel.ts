import type {
  DeliveryInventoryFilter,
  DeliveryInventoryStockState,
  DeliveryInventoryType,
} from '../../types/deliveryInventory'

export type DeliveryInventoryView = 'customers' | 'finishes'

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
