import { useDeferredValue, useState } from 'react'
import { useAvailableFinishPage } from '../../features/delivery/hooks/useAvailableFinishPage'
import type { AvailableFinishVO } from '../../types/delivery'
import { defaultDeliveryFinishFilters, filterDeliveryFinishes, type DeliveryFinishFilters } from './deliveryFinishFilter'
import { filterFinishesByScope, type DeliveryFinishScope } from './deliveryFinishScope'
import { mergeAvailableFinishRows, selectedDeliveryFinishes } from './deliverySelectionModel'

interface Params {
  customerUuid?: string
  enabled: boolean
  warehouseUuid?: string
}

export function useDeliveryAppendInventory({ customerUuid, enabled, warehouseUuid }: Params) {
  const [scope, setScope] = useState<DeliveryFinishScope>('product')
  const [filters, setFilters] = useState<DeliveryFinishFilters>({ ...defaultDeliveryFinishFilters })
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([])
  const [selectedByUuid, setSelectedByUuid] = useState<Record<string, AvailableFinishVO>>({})
  const keyword = useDeferredValue(filters.keyword.trim())
  const query = useAvailableFinishPage({
    customerUuid: customerUuid ?? '', warehouseUuid, current: page, size: pageSize, scope,
    keyword: keyword || undefined, sourceIssue: filters.sourceIssue,
  }, enabled)
  const pageRows = query.data?.records ?? []
  const selectionPool = uniqueRows([...Object.values(selectedByUuid), ...pageRows])
  const selectedRows = selectedDeliveryFinishes(selectionPool, selectedKeys)
  const visibleRows = filters.selectedOnly
    ? filterDeliveryFinishes(filterFinishesByScope(selectedRows, scope), filters, selectedKeys)
    : pageRows

  const changeSelection = (keys: React.Key[]) => {
    setSelectedKeys(keys)
    setSelectedByUuid((current) => mergeAvailableFinishRows(current, keys, [...selectionPool, ...pageRows]))
  }
  const changeScope = (next: DeliveryFinishScope) => { setScope(next); setPage(1) }
  const changeFilters = (next: DeliveryFinishFilters) => { setFilters(next); setPage(1) }
  const changePage = (nextPage: number, nextSize: number) => {
    setPage(nextPage); setPageSize(nextSize)
  }
  const reset = () => {
    setScope('product'); setFilters({ ...defaultDeliveryFinishFilters }); setPage(1); setPageSize(20)
    setSelectedKeys([]); setSelectedByUuid({})
  }

  return {
    changeFilters, changePage, changeScope, changeSelection, filters, page, pageSize, query, reset, scope,
    selectedKeys, selectedRows, selectionPool, visibleRows,
  }
}

function uniqueRows(rows: AvailableFinishVO[]): AvailableFinishVO[] {
  return Array.from(new Map(rows.map((row) => [row.finishUuid, row])).values())
}
