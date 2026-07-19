import { useDeferredValue, useState } from 'react'
import { useAvailableFinishPage } from '../../features/delivery/hooks/useAvailableFinishPage'
import type { AvailableFinishVO } from '../../types/delivery'
import { filterDeliveryFinishes, defaultDeliveryFinishFilters, type DeliveryFinishFilters } from './deliveryFinishFilter'
import { filterFinishesByScope, type DeliveryFinishScope } from './deliveryFinishScope'
import { mergeAvailableFinishRows, selectedDeliveryFinishes } from './deliverySelectionModel'

export function useDeliveryCreateInventory(customerUuid: string | undefined, warehouseUuid: string | undefined,
                                           initialFinishUuids: string[]) {
  const [scope, setScopeState] = useState<DeliveryFinishScope>('product')
  const [filters, setFiltersState] = useState<DeliveryFinishFilters>({ ...defaultDeliveryFinishFilters })
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>(initialFinishUuids)
  const [selectedByUuid, setSelectedByUuid] = useState<Record<string, AvailableFinishVO>>({})
  const deferredKeyword = useDeferredValue(filters.keyword.trim())
  const query = useAvailableFinishPage({
    customerUuid: customerUuid ?? '', warehouseUuid, current: page, size: pageSize, scope,
    keyword: deferredKeyword || undefined, sourceIssue: filters.sourceIssue,
  })
  const initialQuery = useAvailableFinishPage({
    customerUuid: customerUuid ?? '', warehouseUuid, current: 1, size: 100, scope: 'all',
    finishUuids: initialFinishUuids,
  }, initialFinishUuids.length > 0)
  const pageRows = query.data?.records ?? []
  const selectionPool = uniqueRows([...Object.values(selectedByUuid), ...(initialQuery.data?.records ?? [])])
  const selectedRows = selectedDeliveryFinishes(selectionPool, selectedKeys)
  const scopeTotals = query.data?.scopeCounts
  const visibleRows = filters.selectedOnly
    ? filterDeliveryFinishes(filterFinishesByScope(selectedRows, scope), filters, selectedKeys)
    : pageRows

  const changeCustomer = () => {
    setScopeState('product'); setFiltersState({ ...defaultDeliveryFinishFilters }); setPage(1)
    setSelectedKeys([]); setSelectedByUuid({})
  }
  const changeWarehouse = () => { setPage(1); setSelectedKeys([]); setSelectedByUuid({}) }
  const changeScope = (next: DeliveryFinishScope) => { setScopeState(next); setPage(1) }
  const changeFilters = (next: DeliveryFinishFilters) => { setFiltersState(next); setPage(1) }
  const changeSelection = (keys: React.Key[]) => {
    setSelectedKeys(keys)
    setSelectedByUuid((current) => mergeAvailableFinishRows(current, keys, [...pageRows, ...selectionPool]))
  }
  const removeSelected = (finishUuid: string) => changeSelection(
    selectedKeys.filter((key) => String(key) !== finishUuid),
  )
  const changePage = (nextPage: number, nextSize: number) => {
    setPage(nextPage); setPageSize(nextSize)
  }

  return {
    changeCustomer, changeFilters, changePage, changeScope, changeSelection, changeWarehouse, filters,
    initialQuery, page, pageSize, query, removeSelected, scope, scopeTotals, selectedKeys, selectedRows,
    visibleRows,
  }
}

function uniqueRows(rows: AvailableFinishVO[]) {
  return Array.from(new Map(rows.map((row) => [row.finishUuid, row])).values())
}
