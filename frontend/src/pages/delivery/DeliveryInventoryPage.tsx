import { useNavigate, useSearchParams } from 'react-router-dom'
import { useState } from 'react'
import DocumentPaginationBar from '../../components/biz/DocumentPaginationBar'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { PERMISSIONS } from '../../constants/permissions'
import { useHasPermission } from '../../stores/authStore'
import { useWarehouses } from '../../features/processOrderCreate/hooks/useReferenceData'
import { useDeliveryInventoryUnassigned } from '../../features/delivery/hooks/useDeliveryInventoryUnassigned'
import DeliveryInventoryCustomerTable from './DeliveryInventoryCustomerTable'
import DeliveryInventoryFilterBar from './DeliveryInventoryFilterBar'
import DeliveryInventoryFinishTable from './DeliveryInventoryFinishTable'
import DeliveryInventoryPageHeader from './DeliveryInventoryPageHeader'
import DeliveryInventorySummary from './DeliveryInventorySummary'
import DeliveryInventoryWarehouseRepairDrawer from './DeliveryInventoryWarehouseRepairDrawer'
import { useDeliveryInventoryPage } from './useDeliveryInventoryPage'
import './DeliveryInventoryPage.css'
import './DeliveryInventoryTable.css'

export default function DeliveryInventoryPage() {
  const model = useDeliveryInventoryPage()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const canManage = useHasPermission(PERMISSIONS.deliveryManage)
  const [repairOpen, setRepairOpen] = useState(() => searchParams.get('open') === 'unassigned')
  const warehousesQuery = useWarehouses()
  const unassignedQuery = useDeliveryInventoryUnassigned({ current: 1, size: 1 }, canManage)
  const createDelivery = (customerUuid: string, finishUuids: string[] = []) => {
    navigate(`/delivery-orders/create?customerUuid=${encodeURIComponent(customerUuid)}`, { state: { finishUuids } })
  }
  const closeRepairDrawer = () => {
    setRepairOpen(false)
    if (searchParams.get('open') !== 'unassigned') return
    const next = new URLSearchParams(searchParams)
    next.delete('open')
    setSearchParams(next, { replace: true })
  }
  return (
    <section className="delivery-inventory-page">
      <DeliveryInventoryPageHeader
        exporting={model.exportMutation.isPending}
        updatedAt={model.summaryQuery.dataUpdatedAt}
        view={model.view}
        onExport={model.exportInventory}
        onOpenRepair={canManage ? () => setRepairOpen(true) : undefined}
        onRefresh={model.refresh}
        onViewChange={model.setView}
        unassignedCount={unassignedQuery.data?.total ?? 0}
      />
      <div className="delivery-inventory-filter-panel">
        <DeliveryInventoryFilterBar
          filters={model.filters}
          warehouses={warehousesQuery.data?.records ?? []}
          onChange={model.setFilters}
          onSearch={(keyword) => model.setFilters({ ...model.filters, keyword })}
        />
      </div>
      {model.activeQuery.isError || model.summaryQuery.isError ? (
        <QueryLoadErrorAlert message="成品库存加载失败" description="库存数据未完整加载，当前空表不代表没有库存。" onRetry={model.refresh} />
      ) : null}
      <DeliveryInventorySummary summary={model.summaryQuery.data} />
      <div className="delivery-inventory-grid">
        <div className="delivery-inventory-table-shell delivery-inventory-results">
          {model.view === 'customers' ? (
            <DeliveryInventoryCustomerTable canManage={canManage} data={model.customerQuery.data?.records ?? []}
              tableTitle="客户库存" fillHeight loading={model.customerQuery.isLoading || model.customerQuery.isFetching}
              onReload={model.refresh} onCreateDelivery={createDelivery}
              onView={(customer) => navigate(`/delivery-orders/inventory/customers/${customer.customerUuid}`)} />
          ) : (
            <DeliveryInventoryFinishTable showCustomer data={model.finishQuery.data?.records ?? []}
              tableTitle="成品卷库存" fillHeight loading={model.finishQuery.isLoading || model.finishQuery.isFetching}
              onReload={model.refresh}
              onOpenCustomer={(customerUuid) => navigate(`/delivery-orders/inventory/customers/${customerUuid}`)}
              onOpenDelivery={(uuid) => navigate(`/delivery-orders/${uuid}`)} />
          )}
        </div>
        <DocumentPaginationBar current={model.page} pageSize={model.pageSize}
          total={model.activeQuery.data?.total ?? 0} onChange={model.changePage} />
      </div>
      <DeliveryInventoryWarehouseRepairDrawer open={repairOpen}
        warehouses={warehousesQuery.data?.records ?? []} onClose={closeRepairDrawer} />
    </section>
  )
}
