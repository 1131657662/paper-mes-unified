import { useCallback } from 'react'
import type { TableProps } from 'antd'
import type { DeliveryInventoryFinish, DeliveryInventoryOrderGroup } from '../../types/deliveryInventory'
import { useTableSortState } from '../../hooks/useTableSortState'
import {
  isDeliveryInventoryFinishSortSpec,
  isDeliveryInventoryOrderGroupSortSpec,
  updateDeliveryInventoryFinishSortChain,
  updateDeliveryInventoryOrderGroupSortChain,
  type DeliveryInventoryFinishSortSpec,
  type DeliveryInventoryOrderGroupSortSpec,
} from './deliveryInventorySorting'

export const DELIVERY_INVENTORY_FINISHES_STORAGE_KEY = 'table-columns-delivery-inventory-finishes'
export const DELIVERY_INVENTORY_GROUPS_STORAGE_KEY = 'table-columns-delivery-inventory-order-groups'
export const DELIVERY_INVENTORY_DETAIL_STORAGE_KEY = 'table-columns-delivery-inventory-finish-details'

export interface InventoryFinishSortController {
  sortChain: DeliveryInventoryFinishSortSpec[]
  onChange: NonNullable<TableProps<DeliveryInventoryFinish>['onChange']>
  clearSort: () => void
}

export interface InventoryGroupSortController {
  sortChain: DeliveryInventoryOrderGroupSortSpec[]
  onChange: NonNullable<TableProps<DeliveryInventoryOrderGroup>['onChange']>
  clearSort: () => void
}

export function useDeliveryInventoryCustomerSortState() {
  const finishes = useFinishController('customerDetailRolls', DELIVERY_INVENTORY_FINISHES_STORAGE_KEY)
  const groups = useGroupController('customerDetailGroups')
  const detail = useFinishController('customerDetailExpandedRolls', DELIVERY_INVENTORY_DETAIL_STORAGE_KEY)
  return { finishes, groups, detail, clearAll: () => { finishes.clearSort(); groups.clearSort(); detail.clearSort() } }
}

function useFinishController(scope: string, storageKey: string): InventoryFinishSortController {
  const { sortChain, updateSortChain } = useTableSortState<DeliveryInventoryFinishSortSpec>(storageKey, isDeliveryInventoryFinishSortSpec, scope)
  const onChange = useCallback<NonNullable<TableProps<DeliveryInventoryFinish>['onChange']>>(
    (_pagination, _filters, sorter) => updateSortChain(updateDeliveryInventoryFinishSortChain(sortChain, sorter)),
    [sortChain, updateSortChain],
  )
  return { sortChain, onChange, clearSort: () => updateSortChain([]) }
}

function useGroupController(scope: string): InventoryGroupSortController {
  const { sortChain, updateSortChain } = useTableSortState<DeliveryInventoryOrderGroupSortSpec>(
    DELIVERY_INVENTORY_GROUPS_STORAGE_KEY, isDeliveryInventoryOrderGroupSortSpec, scope,
  )
  const onChange = useCallback<NonNullable<TableProps<DeliveryInventoryOrderGroup>['onChange']>>(
    (_pagination, _filters, sorter) => updateSortChain(updateDeliveryInventoryOrderGroupSortChain(sortChain, sorter)),
    [sortChain, updateSortChain],
  )
  return { sortChain, onChange, clearSort: () => updateSortChain([]) }
}
