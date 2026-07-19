import { useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { useDeliveryInventoryCustomers } from '../../features/delivery/hooks/useDeliveryInventoryCustomers'
import { useDeliveryInventoryFinishes } from '../../features/delivery/hooks/useDeliveryInventoryFinishes'
import { useDeliveryInventorySummary } from '../../features/delivery/hooks/useDeliveryInventorySummary'
import { useExportDeliveryInventory } from '../../features/delivery/hooks/useExportDeliveryInventory'
import { useConfiguredPageSize } from '../../features/systemConfig/hooks/useConfiguredPageSize'
import type { DeliveryInventoryFilter } from '../../types/deliveryInventory'
import type { DeliveryInventoryView } from './deliveryInventoryModel'

export function useDeliveryInventoryPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const view: DeliveryInventoryView = location.pathname.endsWith('/finishes') ? 'finishes' : 'customers'
  const filters = filtersFromSearch(searchParams)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useConfiguredPageSize(20)
  const customerQuery = useDeliveryInventoryCustomers(
    { ...filters, current: page, size: pageSize }, view === 'customers')
  const finishQuery = useDeliveryInventoryFinishes(
    { ...filters, current: page, size: pageSize }, view === 'finishes')
  const summaryQuery = useDeliveryInventorySummary(filters)
  const exportMutation = useExportDeliveryInventory()
  const activeQuery = view === 'customers' ? customerQuery : finishQuery

  const setView = (next: DeliveryInventoryView) => {
    setPage(1)
    navigate(`${next === 'finishes' ? '/delivery-orders/inventory/finishes' : '/delivery-orders/inventory'}${location.search}`)
  }
  const setFilters = (next: DeliveryInventoryFilter) => {
    setSearchParams(searchFromFilters(next), { replace: true }); setPage(1)
  }
  const changePage = (nextPage: number, nextSize: number) => {
    setPage(nextPage)
    setPageSize(nextSize)
  }
  const refresh = () => {
    void activeQuery.refetch()
    void summaryQuery.refetch()
  }
  const exportInventory = () => exportMutation.mutate({ ...filters, current: 1, size: pageSize })

  return {
    activeQuery, changePage, customerQuery, exportInventory, exportMutation, filters, finishQuery,
    page, pageSize, refresh, setFilters, setView,
    summaryQuery, view,
  }
}

function filtersFromSearch(params: URLSearchParams): DeliveryInventoryFilter {
  const stockState = Number(params.get('stockState'))
  const inventoryType = Number(params.get('inventoryType'))
  const stockAgeMinDays = Number(params.get('stockAgeMinDays'))
  return {
    keyword: params.get('keyword') || undefined,
    warehouseUuid: params.get('warehouseUuid') || undefined,
    stockState: stockState === 1 || stockState === 2 ? stockState : undefined,
    inventoryType: inventoryType >= 1 && inventoryType <= 3 ? inventoryType as 1 | 2 | 3 : undefined,
    stockAgeMinDays: stockAgeMinDays > 0 ? stockAgeMinDays : undefined,
  }
}

function searchFromFilters(filters: DeliveryInventoryFilter) {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== '') params.set(key, String(value))
  })
  return params
}
