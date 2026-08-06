import { useCallback } from 'react'
import type { TableProps } from 'antd'
import { useTableSortState } from '../../../hooks/useTableSortState'
import type { CustomerSpecificationGroup } from '../../processOrderCustomerSpec/customerSpecModel'
import type { PhysicalSpecificationGroup } from './physicalSpecificationModel'
import type { FinishedProductRow } from './finishedProductRows'
import type {
  CustomerComparisonSortSpec,
  CustomerSpecificationComparisonRow,
  CustomerSummarySortSpec,
  PhysicalItemsSortSpec,
  PhysicalSummarySortSpec,
} from './finishedProductsSorting'
import {
  isCustomerComparisonSortSpec,
  isCustomerSummarySortSpec,
  isPhysicalItemsSortSpec,
  isPhysicalSummarySortSpec,
  updateCustomerComparisonSortChain,
  updateCustomerSummarySortChain,
  updatePhysicalItemsSortChain,
  updatePhysicalSummarySortChain,
} from './finishedProductsSorting'

export const FINISHED_PRODUCTS_STORAGE_KEY = 'table-columns-process-order-finished-products'

export interface FinishedProductsSortController<TRow, TSort> {
  sortChain: TSort[]
  onChange: NonNullable<TableProps<TRow>['onChange']>
  clearSort: () => void
}

export interface FinishedProductsSortState {
  customerSummary: FinishedProductsSortController<CustomerSpecificationGroup, CustomerSummarySortSpec>
  customerItems: FinishedProductsSortController<CustomerSpecificationComparisonRow, CustomerComparisonSortSpec>
  physicalSummary: FinishedProductsSortController<PhysicalSpecificationGroup, PhysicalSummarySortSpec>
  physicalItems: FinishedProductsSortController<FinishedProductRow, PhysicalItemsSortSpec>
  clearAll: () => void
}

export function useFinishedProductsSortState(): FinishedProductsSortState {
  const customerSummary = useController('customerSummary', isCustomerSummarySortSpec, updateCustomerSummarySortChain)
  const customerItems = useController('customerItems', isCustomerComparisonSortSpec, updateCustomerComparisonSortChain)
  const physicalSummary = useController('physicalSummary', isPhysicalSummarySortSpec, updatePhysicalSummarySortChain)
  const physicalItems = useController('physicalItems', isPhysicalItemsSortSpec, updatePhysicalItemsSortChain)
  return { customerSummary, customerItems, physicalSummary, physicalItems, clearAll: () => {
    customerSummary.clearSort(); customerItems.clearSort(); physicalSummary.clearSort(); physicalItems.clearSort()
  } }
}

function useController<TRow, TSort extends { field: string; direction: 'asc' | 'desc' }>(
  scope: string,
  isValid: (value: unknown) => value is TSort,
  update: (current: TSort[], sorter: NonNullable<TableProps<TRow>['onChange']> extends (...args: infer Args) => unknown ? Args[2] : never) => TSort[],
): FinishedProductsSortController<TRow, TSort> {
  const { sortChain, updateSortChain } = useTableSortState<TSort>(FINISHED_PRODUCTS_STORAGE_KEY, isValid, scope)
  const onChange = useCallback<NonNullable<TableProps<TRow>['onChange']>>(
    (_pagination, _filters, sorter) => updateSortChain(update(sortChain, sorter)),
    [sortChain, update, updateSortChain],
  )
  return { sortChain, onChange, clearSort: () => updateSortChain([]) }
}
