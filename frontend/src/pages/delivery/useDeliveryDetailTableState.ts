import { useCallback } from 'react'
import type { TableProps } from 'antd'
import { useTableSortState } from '../../hooks/useTableSortState'
import type { DeliveryDetail } from '../../types/delivery'
import type { DeliveryCustomerSortSpec } from '../../types/deliverySort'
import {
  isDeliveryCustomerSortSpec,
  updateDeliveryCustomerSortChain,
} from '../../features/deliveryCustomerSpec/deliveryCustomerSorting'
import {
  isDeliverySortSpec,
  updateDeliverySortChain,
  type DeliverySortSpec,
} from './deliveryDetailSorting'

export const DELIVERY_DETAIL_TABLE_STORAGE_KEY = 'table-columns-delivery-detail-items'

export function useDeliveryDetailTableState() {
  const physical = usePhysicalSortState()
  const customer = useCustomerSortState('customer')
  const trace = useCustomerSortState('trace')
  return { physical, customer, trace }
}

function usePhysicalSortState() {
  const { sortChain, updateSortChain } = useTableSortState<DeliverySortSpec>(
    DELIVERY_DETAIL_TABLE_STORAGE_KEY,
    isDeliverySortSpec,
    'physical',
  )
  const onChange = useCallback<NonNullable<TableProps<DeliveryDetail>['onChange']>>(
    (_pagination, _filters, sorter) => updateSortChain(updateDeliverySortChain(sortChain, sorter)),
    [sortChain, updateSortChain],
  )
  return { sortChain, onChange, clearSort: () => updateSortChain([]) }
}

function useCustomerSortState(scope: 'customer' | 'trace') {
  const { sortChain, updateSortChain } = useTableSortState<DeliveryCustomerSortSpec>(
    DELIVERY_DETAIL_TABLE_STORAGE_KEY,
    isDeliveryCustomerSortSpec,
    scope,
  )
  const onChange = useCallback<NonNullable<TableProps<import('../../features/deliveryCustomerSpec/deliveryCustomerSorting').DeliveryCustomerTableRow>['onChange']>>(
    (_pagination, _filters, sorter) => updateSortChain(updateDeliveryCustomerSortChain(sortChain, sorter)),
    [sortChain, updateSortChain],
  )
  return { sortChain, onChange, clearSort: () => updateSortChain([]) }
}
