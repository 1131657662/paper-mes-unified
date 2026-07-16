import { useState } from 'react'
import { Card, Form, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import MesPageHeader from '../../components/layout/MesPageHeader'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import { useAvailableFinishes } from '../../features/delivery/hooks/useAvailableFinishes'
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
import { filterFinishesByScope, finishScopeName, type DeliveryFinishScope } from './deliveryFinishScope'
import {
  defaultDeliveryFinishFilters,
  filterDeliveryFinishes,
  type DeliveryFinishFilters,
} from './deliveryFinishFilter'
import {
  deliverySelectionError,
  selectedDeliveryFinishes,
  summarizeDeliverySelection,
  type DeliveryLineEdit,
} from './deliverySelectionModel'
import '../documentModule.css'
import './DeliveryCreatePage.css'

export default function DeliveryCreatePage() {
  const [form] = Form.useForm<DeliveryCreateFormValues>()
  const navigate = useNavigate()
  const customersQuery = useCustomers()
  const createMutation = useCreateDelivery()
  const [customerUuid, setCustomerUuid] = useState<string>()
  const [reviewOpen, setReviewOpen] = useState(false)
  const [selectionExpanded, setSelectionExpanded] = useState(false)
  const [finishScope, setFinishScope] = useState<DeliveryFinishScope>('product')
  const [finishFilters, setFinishFilters] = useState<DeliveryFinishFilters>({ ...defaultDeliveryFinishFilters })
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [lineEdits, setLineEdits] = useState<Record<string, DeliveryLineEdit>>({})
  const finishesQuery = useAvailableFinishes(customerUuid)
  const finishes = finishesQuery.data ?? []
  const scopedFinishes = filterFinishesByScope(finishes, finishScope)
  const visibleFinishes = filterDeliveryFinishes(scopedFinishes, finishFilters, selectedRowKeys)
  const selectedFinishes = selectedDeliveryFinishes(finishes, selectedRowKeys)
  const selectionSummary = summarizeDeliverySelection(selectedFinishes, lineEdits)
  const selectionError = deliverySelectionError(selectedFinishes, lineEdits)
  const scopeName = finishScopeName(finishScope)
  const emptyText = !customerUuid
    ? '请先选择客户'
    : finishesQuery.isError
      ? `可出库${scopeName}加载失败，请重新加载`
      : scopedFinishes.length === 0
      ? `该客户暂无可出库${scopeName}`
      : `没有符合筛选条件的${scopeName}`

  const handleCustomerChange = (value?: string) => {
    setCustomerUuid(value)
    setFinishScope('product')
    setFinishFilters({ ...defaultDeliveryFinishFilters })
    setSelectedRowKeys([])
    setLineEdits({})
  }

  const handleScopeChange = (value: DeliveryFinishScope) => {
    setFinishScope(value)
  }

  const handleEditChange = (finishUuid: string, value: DeliveryLineEdit) => {
    setLineEdits((prev) => ({ ...prev, [finishUuid]: { ...prev[finishUuid], ...value } }))
  }

  const handleRemoveSelected = (finishUuid: string) => {
    setSelectedRowKeys((current) => current.filter((key) => String(key) !== finishUuid))
    setLineEdits((current) => {
      const next = { ...current }
      delete next[finishUuid]
      return next
    })
  }

  const handleSubmit = async () => {
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
        loading={customersQuery.isLoading}
        onCustomerChange={handleCustomerChange}
      />
      {customersQuery.isError && (
        <QueryLoadErrorAlert
          description="客户列表未成功加载，当前下拉选项可能不完整。"
          message="客户资料加载失败"
          onRetry={() => void customersQuery.refetch()}
        />
      )}

      <Card
        className="document-module-card delivery-create-page__selection"
        title="选择出库成品"
        extra={(
          <DeliverySelectionHeaderActions
            expanded={selectionExpanded}
            finishes={finishes}
            scope={finishScope}
            selectedRowKeys={selectedRowKeys}
            onScopeChange={handleScopeChange}
            onToggleExpanded={() => setSelectionExpanded((current) => !current)}
          />
        )}
      >
        {customerUuid && finishesQuery.isError && (
          <QueryLoadErrorAlert
            description="本次未取得库存数据，不能据此判断该客户没有库存。"
            message="可出库库存加载失败"
            onRetry={() => void finishesQuery.refetch()}
          />
        )}
        <DeliveryFinishTableToolbar
          filters={finishFilters}
          resultCount={visibleFinishes.length}
          scope={finishScope}
          totalCount={scopedFinishes.length}
          onChange={setFinishFilters}
        />
        <div className="document-module-table">
          <DeliveryCreateTable
            data={visibleFinishes}
            edits={lineEdits}
            emptyText={emptyText}
            loading={finishesQuery.isLoading || finishesQuery.isFetching}
            scope={finishScope}
            selectedRowKeys={selectedRowKeys}
            onEditChange={handleEditChange}
            onSelectionChange={setSelectedRowKeys}
          />
        </div>
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
