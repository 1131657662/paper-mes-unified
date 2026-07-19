import { useEffect, useState } from 'react'
import { Card, Form, message } from 'antd'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import MesPageHeader from '../../components/layout/MesPageHeader'
import DocumentPaginationBar from '../../components/biz/DocumentPaginationBar'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { useCustomers, useWarehouses } from '../../features/processOrderCreate/hooks/useReferenceData'
import { useCreateDelivery } from '../../features/delivery/hooks/useCreateDelivery'
import DeliveryCreateTable from './DeliveryCreateTable'
import DeliveryCreateFooter from './DeliveryCreateFooter'
import DeliveryFinishTableToolbar from './DeliveryFinishTableToolbar'
import DeliverySelectionHeaderActions from './DeliverySelectionHeaderActions'
import DeliverySelectionReviewDrawer from './DeliverySelectionReviewDrawer'
import DeliveryPickupInfoCard from './DeliveryPickupInfoCard'
import {
  buildDeliveryCreateDTO,
  confirmDeliveryCashRelease,
  type DeliveryCreateFormValues,
} from './deliveryCreateSubmit'
import { finishScopeName } from './deliveryFinishScope'
import {
  deliverySelectionError,
  summarizeDeliverySelection,
  type DeliveryLineEdit,
} from './deliverySelectionModel'
import { useDeliveryCreateInventory } from './useDeliveryCreateInventory'
import { finishUuidsFromNavigationState } from './deliveryCreateNavigation'
import { deliveryAvailableEmptyText } from './deliveryAvailableEmptyText'
import '../documentModule.css'
import './DeliveryCreatePage.css'

