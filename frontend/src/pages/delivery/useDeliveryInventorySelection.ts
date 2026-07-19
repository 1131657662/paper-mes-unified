import { useState } from 'react'
import type { DeliveryInventoryFinish, DeliveryInventoryOrderGroup } from '../../types/deliveryInventory'

export function useDeliveryInventorySelection() {
  const [selectedByUuid, setSelectedByUuid] = useState<Record<string, DeliveryInventoryFinish>>({})
  const selected = Object.values(selectedByUuid)
  const selectedWarehouseUuid = selected[0]?.warehouseUuid
  const selectionDisabled = (row: DeliveryInventoryFinish) => row.stockState !== 1
    || Boolean(selectedWarehouseUuid && row.warehouseUuid !== selectedWarehouseUuid)
  const toggleSelection = (row: DeliveryInventoryFinish, checked: boolean) => {
    if (checked && selectionDisabled(row)) return
    setSelectedByUuid((current) => toggleRow(current, row, checked))
  }
  const toggleGroup = (group: DeliveryInventoryOrderGroup, checked: boolean) => {
    const warehouseUuid = selectedWarehouseUuid
      || group.finishes.find((row) => row.stockState === 1)?.warehouseUuid
    setSelectedByUuid((current) => toggleGroupRows(current, group, warehouseUuid, checked))
  }
  const changeSelection = (keys: React.Key[], selectedRows: DeliveryInventoryFinish[]) => {
    setSelectedByUuid((current) => selectedRowsByKey(current, selectedRows, keys))
  }
  return {
    changeSelection, clearSelection: () => setSelectedByUuid({}), selected, selectedByUuid,
    selectedKeys: selected.map((item) => item.finishUuid), selectedWarehouseUuid,
    selectionDisabled, toggleGroup, toggleSelection,
  }
}

function toggleRow(current: Record<string, DeliveryInventoryFinish>, row: DeliveryInventoryFinish, checked: boolean) {
  const next = { ...current }
  if (checked) next[row.finishUuid] = row
  else delete next[row.finishUuid]
  return next
}

function toggleGroupRows(current: Record<string, DeliveryInventoryFinish>, group: DeliveryInventoryOrderGroup,
  warehouseUuid: string | undefined, checked: boolean) {
  const next = { ...current }
  group.finishes.filter((row) => row.stockState === 1 && row.warehouseUuid === warehouseUuid)
    .forEach((row) => { if (checked) next[row.finishUuid] = row; else delete next[row.finishUuid] })
  return next
}

function selectedRowsByKey(current: Record<string, DeliveryInventoryFinish>, rows: DeliveryInventoryFinish[], keys: React.Key[]) {
  const pool = new Map([...Object.values(current), ...rows].map((row) => [row.finishUuid, row]))
  const selected = keys.map(String).map((key) => pool.get(key)).filter(Boolean) as DeliveryInventoryFinish[]
  const warehouseUuid = selected[0]?.warehouseUuid
  return Object.fromEntries(selected.filter((row) => row.warehouseUuid === warehouseUuid)
    .map((row) => [row.finishUuid, row]))
}
