import { message } from 'antd'
import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { PERMISSIONS } from '../../constants/permissions'
import { useDeliveryInventoryCustomers } from '../../features/delivery/hooks/useDeliveryInventoryCustomers'
import { useDeliveryInventoryFinishes } from '../../features/delivery/hooks/useDeliveryInventoryFinishes'
import { useDeliveryInventoryOrderGroups } from '../../features/delivery/hooks/useDeliveryInventoryOrderGroups'
import { useDeliveryInventorySummary } from '../../features/delivery/hooks/useDeliveryInventorySummary'
import { useExportDeliveryInventory } from '../../features/delivery/hooks/useExportDeliveryInventory'
import { useValidateDeliveryInventory } from '../../features/delivery/hooks/useValidateDeliveryInventory'
import { useWarehouses } from '../../features/processOrderCreate/hooks/useReferenceData'
import { useHasPermission } from '../../stores/authStore'
import type { DeliveryInventoryFilter } from '../../types/deliveryInventory'
import { useDeliveryInventorySelection } from './useDeliveryInventorySelection'

export function useDeliveryInventoryCustomerPage() {
  const navigate = useNavigate()
  const { customerUuid = '' } = useParams()
  const [filters, setFilters] = useState<DeliveryInventoryFilter>({ customerUuid })
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [view, setView] = useState<'rolls' | 'orders'>('rolls')
  const selection = useDeliveryInventorySelection()
  const query = { ...filters, customerUuid, current: page, size: pageSize }
  const finishQuery = useDeliveryInventoryFinishes(query, view === 'rolls')
  const orderGroupQuery = useDeliveryInventoryOrderGroups(query, view === 'orders')
  const summaryQuery = useDeliveryInventorySummary({ ...filters, customerUuid })
  const customerQuery = useDeliveryInventoryCustomers({ customerUuid, current: 1, size: 1 })
  const warehousesQuery = useWarehouses()
  const validation = useValidateDeliveryInventory()
  const exportMutation = useExportDeliveryInventory()
  const canManage = useHasPermission(PERMISSIONS.deliveryManage)
  const rows = finishQuery.data?.records ?? []
  const { selected, selectedKeys, selectedWarehouseUuid } = selection
  const warehouses = warehousesQuery.data?.records ?? []
  const effectiveWarehouseUuid = selectedWarehouseUuid || filters.warehouseUuid
  const groups = orderGroupQuery.data?.records ?? []

  const updateFilters = (next: DeliveryInventoryFilter) => {
    setFilters({ ...next, customerUuid }); setPage(1); selection.clearSelection()
  }
  const changeView = (next: 'rolls' | 'orders') => {
    setView(next)
    setPage(1)
  }
  const createDelivery = async () => {
    if (!effectiveWarehouseUuid || selected.length === 0) return
    const result = await validation.mutateAsync({ customerUuid, warehouseUuid: effectiveWarehouseUuid, finishUuids: selectedKeys })
    if (result.unavailable.length) message.warning(`${result.unavailable.length} 卷状态已变化，已自动移除`)
    if (!result.availableFinishUuids.length) return
    navigate(`/delivery-orders/create?customerUuid=${encodeURIComponent(customerUuid)}&warehouseUuid=${encodeURIComponent(effectiveWarehouseUuid)}`,
      { state: { finishUuids: result.availableFinishUuids } })
  }

  return {
    activeWarehouseName: warehouses.find((item) => item.uuid === effectiveWarehouseUuid)?.warehouseName,
    back: () => navigate('/delivery-orders/inventory'), canManage,
    changePage: (next: number, size: number) => { setPage(next); setPageSize(size) },
    changeSelection: selection.changeSelection, clearSelection: selection.clearSelection,
    createDelivery, customer: customerQuery.data?.records[0],
    exportMutation, exportRows: () => exportMutation.mutate({ ...filters, customerUuid, current: 1, size: 100 }),
    activeIsError: view === 'rolls' ? finishQuery.isError : orderGroupQuery.isError,
    activeTotal: view === 'rolls' ? finishQuery.data?.total ?? 0 : orderGroupQuery.data?.total ?? 0,
    filters, finishQuery, groups, openDelivery: (uuid: string) => navigate(`/delivery-orders/${uuid}`),
    orderGroupQuery, page, pageSize,
    reload: () => { void (view === 'rolls' ? finishQuery.refetch() : orderGroupQuery.refetch()); void summaryQuery.refetch() },
    rows, selected, selectedByUuid: selection.selectedByUuid, selectedKeys,
    selectionDisabled: selection.selectionDisabled, setView: changeView, summaryQuery,
    toggleGroup: selection.toggleGroup, toggleSelection: selection.toggleSelection, updateFilters,
    validation, view, warehouses,
  }
}