export default function DeliveryCreatePage() {
  const [form] = Form.useForm<DeliveryCreateFormValues>()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const initialCustomerUuid = searchParams.get('customerUuid') || undefined
  const initialWarehouseUuid = searchParams.get('warehouseUuid') || undefined
  const initialFinishUuids = finishUuidsFromNavigationState(location.state)
  const customersQuery = useCustomers()
  const warehousesQuery = useWarehouses()
  const createMutation = useCreateDelivery()
  const [customerUuid, setCustomerUuid] = useState<string | undefined>(initialCustomerUuid)
  const warehouseUuid = Form.useWatch('warehouseUuid', form) as string | undefined
  const enabledWarehouses = (warehousesQuery.data?.records ?? []).filter((item) => item.status === 1)
  const suggestedWarehouseUuid = initialWarehouseUuid
    ?? enabledWarehouses.find((item) => item.isDefault === 1)?.uuid
  const [reviewOpen, setReviewOpen] = useState(false)
  const [selectionExpanded, setSelectionExpanded] = useState(false)
  const [lineEdits, setLineEdits] = useState<Record<string, DeliveryLineEdit>>({})
  const inventory = useDeliveryCreateInventory(customerUuid, warehouseUuid, initialFinishUuids)

  useEffect(() => {
    if (!warehouseUuid && suggestedWarehouseUuid) {
      form.setFieldValue('warehouseUuid', suggestedWarehouseUuid)
    }
  }, [form, suggestedWarehouseUuid, warehouseUuid])
  const selectedFinishes = inventory.selectedRows
  const selectionSummary = summarizeDeliverySelection(selectedFinishes, lineEdits)
  const selectionError = deliverySelectionError(selectedFinishes, lineEdits)
  const scopeName = finishScopeName(inventory.scope)
  const emptyText = deliveryAvailableEmptyText({
    customerUuid,
    data: inventory.query.data,
    hasFilters: Boolean(inventory.filters.keyword.trim()) || inventory.filters.sourceIssue !== 'all',
    isError: inventory.query.isError,
    scopeName,
    warehouseUuid,
  })

  const handleCustomerChange = (value?: string) => {
    setCustomerUuid(value)
    form.setFieldValue('warehouseUuid', undefined)
    inventory.changeCustomer()
    setLineEdits({})
  }

  const handleWarehouseChange = () => inventory.changeWarehouse()

  const handleEditChange = (finishUuid: string, value: DeliveryLineEdit) => {
    setLineEdits((prev) => ({ ...prev, [finishUuid]: { ...prev[finishUuid], ...value } }))
  }

  const handleRemoveSelected = (finishUuid: string) => {
    inventory.removeSelected(finishUuid)
    setLineEdits((current) => {
      const next = { ...current }
      delete next[finishUuid]
      return next
    })
  }

  const handleSubmit = async () => {
    if (createMutation.isPending) return
    const values = await form.validateFields()
    if (selectedFinishes.length === 0) {
      message.warning('请先勾选本次要出库的成品或余料')
      return
    }
    if (selectionError) {
      message.error(selectionError)
      return
    }
    const hasRisk = selectedFinishes.some((item) => item.settlementRisk)
    if (hasRisk) {
      const confirmed = await confirmDeliveryCashRelease()
      if (!confirmed) return
    }
    const uuid = await createMutation.mutateAsync(buildDeliveryCreateDTO({
      forceRelease: hasRisk,
      lineEdits,
      selectedFinishes,
      values,
    }))
    message.success('出库单已生成')
    navigate(`/delivery-orders/${uuid}`)
  }

  return (
    <div className={`document-module-page delivery-create-page${selectionExpanded
      ? ' delivery-create-page--selection-expanded'
      : ''}`}
    >
      <MesPageHeader
        title="新建出库单"
        eyebrow="出库管理"
        onBack={() => navigate('/delivery-orders')}
      />

      <DeliveryPickupInfoCard
        customers={customersQuery.data?.records ?? []}
        form={form}
        initialCustomerUuid={initialCustomerUuid}
        initialWarehouseUuid={initialWarehouseUuid}
        loading={customersQuery.isLoading || warehousesQuery.isLoading}
        warehouses={warehousesQuery.data?.records ?? []}
        onCustomerChange={handleCustomerChange}
        onWarehouseChange={handleWarehouseChange}
      />
      {customersQuery.isError && (
        <QueryLoadErrorAlert
          description="客户列表未成功加载，当前下拉选项可能不完整。"
          message="客户资料加载失败"
          onRetry={() => void customersQuery.refetch()}
        />
      )}
      {warehousesQuery.isError && (
        <QueryLoadErrorAlert
          description="仓库资料未成功加载，暂时无法选择正确的出库仓库。"
          message="仓库资料加载失败"
          onRetry={() => void warehousesQuery.refetch()}
        />
      )}

      <Card
        className="document-module-card delivery-create-page__selection"
        title="选择出库成品"
        extra={(
          <DeliverySelectionHeaderActions
            expanded={selectionExpanded}
            finishes={selectedFinishes}
            scope={inventory.scope}
            selectedRowKeys={inventory.selectedKeys}
            scopeTotals={inventory.scopeTotals}
            totalCount={inventory.query.data?.total ?? 0}
            onScopeChange={inventory.changeScope}
            onToggleExpanded={() => setSelectionExpanded((current) => !current)}
          />
        )}
      >
        {customerUuid && inventory.query.isError && (
          <QueryLoadErrorAlert
            description="本次未取得库存数据，不能据此判断该客户没有库存。"
            message="可出库库存加载失败"
            onRetry={() => void inventory.query.refetch()}
          />
        )}
        <DeliveryFinishTableToolbar
          filters={inventory.filters}
          resultCount={inventory.visibleRows.length}
          scope={inventory.scope}
          totalCount={inventory.query.data?.total ?? 0}
          onChange={inventory.changeFilters}
        />
        <div className="document-module-table">
          <DeliveryCreateTable
            data={inventory.visibleRows}
            edits={lineEdits}
            emptyText={emptyText}
            loading={inventory.query.isLoading || inventory.query.isFetching}
            scope={inventory.scope}
            selectedRowKeys={inventory.selectedKeys}
            onEditChange={handleEditChange}
            onSelectionChange={inventory.changeSelection}
          />
        </div>
        {!inventory.filters.selectedOnly && customerUuid && (
          <DocumentPaginationBar
            current={inventory.page}
            pageSize={inventory.pageSize}
            total={inventory.query.data?.total ?? 0}
            onChange={inventory.changePage}
          />
        )}
      </Card>
      <DeliveryCreateFooter
        disabled={Boolean(selectionError)}
        loading={createMutation.isPending}
        summary={selectionSummary}
        onCancel={() => navigate('/delivery-orders')}
        onReview={() => setReviewOpen(true)}
        onSubmit={handleSubmit}
      />
      <DeliverySelectionReviewDrawer
        edits={lineEdits}
        items={selectedFinishes}
        open={reviewOpen}
        onClose={() => setReviewOpen(false)}
        onEditChange={handleEditChange}
        onRemove={handleRemoveSelected}
      />
    </div>
  )
}
